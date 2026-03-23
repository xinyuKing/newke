package com.nowcoder.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.SensitiveFilter;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * 帖子领域服务。
 *
 * <p>负责帖子列表查询、发帖、计数更新以及首页热点列表的 Caffeine 本地缓存。</p>
 */
@Service
public class DiscussPostService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscussPostService.class);

    /**
     * 首页热点列表缓存仅覆盖“游客视角 + 热度排序”场景。
     */
    private static final int HOT_POST_ORDER_MODE = 1;

    private final DiscussPostMapper discussPostMapper;
    private final SensitiveFilter sensitiveFilter;
    private final int maxSize;
    private final int expireSeconds;

    private LoadingCache<String, List<DiscussPost>> postListCache;
    private LoadingCache<Integer, Integer> postRowsCache;

    public DiscussPostService(
            DiscussPostMapper discussPostMapper,
            SensitiveFilter sensitiveFilter,
            @Value("${caffeine.posts.max-size}") int maxSize,
            @Value("${caffeine.posts.expire-seconds}") int expireSeconds) {
        this.discussPostMapper = discussPostMapper;
        this.sensitiveFilter = sensitiveFilter;
        this.maxSize = maxSize;
        this.expireSeconds = expireSeconds;
    }

    /**
     * 初始化本地缓存。
     */
    @PostConstruct
    public void init() {
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Override
                    public List<DiscussPost> load(String key) {
                        int[] params = parsePostListCacheKey(key);
                        LOGGER.debug("load post list from DB for hot cache, offset={}, limit={}", params[0], params[1]);
                        return discussPostMapper.selectDiscussPosts(0, params[0], params[1], HOT_POST_ORDER_MODE);
                    }
                });

        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Override
                    public Integer load(Integer userId) {
                        LOGGER.debug("load post rows from DB for userId={}", userId);
                        return discussPostMapper.selectDiscussPostRows(userId);
                    }
                });
    }

    /**
     * 查询帖子列表。
     *
     * @param userId 用户 ID，0 表示首页公共列表
     * @param offset 偏移量
     * @param limit 条数
     * @param orderMode 排序模式
     * @return 帖子列表
     */
    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
        if (userId == 0 && orderMode == HOT_POST_ORDER_MODE) {
            return postListCache.get(offset + ":" + limit);
        }

        LOGGER.debug("load post list from DB.");
        List<DiscussPost> posts = discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
        return posts == null ? Collections.emptyList() : posts;
    }

    /**
     * 查询帖子总数。
     *
     * @param userId 用户 ID，0 表示首页公共列表
     * @return 帖子总数
     */
    public int selectDiscussPostRows(int userId) {
        if (userId == 0) {
            return postRowsCache.get(userId);
        }

        LOGGER.debug("load post rows from DB.");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    /**
     * 发帖。
     *
     * @param discussPost 帖子实体
     * @return 影响行数
     */
    public int addDiscussPost(DiscussPost discussPost) {
        if (discussPost == null) {
            throw new IllegalArgumentException("帖子内容不能为空");
        }

        discussPost.setTitle(HtmlUtils.htmlEscape(discussPost.getTitle()));
        discussPost.setContent(HtmlUtils.htmlEscape(discussPost.getContent()));
        discussPost.setTitle(sensitiveFilter.filter(discussPost.getTitle()));
        discussPost.setContent(sensitiveFilter.filter(discussPost.getContent()));

        int rows = discussPostMapper.insertDiscussPostRows(discussPost);
        clearPostCache();
        return rows;
    }

    /**
     * 按 ID 查询帖子。
     *
     * @param id 帖子 ID
     * @return 帖子实体
     */
    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    /**
     * 更新帖子评论数。
     *
     * @param id 帖子 ID
     * @param commentCount 评论数
     * @return 影响行数
     */
    public int updateCommentCount(int id, int commentCount) {
        int rows = discussPostMapper.updateCommentCount(id, commentCount);
        clearPostCache();
        return rows;
    }

    /**
     * 更新帖子类型。
     *
     * @param id 帖子 ID
     * @param type 类型
     * @return 影响行数
     */
    public int updateType(int id, int type) {
        int rows = discussPostMapper.updateType(id, type);
        clearPostCache();
        return rows;
    }

    /**
     * 更新帖子状态。
     *
     * @param id 帖子 ID
     * @param status 状态
     * @return 影响行数
     */
    public int updateStatus(int id, int status) {
        int rows = discussPostMapper.updateStatus(id, status);
        clearPostCache();
        return rows;
    }

    /**
     * 更新帖子分数。
     *
     * @param id 帖子 ID
     * @param score 分数
     * @return 影响行数
     */
    public int updateScore(int id, double score) {
        int rows = discussPostMapper.updateScore(id, score);
        clearPostCache();
        return rows;
    }

    /**
     * 查询指定用户的帖子列表。
     *
     * @param userId 用户 ID
     * @param offset 偏移量
     * @param limit 条数
     * @return 帖子列表
     */
    public List<DiscussPost> findDiscussPostByUserId(int userId, int offset, int limit) {
        List<DiscussPost> posts = discussPostMapper.selectDiscussPostByUserId(userId, offset, limit);
        return posts == null ? Collections.emptyList() : posts;
    }

    /**
     * 查询指定用户的帖子总数。
     *
     * @param userId 用户 ID
     * @return 帖子总数
     */
    public int findDiscussPostRowsByUserId(int userId) {
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    /**
     * 失效帖子相关本地缓存。
     */
    private void clearPostCache() {
        if (postListCache != null) {
            postListCache.invalidateAll();
        }
        if (postRowsCache != null) {
            postRowsCache.invalidateAll();
        }
    }

    /**
     * 解析帖子列表缓存 Key。
     *
     * @param key 缓存 Key
     * @return offset 与 limit
     */
    private int[] parsePostListCacheKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("帖子缓存参数不能为空");
        }
        String[] params = key.split(":");
        if (params.length != 2) {
            throw new IllegalArgumentException("帖子缓存参数格式错误");
        }
        return new int[] {Integer.parseInt(params[0]), Integer.parseInt(params[1])};
    }
}
