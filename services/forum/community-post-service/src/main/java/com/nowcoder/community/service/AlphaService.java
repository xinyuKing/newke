package com.nowcoder.community.service;

import com.nowcoder.community.dao.AlphaDao;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Date;

/**
 * Alpha 示例服务。
 *
 * <p>该类主要用于演示 Bean 生命周期、事务和异步任务等 Spring 能力。</p>
 */
@Service
public class AlphaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlphaService.class);

    private final AlphaDao alphaDao;
    private final UserMapper userMapper;
    private final DiscussPostMapper discussPostMapper;
    private final TransactionTemplate transactionTemplate;

    public AlphaService(AlphaDao alphaDao,
                        UserMapper userMapper,
                        DiscussPostMapper discussPostMapper,
                        TransactionTemplate transactionTemplate) {
        this.alphaDao = alphaDao;
        this.userMapper = userMapper;
        this.discussPostMapper = discussPostMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @PostConstruct
    public void init() {
        LOGGER.info("AlphaService initialized");
    }

    @PreDestroy
    public void destroy() {
        LOGGER.info("AlphaService destroyed");
    }

    /**
     * 查询示例数据。
     *
     * @return 查询结果
     */
    public String find() {
        return alphaDao.select();
    }

    /**
     * 声明式事务示例。
     *
     * @return 固定结果
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public Object save1() {
        User user = new User();
        user.setUsername("alpha");
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5("123" + user.getSalt()));
        user.setEmail("alpha@qq.com");
        user.setHeaderUrl("http://image.nowcoder.com/head/99t.png");
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle("hello");
        post.setContent("新人报道");
        post.setCreateTime(new Date());
        discussPostMapper.insertDiscussPostRows(post);

        Integer.valueOf("error");
        return "ok";
    }

    /**
     * 编程式事务示例。
     *
     * @return 固定结果
     */
    public Object save2() {
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        return transactionTemplate.execute(new TransactionCallback<Object>() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                User user = new User();
                user.setUsername("alpha01");
                user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
                user.setPassword(CommunityUtil.md5("123" + user.getSalt()));
                user.setEmail("alpha01@qq.com");
                user.setHeaderUrl("http://image.nowcoder.com/head/999t.png");
                user.setCreateTime(new Date());
                userMapper.insertUser(user);

                DiscussPost post = new DiscussPost();
                post.setUserId(user.getId());
                post.setTitle("hello01");
                post.setContent("新人 01 报道");
                post.setCreateTime(new Date());
                discussPostMapper.insertDiscussPostRows(post);

                Integer.valueOf("error");
                return "ok";
            }
        });
    }

    /**
     * 异步方法示例。
     */
    @Async
    public void execute1() {
        LOGGER.debug("execute1");
    }

    /**
     * 定时任务示例。
     *
     * <p>如需启用，请取消对应的 {@code @Scheduled} 注解。</p>
     */
    public void execute2() {
        LOGGER.debug("execute2");
    }
}
