package com.nowcoder.community.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 媒体服务的 MVC 配置。
 *
 * <p>融合后的前后端都会直接引用论坛媒体 URL，因此这里补上文件系统资源映射，
 * 让上传后的图片与视频可以通过 `/community/media/**` 稳定访问。</p>
 *
 * @author shixi
 * @date 2026-03-22
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Value("${community.path.media}")
    private String mediaPath;

    /**
     * 暴露论坛媒体静态资源目录。
     *
     * @param registry Spring MVC 资源处理器注册器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = mediaPath.replace("\\", "/");
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/media/**")
                .addResourceLocations("file:" + location);
    }
}
