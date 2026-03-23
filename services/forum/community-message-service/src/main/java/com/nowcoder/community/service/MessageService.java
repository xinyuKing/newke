package com.nowcoder.community.service;

import com.nowcoder.community.dao.MessageMapper;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Objects;

/**
 * Service for private letters and system notices.
 */
@Service
public class MessageService {

    private static final int MESSAGE_STATUS_UNREAD = 0;
    private static final int MESSAGE_STATUS_READ = 1;

    private final MessageMapper messageMapper;
    private final SensitiveFilter sensitiveFilter;

    public MessageService(MessageMapper messageMapper, SensitiveFilter sensitiveFilter) {
        this.messageMapper = messageMapper;
        this.sensitiveFilter = sensitiveFilter;
    }

    public List<Message> findConversations(int userId, int offset, int limit) {
        return messageMapper.selectConversations(userId, offset, limit);
    }

    public int findConversationCount(int userId) {
        return messageMapper.selectConversationCount(userId);
    }

    public List<Message> findLetters(String conversationId, int offset, int limit) {
        return messageMapper.selectLetters(conversationId, offset, limit);
    }

    public int findLetterCount(String conversationId) {
        return messageMapper.selectLetterCount(conversationId);
    }

    public int findLetterUnreadCount(int userId, String conversationId) {
        return messageMapper.selectLetterUnreadCount(userId, conversationId);
    }

    public int addMessage(Message message) {
        Message currentMessage = Objects.requireNonNull(message, "message must not be null");
        currentMessage.setContent(sanitizeContent(currentMessage.getContent()));
        return messageMapper.insertMessage(currentMessage);
    }

    public int readMessage(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return messageMapper.updateStatus(ids, MESSAGE_STATUS_READ);
    }

    public Message findLatestNotice(int userId, String topic) {
        return messageMapper.selectLatestNotice(userId, topic);
    }

    public int findNoticeCount(int userId, String topic) {
        return messageMapper.selectNoticeCount(userId, topic);
    }

    public int findNoticeUnreadCount(int userId, String topic) {
        return messageMapper.selectNoticeUnreadCount(userId, topic);
    }

    public List<Message> findNotices(int userId, String topic, int offset, int limit) {
        return messageMapper.selectNotices(userId, topic, offset, limit);
    }

    public boolean isUnreadForUser(Message message, int userId) {
        return message != null
                && message.getStatus() == MESSAGE_STATUS_UNREAD
                && message.getToId() == userId;
    }

    private String sanitizeContent(String content) {
        if (content == null) {
            return null;
        }
        return sensitiveFilter.filter(HtmlUtils.htmlEscape(content));
    }
}
