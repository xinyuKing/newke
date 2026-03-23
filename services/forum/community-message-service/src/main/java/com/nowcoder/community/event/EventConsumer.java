package com.nowcoder.community.event;

import com.alibaba.fastjson2.JSONObject;
import com.nowcoder.community.client.SearchClient;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.ApiResponseUtils;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * 论坛消息事件消费者。
 *
 * <p>负责消费评论、点赞、关注、发帖、删帖和分享等异步事件，并同步站内通知、搜索索引和分享图片。</p>
 */
@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventConsumer.class);

    private final MessageService messageService;
    private final SearchClient searchClient;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final String wkImageCommand;
    private final String wkImageStorage;
    private final String access;
    private final String secret;
    private final String shareBucketName;

    public EventConsumer(MessageService messageService,
                         SearchClient searchClient,
                         ThreadPoolTaskScheduler taskScheduler,
                         @Value("${wk.image.command}") String wkImageCommand,
                         @Value("${wk.image.storage}") String wkImageStorage,
                         @Value("${qiniu.key.access}") String access,
                         @Value("${qiniu.key.secret}") String secret,
                         @Value("${qiniu.bucket.share.name}") String shareBucketName) {
        this.messageService = messageService;
        this.searchClient = searchClient;
        this.taskScheduler = taskScheduler;
        this.wkImageCommand = wkImageCommand;
        this.wkImageStorage = wkImageStorage;
        this.access = access;
        this.secret = secret;
        this.shareBucketName = shareBucketName;
    }

    /**
     * 处理评论、点赞、关注的站内通知事件。
     *
     * @param record Kafka 消息
     */
    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_FOLLOW, TOPIC_LIKE})
    public void handleCommentMessage(ConsumerRecord<?, ?> record) {
        Event event = parseEvent(record);
        if (event == null) {
            return;
        }

        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());

        Map<String, Object> eventData = event.getData();
        if (eventData != null && !eventData.isEmpty()) {
            content.putAll(eventData);
        }

        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);
    }

    /**
     * 处理发帖后同步 ES 索引的事件。
     *
     * @param record Kafka 消息
     */
    @KafkaListener(topics = TOPIC_PUBLISH)
    public void handlePublish(ConsumerRecord<?, ?> record) {
        Event event = parseEvent(record);
        if (event == null) {
            return;
        }

        if (!ApiResponseUtils.isOk(searchClient.indexPost(event.getEntityId()))) {
            LOGGER.warn("failed to index post {}", event.getEntityId());
        }
    }

    /**
     * 处理删帖后同步删除 ES 索引的事件。
     *
     * @param record Kafka 消息
     */
    @KafkaListener(topics = TOPIC_DELETE)
    public void handleDelete(ConsumerRecord<?, ?> record) {
        Event event = parseEvent(record);
        if (event == null) {
            return;
        }

        if (!ApiResponseUtils.isOk(searchClient.deletePost(event.getEntityId()))) {
            LOGGER.warn("failed to delete index for post {}", event.getEntityId());
        }
    }

    /**
     * 处理分享图片生成与上传事件。
     *
     * @param record Kafka 消息
     */
    @KafkaListener(topics = TOPIC_SHARE)
    public void handleShare(ConsumerRecord<?, ?> record) {
        Event event = parseEvent(record);
        if (event == null) {
            return;
        }

        Map<String, Object> eventData = event.getData();
        String htmlUrl = getString(eventData, "htmlUrl");
        String fileName = getString(eventData, "fileName");
        String suffix = getString(eventData, "suffix");
        if (StringUtils.isAnyBlank(htmlUrl, fileName, suffix)) {
            LOGGER.error("share event missing required data: {}", eventData);
            return;
        }

        List<String> command = new ArrayList<>();
        command.add(wkImageCommand);
        command.add("--quality");
        command.add("75");
        command.add(htmlUrl);
        command.add(wkImageStorage + "/" + fileName + suffix);
        try {
            new ProcessBuilder(command).start();
            LOGGER.info("share image command started, cmd={}", command);
        } catch (IOException ex) {
            LOGGER.error("share image command failed", ex);
            return;
        }

        UploadTask task = new UploadTask(fileName, suffix);
        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(task, Duration.ofMillis(500));
        task.setFuture(future);
    }

    /**
     * 统一解析 Kafka 事件消息。
     *
     * @param record Kafka 消息
     * @return 事件对象；非法消息返回 {@code null}
     */
    private Event parseEvent(ConsumerRecord<?, ?> record) {
        if (record == null || record.value() == null) {
            LOGGER.error("消息内容为空！");
            return null;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            LOGGER.error("消息格式错误！");
        }
        return event;
    }

    /**
     * 从事件数据中安全读取字符串字段。
     *
     * @param data 事件数据
     * @param key 字段名
     * @return 字符串值
     */
    private String getString(Map<String, Object> data, String key) {
        if (data == null) {
            return null;
        }
        Object value = data.get(key);
        return value == null ? null : value.toString();
    }

    /**
     * 分享图片上传任务。
     */
    private class UploadTask implements Runnable {

        /**
         * 单次上传任务最大允许运行时间，单位：毫秒。
         */
        private static final long MAX_WAIT_MILLIS = 30_000L;

        /**
         * 图片上传最多重试次数。
         */
        private static final int MAX_UPLOAD_TIMES = 10;

        private final String fileName;
        private final String suffix;
        private final long startTime;
        private int uploadTimes;
        private ScheduledFuture<?> future;

        UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        void setFuture(ScheduledFuture<?> future) {
            this.future = future;
        }

        @Override
        public void run() {
            if (System.currentTimeMillis() - startTime > MAX_WAIT_MILLIS) {
                LOGGER.error("执行时间过长，终止任务：{}", fileName);
                cancel();
                return;
            }
            if (uploadTimes >= MAX_UPLOAD_TIMES) {
                LOGGER.error("上传次数过多，终止任务：{}", fileName);
                cancel();
                return;
            }

            String path = wkImageStorage + "/" + fileName + suffix;
            File file = new File(path);
            if (!file.exists()) {
                LOGGER.info("等待图片生成[{}]", fileName);
                return;
            }

            uploadTimes++;
            LOGGER.info("开始第{}次上传[{}]。", uploadTimes, fileName);

            StringMap policy = new StringMap();
            policy.put("returnBody", CommunityUtil.getJSONString(0));
            Auth auth = Auth.create(access, secret);
            String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);
            UploadManager manager = new UploadManager(new Configuration(Zone.zone0()));

            try {
                String contentType = "image/" + (suffix.startsWith(".") ? suffix.substring(1) : suffix);
                Response response = manager.put(path, fileName, uploadToken, null, contentType, false);
                JSONObject json = JSONObject.parseObject(response.bodyString());
                if (json == null || json.get("code") == null || !"0".equals(json.get("code").toString())) {
                    LOGGER.warn("第{}次上传失败[{}]。", uploadTimes, fileName);
                    return;
                }
                LOGGER.info("第{}次上传成功[{}]。", uploadTimes, fileName);
                cancel();
            } catch (QiniuException ex) {
                LOGGER.warn("第{}次上传失败[{}]。", uploadTimes, fileName, ex);
            }
        }

        /**
         * 安全取消定时任务。
         */
        private void cancel() {
            if (future != null) {
                future.cancel(true);
            }
        }
    }
}
