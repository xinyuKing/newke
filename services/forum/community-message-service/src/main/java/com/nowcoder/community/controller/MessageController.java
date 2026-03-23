package com.nowcoder.community.controller;

import com.alibaba.fastjson2.JSONObject;
import com.nowcoder.community.client.UserClient;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.ApiResponseUtils;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

/**
 * Thymeleaf pages for private letters and system notices.
 */
@Controller
public class MessageController implements CommunityConstant {

    private static final int PAGE_LIMIT = 5;

    private final MessageService messageService;
    private final HostHolder hostHolder;
    private final UserClient userClient;

    public MessageController(MessageService messageService, HostHolder hostHolder, UserClient userClient) {
        this.messageService = messageService;
        this.hostHolder = hostHolder;
        this.userClient = userClient;
    }

    @GetMapping("/letter/list")
    public String getLetterList(Model model, Page page) {
        User currentUser = hostHolder.getUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        initPage(page, "/letter/list", messageService.findConversationCount(currentUser.getId()));

        List<Message> conversationList =
                messageService.findConversations(currentUser.getId(), page.getOffset(), page.getLimit());
        List<Map<String, Object>> conversations = new ArrayList<>();
        for (Message conversation : conversationList) {
            conversations.add(buildConversationView(currentUser.getId(), conversation));
        }

        model.addAttribute("conversations", conversations);
        fillUnreadCounts(model, currentUser.getId());
        return "/site/letter";
    }

