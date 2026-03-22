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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@RestController
@RequestMapping("/api")
public class MessageApiController implements CommunityConstant {
    @Autowired
    private MessageService messageService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserClient userClient;

    @GetMapping("/messages/conversations")
    public ApiResponse<Map<String, Object>> conversations(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int limit) {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(403, "not_login");
        }
        int offset = (page - 1) * limit;
        int rows = messageService.findConversationCount(user.getId());
        List<Message> conversationList = messageService.findConversations(user.getId(), offset, limit);
        List<Map<String, Object>> conversations = new ArrayList<>();
        for (Message conversation : conversationList) {
            Map<String, Object> map = new HashMap<>();
            map.put("conversation", conversation);
            map.put("letterCount", messageService.findLetterCount(conversation.getConversationId()));
            map.put("unreadCount", messageService.findLetterUnreadCount(user.getId(), conversation.getConversationId()));
            int targetId = user.getId() == conversation.getFromId() ? conversation.getToId() : conversation.getFromId();
            map.put("target", ApiResponseUtils.unwrap(userClient.getUser(targetId)));
            conversations.add(map);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("list", conversations);
        data.put("rows", rows);
        data.put("page", page);
        data.put("limit", limit);
        data.put("letterUnreadCount", messageService.findLetterUnreadCount(user.getId(), null));
        data.put("noticeUnreadCount", messageService.findNoticeUnreadCount(user.getId(), null));
        return ApiResponse.success(data);
    }

    @GetMapping("/messages/conversations/{conversationId}")
    public ApiResponse<Map<String, Object>> conversationDetail(@PathVariable String conversationId,
                                                               @RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "10") int limit) {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(403, "not_login");
        }
        int offset = (page - 1) * limit;
        int rows = messageService.findLetterCount(conversationId);
        List<Message> letterList = messageService.findLetters(conversationId, offset, limit);

        List<Map<String, Object>> letters = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        if (letterList != null) {
            for (Message message : letterList) {
                Map<String, Object> map = new HashMap<>();
                map.put("letter", message);
                int fromId = message.getFromId();
                map.put("fromUser", ApiResponseUtils.unwrap(userClient.getUser(fromId)));
                letters.add(map);
                if (message.getStatus() == 0 && message.getToId() == user.getId()) {
                    ids.add(message.getId());
                }
            }
        }
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("list", letters);
        data.put("rows", rows);
        data.put("page", page);
        data.put("limit", limit);
        data.put("target", getLetterTarget(conversationId));
        return ApiResponse.success(data);
    }

    @PostMapping("/messages")
    public ApiResponse<Void> send(@RequestBody Map<String, Object> body) {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(403, "not_login");
        }
        String toName = body.get("toName") == null ? null : body.get("toName").toString();
        String content = body.get("content") == null ? null : body.get("content").toString();
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
        if (message.getFromId() < message.getToId()) {
            message.setConversationId(message.getFromId() + "_" + message.getToId());
        } else {
            message.setConversationId(message.getToId() + "_" + message.getFromId());
        }
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
    public ApiResponse<Map<String, Object>> noticeDetail(@PathVariable String topic,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int limit) {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(403, "not_login");
        }
        int offset = (page - 1) * limit;
        int rows = messageService.findNoticeCount(user.getId(), topic);
        List<Message> noticeList = messageService.findNotices(user.getId(), topic, offset, limit);
        List<Map<String, Object>> noticeVoList = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        if (noticeList != null) {
            for (Message notice : noticeList) {
                Map<String, Object> noticeVo = new HashMap<>();
                noticeVo.put("notice", notice);
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
                noticeVo.put("user", ApiResponseUtils.unwrap(userClient.getUser((Integer) data.get("userId"))));
                noticeVo.put("entityType", data.get("entityType"));
                noticeVo.put("entityId", data.get("entityId"));
                noticeVo.put("postId", data.get("postId"));
                noticeVo.put("fromUser", ApiResponseUtils.unwrap(userClient.getUser(notice.getFromId())));
                noticeVoList.add(noticeVo);

                if (notice.getStatus() == 0 && notice.getToId() == user.getId()) {
                    ids.add(notice.getId());
                }
            }
        }
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", noticeVoList);
        result.put("rows", rows);
        result.put("page", page);
        result.put("limit", limit);
        return ApiResponse.success(result);
    }

    private User getLetterTarget(String conversationId) {
        String[] ids = conversationId.split("_");
        int id0 = Integer.parseInt(ids[0]);
        int id1 = Integer.parseInt(ids[1]);
        if (hostHolder.getUser().getId() == id0) {
            return ApiResponseUtils.unwrap(userClient.getUser(id1));
        } else {
            return ApiResponseUtils.unwrap(userClient.getUser(id0));
        }
    }

    private Map<String, Object> buildNoticeSummary(int userId, String topic) {
        Message message = messageService.findLatestNotice(userId, topic);
        if (message == null) {
            return null;
        }
        Map<String, Object> messageVo = new HashMap<>();
        messageVo.put("message", message);
        String content = HtmlUtils.htmlUnescape(message.getContent());
        Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
        messageVo.put("user", ApiResponseUtils.unwrap(userClient.getUser((Integer) data.get("userId"))));
        messageVo.put("entityType", data.get("entityType"));
        messageVo.put("entityId", data.get("entityId"));
        messageVo.put("postId", data.get("postId"));
        messageVo.put("count", messageService.findNoticeCount(userId, topic));
        messageVo.put("unreadCount", messageService.findNoticeUnreadCount(userId, topic));
        return messageVo;
    }
}
