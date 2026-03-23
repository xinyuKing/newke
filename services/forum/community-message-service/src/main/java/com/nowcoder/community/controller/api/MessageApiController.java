package com.nowcoder.community.controller.api;

import com.alibaba.fastjson2.JSONObject;
import com.nowcoder.community.client.UserClient;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.ApiResponse;
import com.nowcoder.community.util.ApiResponseUtils;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

/**
 * 私信与系统通知 REST 接口。
 */
@RestController
@RequestMapping("/api")
public class MessageApiController implements CommunityConstant {

    private final MessageService messageService;
    private final HostHolder hostHolder;
    private final UserClient userClient;

    public MessageApiController(MessageService messageService, HostHolder hostHolder, UserClient userClient) {
        this.messageService = messageService;
        this.hostHolder = hostHolder;
        this.userClient = userClient;
    }

    @GetMapping("/messages/conversations")
    public ApiResponse<Map<String, Object>> conversations(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int limit) {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(403, "not_login");
        }

        int safePage = normalizePage(page);
        int safeLimit = normalizeLimit(limit);
        int offset = (safePage - 1) * safeLimit;
        int rows = messageService.findConversationCount(user.getId());
        List<Message> conversationList = messageService.findConversations(user.getId(), offset, safeLimit);
        List<Map<String, Object>> conversations = new ArrayList<>();
        for (Message conversation : conversationList) {
            Map<String, Object> item = new HashMap<>();
            item.put("conversation", conversation);
            item.put("letterCount", messageService.findLetterCount(conversation.getConversationId()));
            item.put(
                    "unreadCount",
                    messageService.findLetterUnreadCount(user.getId(), conversation.getConversationId()));
            int targetId = user.getId() == conversation.getFromId() ? conversation.getToId() : conversation.getFromId();
            item.put("target", ApiResponseUtils.unwrap(userClient.getUser(targetId)));
            conversations.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", conversations);
        data.put("rows", rows);
        data.put("page", safePage);
        data.put("limit", safeLimit);
        data.put("letterUnreadCount", messageService.findLetterUnreadCount(user.getId(), null));
        data.put("noticeUnreadCount", messageService.findNoticeUnreadCount(user.getId(), null));
        return ApiResponse.success(data);
    }

    @GetMapping("/messages/conversations/{conversationId}")
    public ApiResponse<Map<String, Object>> conversationDetail(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(403, "not_login");
        }

        int safePage = normalizePage(page);
        int safeLimit = normalizeLimit(limit);
        int offset = (safePage - 1) * safeLimit;
        int rows = messageService.findLetterCount(conversationId);
        List<Message> letterList = messageService.findLetters(conversationId, offset, safeLimit);

        List<Map<String, Object>> letters = new ArrayList<>();
        List<Integer> unreadIds = new ArrayList<>();
        for (Message message : letterList) {
            Map<String, Object> item = new HashMap<>();
            item.put("letter", message);
            item.put("fromUser", ApiResponseUtils.unwrap(userClient.getUser(message.getFromId())));
            letters.add(item);

            if (message.getStatus() == 0 && message.getToId() == user.getId()) {
                unreadIds.add(message.getId());
            }
        }
        if (!unreadIds.isEmpty()) {
            messageService.readMessage(unreadIds);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", letters);
        data.put("rows", rows);
        data.put("page", safePage);
        data.put("limit", safeLimit);
        data.put("target", getLetterTarget(conversationId));
        return ApiResponse.success(data);
    }

    @PostMapping("/messages")
    public ApiResponse<Void> send(@RequestBody Map<String, Object> body) {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(403, "not_login");
        }
        if (body == null) {
            return ApiResponse.error(400, "invalid_request");
        }

        String toName = body.get("toName") == null ? null : body.get("toName").toString();
        String content =
                body.get("content") == null ? null : body.get("content").toString();
        if (StringUtils.isBlank(toName)) {
            return ApiResponse.error(1, "target_empty");
        }
        if (StringUtils.isBlank(content)) {
            return ApiResponse.error(1, "content_empty");
        }

        User target = ApiResponseUtils.unwrap(userClient.getUserByName(toName));
        if (target == null) {
            return ApiResponse.error(1, "target_not_found");
        }

