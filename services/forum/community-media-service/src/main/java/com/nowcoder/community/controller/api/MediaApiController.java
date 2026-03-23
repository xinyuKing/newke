package com.nowcoder.community.controller.api;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.ApiResponse;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 媒体上传接口。
 */
@RestController
@RequestMapping("/api/media")
public class MediaApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaApiController.class);
    private static final long BYTES_PER_MB = 1024L * 1024L;
    private static final String POST_USAGE = "post";
    private static final String COMMENT_USAGE = "comment";
    private static final String MEDIA_TYPE_VIDEO = "video";
    private static final String MEDIA_TYPE_IMAGE = "image";

    private final HostHolder hostHolder;
    private final String mediaPath;
    private final String domain;
    private final String contextPath;
    private final String allowedImageTypes;
    private final String allowedVideoTypes;
    private final long maxImageMb;
    private final long maxVideoMb;

    private Set<String> imageTypes;
    private Set<String> videoTypes;

    public MediaApiController(
            HostHolder hostHolder,
            @Value("${community.path.media}") String mediaPath,
            @Value("${community.path.domain}") String domain,
            @Value("${server.servlet.context-path}") String contextPath,
            @Value("${community.media.allowed-image-types:jpg,jpeg,png,gif,webp}") String allowedImageTypes,
            @Value("${community.media.allowed-video-types:mp4,webm,ogg}") String allowedVideoTypes,
            @Value("${community.media.max-image-mb:10}") long maxImageMb,
            @Value("${community.media.max-video-mb:200}") long maxVideoMb) {
        this.hostHolder = hostHolder;
        this.mediaPath = mediaPath;
        this.domain = domain;
        this.contextPath = contextPath;
        this.allowedImageTypes = allowedImageTypes;
        this.allowedVideoTypes = allowedVideoTypes;
        this.maxImageMb = maxImageMb;
        this.maxVideoMb = maxVideoMb;
    }

    @PostConstruct
    public void init() {
        imageTypes = parseTypes(allowedImageTypes);
        videoTypes = parseTypes(allowedVideoTypes);
        File baseDirectory = new File(mediaPath);
        if (!baseDirectory.exists() && !baseDirectory.mkdirs()) {
            LOGGER.warn("创建媒体根目录失败，mediaPath={}", mediaPath);
        }
    }

    /**
     * 上传帖子或评论媒体文件。
     *
     * @param file  媒体文件
     * @param usage 使用场景，仅支持 post / comment
     * @return 上传结果
     */
    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file, @RequestParam("usage") String usage) {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(403, "not_login");
        }
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "file_empty");
        }
        if (!POST_USAGE.equals(usage) && !COMMENT_USAGE.equals(usage)) {
            return ApiResponse.error(400, "invalid_usage");
        }

        String originalName = file.getOriginalFilename();
        if (StringUtils.isBlank(originalName) || !originalName.contains(".")) {
            return ApiResponse.error(400, "invalid_filename");
        }

        String suffix = StringUtils.substringAfterLast(originalName, ".").toLowerCase();
        boolean isImage = imageTypes.contains(suffix);
        boolean isVideo = videoTypes.contains(suffix);
        if (!isImage && !isVideo) {
            return ApiResponse.error(400, "unsupported_type");
        }

        long maxBytes = (isVideo ? maxVideoMb : maxImageMb) * BYTES_PER_MB;
        if (file.getSize() > maxBytes) {
            return ApiResponse.error(400, "file_too_large");
        }

        String fileName = CommunityUtil.generateUUID() + "." + suffix;
        File usageDirectory = new File(mediaPath, usage);
        if (!usageDirectory.exists() && !usageDirectory.mkdirs()) {
            return ApiResponse.error(500, "storage_unavailable");
        }

        File targetFile = new File(usageDirectory, fileName);
        try {
            file.transferTo(targetFile);
        } catch (IOException ex) {
            LOGGER.error("媒体文件上传失败，usage={}, fileName={}", usage, fileName, ex);
            return ApiResponse.error(500, "upload_failed");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("url", buildMediaUrl(usage, fileName));
        data.put("name", originalName);
        data.put("type", isVideo ? MEDIA_TYPE_VIDEO : MEDIA_TYPE_IMAGE);
        data.put("size", file.getSize());
        return ApiResponse.success(data);
    }

    /**
     * 解析允许的文件类型。
     *
     * @param types 配置字符串
     * @return 类型集合
     */
    private Set<String> parseTypes(String types) {
        Set<String> result = new HashSet<>();
        if (types == null) {
            return result;
        }
        for (String item : types.split(",")) {
            String value = item == null ? "" : item.trim().toLowerCase();
            if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 构造媒体访问地址。
     *
     * @param usage    使用场景
     * @param fileName 文件名
     * @return 访问地址
     */
    private String buildMediaUrl(String usage, String fileName) {
        return domain + contextPath + "/media/" + usage + "/" + fileName;
    }
}