    @GetMapping("/letter/detail/{conversationId}")
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Model model, Page page) {
        User currentUser = hostHolder.getUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        initPage(page, "/letter/detail/" + conversationId, messageService.findLetterCount(conversationId));

        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> letters = new ArrayList<>();
        List<Integer> unreadIds = new ArrayList<>();
        for (Message message : letterList) {
            letters.add(buildLetterView(message));
            if (messageService.isUnreadForUser(message, currentUser.getId())) {
                unreadIds.add(message.getId());
            }
        }

        messageService.readMessage(unreadIds);
        model.addAttribute("letters", letters);
        model.addAttribute("target", getLetterTarget(conversationId, currentUser.getId()));
        return "/site/letter-detail";
    }

    @PostMapping("/letter/send")
    @ResponseBody
    public String addMessage(String toName, String content) {
        User currentUser = hostHolder.getUser();
        if (currentUser == null) {
            return CommunityUtil.getJSONString(403, "\u60a8\u8fd8\u6ca1\u6709\u767b\u5f55");
        }
        if (StringUtils.isBlank(toName)) {
            return CommunityUtil.getJSONString(1, "\u76ee\u6807\u7528\u6237\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (StringUtils.isBlank(content)) {
            return CommunityUtil.getJSONString(1, "\u79c1\u4fe1\u5185\u5bb9\u4e0d\u80fd\u4e3a\u7a7a");
        }

        User target = ApiResponseUtils.unwrap(userClient.getUserByName(toName));
        if (target == null) {
            return CommunityUtil.getJSONString(1, "\u76ee\u6807\u7528\u6237\u4e0d\u5b58\u5728");
        }

        Message message = new Message();
        message.setFromId(currentUser.getId());
        message.setToId(target.getId());
        message.setCreateTime(new Date());
        message.setStatus(0);
        message.setContent(content);
        message.setConversationId(buildConversationId(message.getFromId(), message.getToId()));
        messageService.addMessage(message);
        return CommunityUtil.getJSONString(0);
    }

    @GetMapping("/notice/list")
    public String getNoticeList(Model model) {
        User currentUser = hostHolder.getUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("commentNotice", buildNoticeSummary(currentUser.getId(), TOPIC_COMMENT));
        model.addAttribute("likeNotice", buildNoticeSummary(currentUser.getId(), TOPIC_LIKE));
        model.addAttribute("followNotice", buildNoticeSummary(currentUser.getId(), TOPIC_FOLLOW));
        fillUnreadCounts(model, currentUser.getId());
        return "/site/notice";
    }

    @GetMapping("/notice/detail/{topic}")
    public String getNoticeDetail(Model model, Page page, @PathVariable("topic") String topic) {
        User currentUser = hostHolder.getUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        initPage(page, "/notice/detail/" + topic, messageService.findNoticeCount(currentUser.getId(), topic));

        List<Message> noticeList =
                messageService.findNotices(currentUser.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String, Object>> notices = new ArrayList<>();
        List<Integer> unreadIds = new ArrayList<>();
        for (Message notice : noticeList) {
            notices.add(buildNoticeDetailView(notice));
            if (messageService.isUnreadForUser(notice, currentUser.getId())) {
                unreadIds.add(notice.getId());
            }
        }

        messageService.readMessage(unreadIds);
        model.addAttribute("notices", notices);
        return "/site/notice-detail";
    }

    private void initPage(Page page, String path, int rows) {
        page.setLimit(PAGE_LIMIT);
        page.setPath(path);
        page.setRows(rows);
    }

    private Map<String, Object> buildConversationView(int currentUserId, Message conversation) {
        Map<String, Object> item = new HashMap<>();
        item.put("conversation", conversation);
        item.put("letterCount", messageService.findLetterCount(conversation.getConversationId()));
        item.put("unreadCount", messageService.findLetterUnreadCount(currentUserId, conversation.getConversationId()));

        int targetId = currentUserId == conversation.getFromId() ? conversation.getToId() : conversation.getFromId();
        item.put("target", ApiResponseUtils.unwrap(userClient.getUser(targetId)));
        return item;
    }

    private Map<String, Object> buildLetterView(Message message) {
        Map<String, Object> item = new HashMap<>();
        item.put("letter", message);
        item.put("fromUser", ApiResponseUtils.unwrap(userClient.getUser(message.getFromId())));
        return item;
    }

    private Map<String, Object> buildNoticeDetailView(Message notice) {
        Map<String, Object> item = new HashMap<>();
        item.put("notice", notice);

        Map<String, Object> data = parseNoticeContent(notice.getContent());
        item.put("user", ApiResponseUtils.unwrap(userClient.getUser((Integer) data.get("userId"))));
        item.put("entityType", data.get("entityType"));
        item.put("entityId", data.get("entityId"));
        item.put("postId", data.get("postId"));
        item.put("fromUser", ApiResponseUtils.unwrap(userClient.getUser(notice.getFromId())));
        return item;
    }

    private User getLetterTarget(String conversationId, int currentUserId) {
        String[] ids = conversationId.split("_");
        int id0 = Integer.parseInt(ids[0]);
        int id1 = Integer.parseInt(ids[1]);
        return currentUserId == id0
                ? ApiResponseUtils.unwrap(userClient.getUser(id1))
                : ApiResponseUtils.unwrap(userClient.getUser(id0));
    }

    private Map<String, Object> buildNoticeSummary(int userId, String topic) {
        Message message = messageService.findLatestNotice(userId, topic);
        if (message == null) {
            return null;
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("message", message);

        Map<String, Object> data = parseNoticeContent(message.getContent());
        summary.put("user", ApiResponseUtils.unwrap(userClient.getUser((Integer) data.get("userId"))));
        summary.put("entityType", data.get("entityType"));
        summary.put("entityId", data.get("entityId"));
        summary.put("postId", data.get("postId"));
        summary.put("count", messageService.findNoticeCount(userId, topic));
        summary.put("unreadCount", messageService.findNoticeUnreadCount(userId, topic));
        return summary;
    }

    private void fillUnreadCounts(Model model, int userId) {
        model.addAttribute("letterUnreadCount", messageService.findLetterUnreadCount(userId, null));
        model.addAttribute("noticeUnreadCount", messageService.findNoticeUnreadCount(userId, null));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseNoticeContent(String content) {
        return JSONObject.parseObject(HtmlUtils.htmlUnescape(content), HashMap.class);
    }

    private String buildConversationId(int fromId, int toId) {
        return fromId < toId ? fromId + "_" + toId : toId + "_" + fromId;
    }
}
