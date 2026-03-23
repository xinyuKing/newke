package com.nowcoder.community.service.moderation;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ModerationService {

    private static final String SYSTEM_BUSY_MESSAGE = "审核服务繁忙，请稍后重试";

    private final boolean enabled;
    private final int maxReasons;
    private final String mode;
    private final boolean llmFailOpen;
    private final LlmModerationClient llmModerationClient;

    private List<Rule> rules = Collections.emptyList();

    public ModerationService(
            @Value("${community.moderation.enabled:true}") boolean enabled,
            @Value("${community.moderation.max-reasons:3}") int maxReasons,
            @Value("${community.moderation.mode:rules}") String mode,
            @Value("${community.moderation.llm.fail-open:false}") boolean llmFailOpen,
            ObjectProvider<LlmModerationClient> llmModerationClientProvider) {
        this.enabled = enabled;
        this.maxReasons = maxReasons;
        this.mode = mode;
        this.llmFailOpen = llmFailOpen;
        this.llmModerationClient = llmModerationClientProvider.getIfAvailable();
    }

    @PostConstruct
    public void init() {
        rules = List.of(
                keywordRule(
                        "违法违规",
                        "疑似涉及赌博、诈骗、毒品、枪支或违禁交易等内容",
                        "赌博",
                        "博彩",
                        "六合彩",
                        "刷单",
                        "返利",
                        "诈骗",
                        "骗钱",
                        "洗钱",
                        "传销",
                        "毒品",
                        "冰毒",
                        "大麻",
                        "枪支",
                        "爆炸物"),
                keywordRule("色情低俗", "疑似包含色情、性暗示或成人内容", "色情", "裸聊", "约炮", "成人视频", "黄色网站", "露点", "黄片", "成人影片"),
                keywordRule("暴力血腥与自伤", "疑似包含暴力血腥、自残或自杀内容", "杀人", "砍人", "爆头", "虐待", "自杀", "轻生", "跳楼", "自残"),
                keywordRule("仇恨辱骂", "疑似包含人身攻击、歧视或仇恨言论", "傻逼", "废物", "去死", "种族歧视", "地域黑", "性别歧视"),
                new Rule("未成年人保护", "疑似涉及未成年人不当内容", Pattern.compile("(未成年|幼女|小学生|初中生|高中生).*(色情|约会|交易)")),
                new Rule(
                        "隐私泄露",
                        "疑似包含手机号、身份证号、邮箱、QQ 或微信等隐私信息",
                        Pattern.compile("(1[3-9]\\d{9}|\\d{17}[0-9Xx]|"
                                + "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}|"
                                + "QQ\\s*[:：]?\\s*\\d{5,12}|"
                                + "(微信|vx|v信|wx)\\s*[:：]?\\s*[a-zA-Z][-_a-zA-Z0-9]{5,19})")),
                keywordRule(
                        "广告引流",
                        "疑似包含营销推广、引流或站外联系方式",
                        "加微信",
                        "私聊",
                        "公众号",
                        "二维码",
                        "扫码",
                        "代理",
                        "招商",
                        "推广",
                        "全网最低",
                        "vx",
                        "v信",
                        "wx"),
                keywordRule("侵权盗版", "疑似包含盗版资源、破解软件或外挂内容", "盗版", "破解", "外挂", "网盘链接", "资源合集", "激活码"),
                keywordRule("不实承诺", "疑似包含夸大、欺诈或不实承诺", "包赚", "稳赚不赔", "百分之百有效", "绝对有效", "保过", "内部消息", "一夜暴富"));
    }

    public ModerationResult reviewPost(String title, String content, String mediaJson) {
        return review("post", title, content, mediaJson);
    }

    public ModerationResult reviewComment(String content, String mediaJson) {
        return review("comment", null, content, mediaJson);
    }

    private ModerationResult review(String scene, String title, String content, String mediaJson) {
        if (!enabled) {
            return ModerationResult.pass();
        }

        String selectedMode = StringUtils.defaultIfBlank(mode, "rules").trim().toLowerCase(Locale.ROOT);
        if ("llm".equals(selectedMode)) {
            return reviewWithLlmOnly(scene, title, content, mediaJson);
        }

        if ("hybrid".equals(selectedMode)) {
            ModerationResult ruleResult = reviewByRules(title, content, mediaJson);
            if (!ruleResult.isPass()) {
                return ruleResult;
            }
            return reviewWithLlmFallback(scene, title, content, mediaJson);
        }

        return reviewByRules(title, content, mediaJson);
    }

    private ModerationResult reviewWithLlmOnly(String scene, String title, String content, String mediaJson) {
        ModerationResult llmResult = reviewByLlm(scene, title, content, mediaJson);
        if (llmResult != null) {
            return llmResult;
        }
        return fallbackWhenLlmUnavailable();
    }

    private ModerationResult reviewWithLlmFallback(String scene, String title, String content, String mediaJson) {
        ModerationResult llmResult = reviewByLlm(scene, title, content, mediaJson);
        if (llmResult != null) {
            return llmResult;
        }
        return fallbackWhenLlmUnavailable();
    }

    private ModerationResult fallbackWhenLlmUnavailable() {
        if (llmFailOpen) {
            return ModerationResult.pass();
        }
        return ModerationResult.reject(
                Collections.singletonList(SYSTEM_BUSY_MESSAGE), Collections.singletonList("system_error"));
    }

    private ModerationResult reviewByLlm(String scene, String title, String content, String mediaJson) {
        if (llmModerationClient == null) {
            return null;
        }
        try {
            return llmModerationClient.review(scene, title, content, mediaJson);
        } catch (Exception ex) {
            return null;
        }
    }

    private ModerationResult reviewByRules(String title, String content, String mediaJson) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotBlank(title)) {
            builder.append(title).append('\n');
        }
        if (StringUtils.isNotBlank(content)) {
            builder.append(content);
        }

        String text = builder.toString();
        if (StringUtils.isBlank(text) && StringUtils.isBlank(mediaJson)) {
            return ModerationResult.pass();
        }

        String normalized = normalize(text);
        Set<String> tags = new HashSet<>();
        List<String> reasons = new ArrayList<>();
        for (Rule rule : rules) {
            if (rule.matches(text, normalized)) {
                tags.add(rule.tag());
                reasons.add(rule.reason());
                if (reasons.size() >= maxReasons) {
                    break;
                }
            }
        }
        if (reasons.isEmpty()) {
            return ModerationResult.pass();
        }
        return ModerationResult.reject(reasons, new ArrayList<>(tags));
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        String lowerText = text.toLowerCase(Locale.ROOT);
        return lowerText.replaceAll("[\\s\\p{Punct}，。！？、】【《》“”‘’（）…]", "");
    }

    private Rule keywordRule(String tag, String reason, String... keywords) {
        return new Rule(tag, reason, Pattern.compile("(" + String.join("|", keywords) + ")"));
    }

    private record Rule(String tag, String reason, Pattern pattern) {
        private boolean matches(String text, String normalized) {
            String safeText = text == null ? "" : text;
            String safeNormalized = normalized == null ? "" : normalized;
            return pattern.matcher(safeText).find()
                    || pattern.matcher(safeNormalized).find();
        }
    }
}
