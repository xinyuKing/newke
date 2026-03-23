package com.nowcoder.community.service;

import com.nowcoder.community.dao.AlphaDao;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Date;
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

@Service
public class AlphaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlphaService.class);
    private static final int DEMO_USER_ID = 1;

    private final AlphaDao alphaDao;
    private final DiscussPostMapper discussPostMapper;
    private final TransactionTemplate transactionTemplate;

    public AlphaService(
            AlphaDao alphaDao, DiscussPostMapper discussPostMapper, TransactionTemplate transactionTemplate) {
        this.alphaDao = alphaDao;
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

    public String find() {
        return alphaDao.select();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public Object save1() {
        insertDemoPost("hello", "新人报道");
        Integer.valueOf("error");
        return "ok";
    }

    public Object save2() {
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        return transactionTemplate.execute(new TransactionCallback<Object>() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                insertDemoPost("hello01", "新人 01 报道");
                Integer.valueOf("error");
                return "ok";
            }
        });
    }

    @Async
    public void execute1() {
        LOGGER.debug("execute1");
    }

    public void execute2() {
        LOGGER.debug("execute2");
    }

    private void insertDemoPost(String title, String content) {
        DiscussPost post = new DiscussPost();
        post.setUserId(DEMO_USER_ID);
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostMapper.insertDiscussPostRows(post);
    }
}
