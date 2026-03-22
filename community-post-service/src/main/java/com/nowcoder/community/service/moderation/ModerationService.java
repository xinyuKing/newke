package com.nowcoder.community.service.moderation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ModerationService {
    @Value("${community.moderation.enabled:true}")
    private boolean enabled;

    @Value("${community.moderation.max-reasons:3}")
    private int maxReasons;

    @Value("${community.moderation.mode:rules}")
    private String mode;

    @Value("${community.moderation.llm.fail-open:false}")
    private boolean llmFailOpen;

    @Autowired(required = false)
    private LlmModerationClient llmModerationClient;

    private List<Rule> rules;

    @PostConstruct
    public void init() {
        rules = new ArrayList<>();
        rules.add(new Rule("违法违规", "疑似涉及涉赌、诈骗、毒品、枪支或违禁交易等内容",
                Pattern.compile("(赌博|博彩|六合彩|时时彩|刷单|刷流水|返利|诈骗|骗局|骗钱|洗钱|传销|毒品|冰毒|K粉|大麻|摇头丸|制毒|枪支|制枪|买枪|卖枪|爆炸物|炸弹)")));
        rules.add(new Rule("色情低俗", "疑似包含色情或性暗示内容",
                Pattern.compile("(色情|裸聊|约炮|一夜情|成人视频|强奸|援交|裸照|露点|黄图|自拍偷拍|萝莉|未成年.*(性|色情))")));
        rules.add(new Rule("暴力血腥/自伤", "疑似包含暴力血腥或自伤自杀内容",
                Pattern.compile("(血腥|砍人|杀人|爆头|虐待|自杀|轻生|割腕|跳楼|自残|危险挑战)")));
        rules.add(new Rule("仇恨辱骂", "疑似包含人身攻击、侮辱或仇恨歧视言论",
                Pattern.compile("(傻逼|垃圾人|废物|狗东西|去死|种族歧视|地域黑|性别歧视)")));
        rules.add(new Rule("未成年人保护", "疑似涉及未成年人不当内容",
                Pattern.compile("(未成年|幼女|小学生|初中生|高中生).*?(性|裸|约会|交易)")));
        rules.add(new Rule("隐私泄露", "疑似包含手机号、身份证或联系方式等隐私信息",
                Pattern.compile("(1[3-9]\\d{9}|\\d{17}[0-9Xx]|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}|QQ\\s*[:：]?\\s*\\d{5,12}|(微信|vx|v信|wx)\\s*[:：]?\\s*[a-zA-Z][-_a-zA-Z0-9]{5,19})")));
        rules.add(new Rule("广告引流", "疑似包含营销推广、引流或外部联系方式",
                Pattern.compile("(加微信|私聊|引流|关注公众号|二维码|扫码|代理|招商|推广|低价|全网最低|联系我|vx|v信|wx)")));
        rules.add(new Rule("侵权盗版", "疑似包含盗版资源、破解或外挂内容",
                Pattern.compile("(盗版|破解|外挂|资源合集|全集下载|网盘链接|破解软件|激活码)")));
        rules.add(new Rule("不实信息", "疑似包含夸大或不实信息",
                Pattern.compile("(包赚|稳赚不赔|百分百有效|绝对有效|保过|内部消息|稳赚|一夜暴富)")));
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
        String selected = mode == null ? "rules" : mode.trim().toLowerCase(Locale.ROOT);
        if ("llm".equals(selected)) {
            ModerationResult result = reviewByLlm(scene, title, content, mediaJson);
            if (result != null) {
                return result;
            }
            return llmFailOpen ? ModerationResult.pass()
                    : ModerationResult.reject(Collections.singletonList("审核服务繁忙，请稍后重试"), Collections.singletonList("system_error"));
        }
        if ("hybrid".equals(selected)) {
            ModerationResult ruleResult = reviewByRules(title, content, mediaJson);
            if (!ruleResult.isPass()) {
                return ruleResult;
            }
            ModerationResult llmResult = reviewByLlm(scene, title, content, mediaJson);
            if (llmResult != null) {
                return llmResult;
            }
            return llmFailOpen ? ModerationResult.pass()
                    : ModerationResult.reject(Collections.singletonList("审核服务繁忙，请稍后重试"), Collections.singletonList("system_error"));
        }
        return reviewByRules(title, content, mediaJson);
    }

    private ModerationResult reviewByLlm(String scene, String title, String content, String mediaJson) {
        if (llmModerationClient == null) {
            return null;
        }
        try {
            return llmModerationClient.review(scene, title, content, mediaJson);
        } catch (Exception e) {
            return null;
        }
    }

    private ModerationResult reviewByRules(String title, String content, String mediaJson) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(title)) {
            sb.append(title).append('\n');
        }
        if (StringUtils.isNotBlank(content)) {
            sb.append(content);
        }
        String text = sb.toString();
        if (StringUtils.isBlank(text) && StringUtils.isBlank(mediaJson)) {
            return ModerationResult.pass();
        }
        String normalized = normalize(text);
        Set<String> tags = new HashSet<>();
        List<String> reasons = new ArrayList<>();
        for (Rule rule : rules) {
            if (rule.matches(text, normalized)) {
                tags.add(rule.tag);
                reasons.add(rule.reason);
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
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.replaceAll("[\\s\\p{Punct}，。！？、；：“”‘’（）【】《》…—-]", "");
    }

    private static class Rule {
        private final String tag;
        private final String reason;
        private final Pattern pattern;

        private Rule(String tag, String reason, Pattern pattern) {
            this.tag = tag;
            this.reason = reason;
            this.pattern = pattern;
        }

        private boolean matches(String text, String normalized) {
            if (text == null) {
                text = "";
            }
            if (normalized == null) {
                normalized = "";
            }
            return pattern.matcher(text).find() || pattern.matcher(normalized).find();
        }
    }
}
