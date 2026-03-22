package com.nowcoder.community.controller.api;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.ApiResponse;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/media")
public class MediaApiController {
    private static final Logger logger = LoggerFactory.getLogger(MediaApiController.class);

    @Autowired
    private HostHolder hostHolder;

    @Value("${community.path.media}")
    private String mediaPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${community.media.allowed-image-types:jpg,jpeg,png,gif,webp}")
    private String allowedImageTypes;

    @Value("${community.media.allowed-video-types:mp4,webm,ogg}")
    private String allowedVideoTypes;

    @Value("${community.media.max-image-mb:10}")
    private long maxImageMb;

    @Value("${community.media.max-video-mb:200}")
    private long maxVideoMb;

    private Set<String> imageTypes;
    private Set<String> videoTypes;

    @PostConstruct
    public void init() {
        imageTypes = parseTypes(allowedImageTypes);
        videoTypes = parseTypes(allowedVideoTypes);
        File baseDir = new File(mediaPath);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            logger.warn("failed to create media base dir: {}", mediaPath);
        }
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam("usage") String usage) {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(403, "not_login");
        }
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "file_empty");
        }
        if (!"post".equals(usage) && !"comment".equals(usage)) {
            return ApiResponse.error(400, "invalid_usage");
        }
        String originalName = file.getOriginalFilename();
        if (StringUtils.isBlank(originalName) || !originalName.contains(".")) {
            return ApiResponse.error(400, "invalid_filename");
        }
        String suffix = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        boolean isImage = imageTypes.contains(suffix);
        boolean isVideo = videoTypes.contains(suffix);
        if (!isImage && !isVideo) {
            return ApiResponse.error(400, "unsupported_type");
        }
        long maxBytes = (isVideo ? maxVideoMb : maxImageMb) * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            return ApiResponse.error(400, "file_too_large");
        }

        String fileName = CommunityUtil.generateUUID() + "." + suffix;
        File dir = new File(mediaPath, usage);
        if (!dir.exists() && !dir.mkdirs()) {
            return ApiResponse.error(500, "storage_unavailable");
        }
        File target = new File(dir, fileName);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            logger.error("media upload failed", e);
            return ApiResponse.error(500, "upload_failed");
        }

        String url = domain + contextPath + "/media/" + usage + "/" + fileName;
        Map<String, Object> data = new HashMap<>();
        data.put("url", url);
        data.put("name", originalName);
        data.put("type", isVideo ? "video" : "image");
        data.put("size", file.getSize());
        return ApiResponse.success(data);
    }

    private Set<String> parseTypes(String types) {
        Set<String> set = new HashSet<>();
        if (types == null) {
            return set;
        }
        for (String item : types.split(",")) {
            String value = item == null ? "" : item.trim().toLowerCase();
            if (!value.isEmpty()) {
                set.add(value);
            }
        }
        return set;
    }
}
