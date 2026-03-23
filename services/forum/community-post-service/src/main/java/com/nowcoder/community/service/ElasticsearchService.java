package com.nowcoder.community.service;

import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * 帖子搜索与索引同步服务。
 */
@Service
public class ElasticsearchService {

    private final DiscussPostRepository discussPostRepository;

    public ElasticsearchService(DiscussPostRepository discussPostRepository) {
        this.discussPostRepository = discussPostRepository;
    }

    /**
     * 写入帖子索引。
     *
     * @param discussPost 帖子实体
     */
    public void saveDiscussPost(DiscussPost discussPost) {
        discussPostRepository.save(discussPost);
    }

    /**
     * 删除帖子索引。
     *
     * @param id 帖子 ID
     */
    public void deleteDiscussPost(int id) {
        discussPostRepository.deleteById(id);
    }

    /**
     * 搜索帖子。
     *
     * @param keyword 搜索关键词
     * @param current 当前页，从 0 开始
     * @param limit 每页条数
     * @return 搜索结果
     */
    public Page<DiscussPost> searchDiscussPost(String keyword, int current, int limit) {
        Sort sort = Sort.by(
                Sort.Order.desc("type"),
                Sort.Order.desc("score"),
                Sort.Order.desc("createTime")
        );
        if (keyword == null || keyword.isBlank()) {
            return Page.empty(PageRequest.of(current, limit, sort));
        }

        String normalizedKeyword = keyword.trim();
        return discussPostRepository.findByTitleContainingOrContentContaining(
                normalizedKeyword,
                normalizedKeyword,
                PageRequest.of(current, limit, sort)
        );
    }
}
