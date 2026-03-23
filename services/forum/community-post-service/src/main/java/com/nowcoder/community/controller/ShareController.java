package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Event;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * 分享图片控制器。
 */
@Controller
public class ShareController implements CommunityConstant {

    private final EventProducer eventProducer;
    private final String domain;
    private final String contextPath;
    private final String shareBucketUrl;

    public ShareController(EventProducer eventProducer,
                           @Value("${community.path.domain}") String domain,
                           @Value("${server.servlet.context-path}") String contextPath,
                           @Value("${qiniu.bucket.share.url}") String shareBucketUrl) {
        this.eventProducer = eventProducer;
        this.domain = domain;
        this.contextPath = contextPath;
        this.shareBucketUrl = shareBucketUrl;
    }

    /**
     * 触发分享长图生成任务。
     *
     * @param htmlUrl 被截图的页面地址
     * @return 分享结果
     */
    @GetMapping("/share")
    @ResponseBody
    public String share(String htmlUrl) {
        if (StringUtils.isBlank(htmlUrl) || !isValidShareUrl(htmlUrl)) {
            return CommunityUtil.getJSONString(1, "Invalid share url");
        }

        String fileName = CommunityUtil.generateUUID();
        Event event = new Event()
                .setTopic(TOPIC_SHARE)
                .setData("htmlUrl", htmlUrl)
                .setData("fileName", fileName)
                .setData("suffix", ".png");
        eventProducer.fireEvent(event);

        Map<String, Object> data = new HashMap<>();
        data.put("shareUrl", shareBucketUrl + "/" + fileName);
        return CommunityUtil.getJSONString(0, null, data);
    }

    /**
     * 校验分享地址是否属于当前站点，避免被用作任意 URL 截图入口。
     *
     * @param htmlUrl 目标地址
     * @return 是否合法
     */
    private boolean isValidShareUrl(String htmlUrl) {
        try {
            URI target = URI.create(htmlUrl);
            URI base = URI.create(domain);
            String scheme = target.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                return false;
            }
            if (base.getHost() != null && !base.getHost().equalsIgnoreCase(target.getHost())) {
                return false;
            }

            int basePort = base.getPort() == -1
                    ? ("https".equalsIgnoreCase(base.getScheme()) ? 443 : 80)
                    : base.getPort();
            int targetPort = target.getPort() == -1
                    ? ("https".equalsIgnoreCase(scheme) ? 443 : 80)
                    : target.getPort();
            if (base.getHost() != null && basePort != targetPort) {
                return false;
            }

            String path = target.getPath();
            return path != null && path.startsWith(contextPath);
        } catch (Exception ex) {
            return false;
        }
    }
}
