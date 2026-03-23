package com.nowcoder.community.quartz;

import com.nowcoder.community.client.LikeClient;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.util.ApiResponseUtils;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 刷新帖子分数并同步搜索索引的定时任务。
 */
public class PostScoreRefreshJob implements Job, CommunityConstant {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    /**
     * 牛客社区项目的基准时间，使用不可变时间对象避免线程安全问题。
     */
    private static final long EPOCH_MILLIS = LocalDateTime.of(2014, 8, 1, 0, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();

    /**
     * Quartz 默认按无参构造实例化 Job，此处保留字段注入以兼容当前调度配置。
     */
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeClient likeClient;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        BoundSetOperations<String, Object> operations = redisTemplate.boundSetOps(RedisKeyUtil.getPostScoreKey());
        Long size = operations.size();
        if (size == null || size == 0L) {
            LOGGER.info("[任务取消] 当前没有需要刷新分数的帖子。");
            return;
        }

        LOGGER.info("[任务开始] 正在刷新帖子分数，待处理数量：{}。", size);
        while (true) {
            Object postIdValue = operations.pop();
            if (postIdValue == null) {
                break;
            }
            if (!(postIdValue instanceof Number)) {
                LOGGER.warn("跳过无法识别的帖子标识：{}", postIdValue);
                continue;
            }
            refresh(((Number) postIdValue).intValue());
        }
        LOGGER.info("[任务结束] 帖子分数刷新完成。");
    }

    /**
     * 刷新单个帖子的分数并同步搜索索引。
     *
     * @param postId 帖子 ID
     */
    private void refresh(int postId) {
        DiscussPost post = discussPostService.findDiscussPostById(postId);
        if (post == null) {
            LOGGER.error("帖子不存在，postId={}", postId);
            return;
        }

        boolean wonderful = post.getStatus() == 1;
        int commentCount = post.getCommentCount();
        Long likeCountValue = ApiResponseUtils.unwrap(likeClient.getEntityLikeCount(ENTITY_TYPE_POST, postId));
        long likeCount = likeCountValue == null ? 0L : likeCountValue;

        double weight = (wonderful ? 75 : 0) + commentCount * 10 + likeCount * 2;
        double score = Math.log10(Math.max(weight, 1))
                + (post.getCreateTime().getTime() - EPOCH_MILLIS) / (1000.0 * 60 * 60 * 24);

        discussPostService.updateScore(postId, score);
        post.setScore(score);
        elasticsearchService.saveDiscussPost(post);
    }
}
