package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.client.FollowClient;
import com.nowcoder.community.client.LikeClient;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.ApiResponseUtils;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户资料与账号设置控制器。
 *
 * <p>该控制器同时保留了两条头像处理链路：</p>
 *
 * <p>1. 当前主流程：生成七牛直传凭证，由前端直接上传到对象存储。</p>
 * <p>2. 兼容旧流程：保留本地文件上传与回显接口，便于历史页面逐步迁移。</p>
 */
@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
    private static final int UPLOAD_TOKEN_EXPIRE_SECONDS = 3600;
    private static final int FILE_STREAM_BUFFER_SIZE = 1024;

    private final String uploadPath;
    private final String domain;
    private final String contextPath;
    private final UserService userService;
    private final HostHolder hostHolder;
    private final LikeClient likeClient;
    private final FollowClient followClient;
    private final String accessKey;
    private final String secretKey;
    private final String headerBucketName;
    private final String headerBucketUrl;
    private final String uploadHost;

    public UserController(
            UserService userService,
            HostHolder hostHolder,
            LikeClient likeClient,
            FollowClient followClient,
            @Value("${community.path.upload}") String uploadPath,
            @Value("${community.path.domain}") String domain,
            @Value("${server.servlet.context-path}") String contextPath,
            @Value("${qiniu.key.access}") String accessKey,
            @Value("${qiniu.key.secret}") String secretKey,
            @Value("${qiniu.bucket.header.name}") String headerBucketName,
            @Value("${qiniu.bucket.header.url}") String headerBucketUrl,
            @Value("${qiniu.bucket.upload.host}") String uploadHost) {
        this.userService = userService;
        this.hostHolder = hostHolder;
        this.likeClient = likeClient;
        this.followClient = followClient;
        this.uploadPath = uploadPath;
        this.domain = domain;
        this.contextPath = contextPath;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.headerBucketName = headerBucketName;
        this.headerBucketUrl = headerBucketUrl;
        this.uploadHost = uploadHost;
    }

    /**
     * 修改当前登录用户密码。
     *
     * @param oldPassword 原密码
     * @param newPassword 新密码
     * @return 统一 JSON 结果
     */
    @RequestMapping(path = "/setting/updatepassword", method = RequestMethod.POST)
    @ResponseBody
    public String updatePassword(String oldPassword, String newPassword) {
        if (oldPassword == null || newPassword == null) {
            return CommunityUtil.getJSONString(1, "提交数据不能为空");
        }

        User user = hostHolder.getUser();
        if (!userService.matchesPassword(user, oldPassword)) {
            return CommunityUtil.getJSONString(1, "原始密码错误");
        }

        userService.updatePassword(user.getId(), userService.encodePassword(newPassword));
        return CommunityUtil.getJSONString(0);
    }

    /**
     * 加载账号设置页，并生成七牛上传凭证。
     *
     * @param model 页面模型
     * @return 账号设置页
     */
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model) {
        String fileName = CommunityUtil.generateUUID();
        StringMap policy = new StringMap();
        policy.put("returnBody", CommunityUtil.getJSONString(0));

        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(headerBucketName, fileName, UPLOAD_TOKEN_EXPIRE_SECONDS, policy);

        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("fileName", fileName);
        // 上传域名从后端统一下发，避免前端脚本写死对象存储地域地址。
        model.addAttribute("uploadHost", uploadHost);
        return "/site/setting";
    }

    /**
     * 更新当前用户头像地址。
     *
     * @param fileName 上传后的文件名
     * @return 统一 JSON 结果
     */
    @RequestMapping(path = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJSONString(1, "文件名不能为空");
        }

        userService.updateHeader(hostHolder.getUser().getId(), buildHeaderUrl(fileName));
        return CommunityUtil.getJSONString(0);
    }

    /**
     * 兼容旧版本地上传链路。
     *
     * <p>当前主流程已经切换到七牛直传，此接口仅用于保留历史页面兼容能力。</p>
     *
     * @param headerImage 头像文件
     * @param model       页面模型
     * @return 处理后的页面跳转
     */
    @Deprecated
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String upload(MultipartFile headerImage, Model model) {
        if (headerImage == null || StringUtils.isBlank(headerImage.getOriginalFilename())) {
            model.addAttribute("error", "您还没有选择图片!");
            return "/site/setting";
        }

        String suffix = StringUtils.substringAfterLast(headerImage.getOriginalFilename(), ".");
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件格式错误!");
            return "/site/setting";
        }

        String fileName = CommunityUtil.generateUUID() + "." + suffix;
        ensureUploadDirectoryExists();
        File destination = new File(uploadPath, fileName);
        try {
            headerImage.transferTo(destination);
        } catch (IOException ex) {
            LOGGER.error("上传头像失败，fileName={}", fileName, ex);
            throw new RuntimeException("上传图片失败，服务器运行异常!", ex);
        }

        User user = hostHolder.getUser();
        String url = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), url);
        return "redirect:/index";
    }

    /**
     * 兼容旧版本地头像读取链路。
     *
     * @param fileName 头像文件名
     * @param response HTTP 响应
     */
    @Deprecated
    @RequestMapping(path = "/header/{filename}", method = RequestMethod.GET)
    public void getFile(@PathVariable("filename") String fileName, HttpServletResponse response) {
        String suffix = StringUtils.substringAfterLast(fileName, ".");
        if (StringUtils.isBlank(suffix)) {
            response.setContentType("application/octet-stream");
        } else {
            response.setContentType("image/" + suffix);
        }

        File sourceFile = new File(uploadPath, fileName);
        try (FileInputStream inputStream = new FileInputStream(sourceFile);
                ServletOutputStream outputStream = response.getOutputStream()) {
            byte[] buffer = new byte[FILE_STREAM_BUFFER_SIZE];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
        } catch (IOException ex) {
            LOGGER.error("读取头像文件失败，fileName={}", fileName, ex);
        }
    }

    /**
     * 加载用户个人主页。
     *
     * @param model  页面模型
     * @param userId 用户 ID
     * @return 个人主页模板
     */
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(Model model, @PathVariable(name = "userId") int userId) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在!");
        }
        model.addAttribute("user", user);

        Integer userLikeCount = ApiResponseUtils.unwrap(likeClient.getUserLikeCount(userId));
        model.addAttribute("userLikeCount", userLikeCount == null ? 0 : userLikeCount);

        Long followeeCount = ApiResponseUtils.unwrap(followClient.getFolloweeCount(userId, ENTITY_TYPE_USER));
        model.addAttribute("followeeCount", followeeCount == null ? 0L : followeeCount);

        Long followerCount = ApiResponseUtils.unwrap(followClient.getFollowerCount(ENTITY_TYPE_USER, userId));
        model.addAttribute("followerCount", followerCount == null ? 0L : followerCount);

        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            Boolean followed = ApiResponseUtils.unwrap(
                    followClient.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId));
            hasFollowed = Boolean.TRUE.equals(followed);
        }
        model.addAttribute("hasFollowed", hasFollowed);
        return "/site/profile";
    }

    /**
     * 生成头像访问地址。
     *
     * @param fileName 文件名
     * @return 完整访问地址
     */
    private String buildHeaderUrl(String fileName) {
        return StringUtils.removeEnd(headerBucketUrl, "/") + "/" + fileName;
    }

    /**
     * 确保本地上传目录存在。
     */
    private void ensureUploadDirectoryExists() {
        File directory = new File(uploadPath);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("创建上传目录失败: " + uploadPath);
        }
    }
}