        Message message = new Message();
        message.setFromId(user.getId());
        message.setToId(target.getId());
        message.setCreateTime(new Date());
        message.setStatus(0);
        message.setContent(content);
        message.setConversationId(buildConversationId(message.getFromId(), message.getToId()));
        messageService.addMessage(message);
        return ApiResponse.success(null);
    }

    @GetMapping("/notices")
    public ApiResponse<Map<String, Object>> notices() {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(403, "not_login");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("commentNotice", buildNoticeSummary(user.getId(), TOPIC_COMMENT));
        data.put("likeNotice", buildNoticeSummary(user.getId(), TOPIC_LIKE));
        data.put("followNotice", buildNoticeSummary(user.getId(), TOPIC_FOLLOW));
        data.put("letterUnreadCount", messageService.findLetterUnreadCount(user.getId(), null));
        data.put("noticeUnreadCount", messageService.findNoticeUnreadCount(user.getId(), null));
        return ApiResponse.success(data);
    }

    @GetMapping("/notices/{topic}")
    public ApiResponse<Map<String, Object>> noticeDetail(
            @PathVariable String topic,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(403, "not_login");
        }

        int safePage = normalizePage(page);
        int safeLimit = normalizeLimit(limit);
        int offset = (safePage - 1) * safeLimit;
        int rows = messageService.findNoticeCount(user.getId(), topic);
        List<Message> noticeList = messageService.findNotices(user.getId(), topic, offset, safeLimit);
        List<Map<String, Object>> noticeVoList = new ArrayList<>();
        List<Integer> unreadIds = new ArrayList<>();
        for (Message notice : noticeList) {
            Map<String, Object> item = new HashMap<>();
            item.put("notice", notice);

            Map<String, Object> data = parseNoticeContent(notice.getContent());
            item.put("user", ApiResponseUtils.unwrap(userClient.getUser((Integer) data.get("userId"))));
            item.put("entityType", data.get("entityType"));
            item.put("entityId", data.get("entityId"));
            item.put("postId", data.get("postId"));
            item.put("fromUser", ApiResponseUtils.unwrap(userClient.getUser(notice.getFromId())));
            noticeVoList.add(item);

            if (notice.getStatus() == 0 && notice.getToId() == user.getId()) {
                unreadIds.add(notice.getId());
            }
        }
        if (!unreadIds.isEmpty()) {
            messageService.readMessage(unreadIds);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", noticeVoList);
        result.put("rows", rows);
        result.put("page", safePage);
        result.put("limit", safeLimit);
        return ApiResponse.success(result);
    }

    /**
     * 解析会话目标用户。
     *
     * @param conversationId 会话 ID
     * @return 目标用户
     */
    private User getLetterTarget(String conversationId) {
        String[] ids = conversationId.split("_");
        int id0 = Integer.parseInt(ids[0]);
        int id1 = Integer.parseInt(ids[1]);
        int currentUserId = hostHolder.getUser().getId();
        return currentUserId == id0
                ? ApiResponseUtils.unwrap(userClient.getUser(id1))
                : ApiResponseUtils.unwrap(userClient.getUser(id0));
    }

    /**
     * 构造通知摘要。
     *
     * @param userId 用户 ID
     * @param topic 通知主题
     * @return 摘要数据
     */
    private Map<String, Object> buildNoticeSummary(int userId, String topic) {
        Message message = messageService.findLatestNotice(userId, topic);
        if (message == null) {
            return null;
        }

        Map<String, Object> messageVo = new HashMap<>();
        messageVo.put("message", message);
        Map<String, Object> data = parseNoticeContent(message.getContent());
        messageVo.put("user", ApiResponseUtils.unwrap(userClient.getUser((Integer) data.get("userId"))));
        messageVo.put("entityType", data.get("entityType"));
        messageVo.put("entityId", data.get("entityId"));
        messageVo.put("postId", data.get("postId"));
        messageVo.put("count", messageService.findNoticeCount(userId, topic));
        messageVo.put("unreadCount", messageService.findNoticeUnreadCount(userId, topic));
        return messageVo;
    }

    /**
     * 统一解析通知内容 JSON。
     *
     * @param content 通知内容
     * @return 解析结果
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseNoticeContent(String content) {
        return JSONObject.parseObject(HtmlUtils.htmlUnescape(content), HashMap.class);
    }

    /**
     * 构造双方一致的会话 ID。
     *
     * @param fromId 发送方 ID
     * @param toId 接收方 ID
     * @return 会话 ID
     */
    private String buildConversationId(int fromId, int toId) {
        return fromId < toId ? fromId + "_" + toId : toId + "_" + fromId;
    }

    /**
     * 修正页码入参。
     *
     * @param page 页码
     * @return 合法页码
     */
    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    /**
     * 修正分页大小入参。
     *
     * @param limit 分页大小
     * @return 合法分页大小
     */
    private int normalizeLimit(int limit) {
        return limit > 0 ? limit : 10;
    }
}
