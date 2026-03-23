package com.nowcoder.community.service;

import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.dto.AuthRegisterRequest;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 论坛用户核心领域服务。
 *
 * <p>该服务负责注册、激活、登录凭证管理、用户缓存、权限查询，以及论坛与商城融合后
 * 的跨服务用户注册与密码兼容校验逻辑。</p>
 */
@Service
public class UserService implements CommunityConstant {

    /**
     * Redis 中用户缓存的默认过期时间，单位：秒。
     */
    private static final long USER_CACHE_TTL_SECONDS = 3600L;

    /**
     * 外部服务未显式提供邮箱时使用的占位域名。
     */
    private static final String EXTERNAL_EMAIL_DOMAIN = "ecommerce.local";

    private final UserMapper userMapper;
    private final MailClient mailClient;
    private final TemplateEngine templateEngine;
    private final RedisTemplate<String, Object> redisTemplate;
    private final String domain;
    private final String contextPath;

    /**
     * BCrypt 编码器，用于新密码存储和历史密码迁移。
     */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserMapper userMapper,
                       MailClient mailClient,
                       TemplateEngine templateEngine,
                       RedisTemplate<String, Object> redisTemplate,
                       @Value("${community.path.domain}") String domain,
                       @Value("${server.servlet.context-path}") String contextPath) {
        this.userMapper = userMapper;
        this.mailClient = mailClient;
        this.templateEngine = templateEngine;
        this.redisTemplate = redisTemplate;
        this.domain = domain;
        this.contextPath = contextPath;
    }

    /**
     * 修改用户密码，并清理对应缓存。
     *
     * @param id 用户 ID
     * @param password 已编码密码
     * @return 影响行数
     */
    public int updatePassword(int id, String password) {
        int rows = userMapper.updatePassword(id, password);
        clearCache(id);
        return rows;
    }

    /**
     * 按用户 ID 查询用户，优先走 Redis 缓存。
     *
     * @param id 用户 ID
     * @return 用户实体；不存在时返回 {@code null}
     */
    public User findUserById(int id) {
        User user = getCache(id);
        return user != null ? user : initCache(id);
    }

    /**
     * 处理论坛原生注册流程。
     *
     * <p>注册成功后会发送激活邮件，保持论坛既有账号生命周期不变。</p>
     *
     * @param user 注册用户
     * @return 校验失败信息；成功时返回空 Map
     */
    public Map<String, Object> register(User user) {
        Map<String, Object> result = new HashMap<>();
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            result.put("usernameMsg", "账号不能为空！");
            return result;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            result.put("passwordMsg", "密码不能为空！");
            return result;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            result.put("emailMsg", "邮箱不能为空！");
            return result;
        }

        User existingUser = userMapper.selectByName(user.getUsername());
        if (existingUser != null) {
            result.put("usernameMsg", "账号已存在！");
            return result;
        }

        User emailOwner = userMapper.selectByEmail(user.getEmail());
        if (emailOwner != null) {
            result.put("emailMsg", "邮箱已被注册！");
            return result;
        }

        user.setSalt("");
        user.setPassword(encodePassword(user.getPassword()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID().substring(0, 6));
        user.setHeaderUrl(generateRandomHeaderUrl());
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        sendActivationMail(user);
        return result;
    }

    /**
     * 处理来自商城等外部服务的注册请求。
     *
     * <p>论坛仍然是统一账号主数据源，其他服务应通过该方法申请账号，避免出现多套用户 ID。</p>
     *
     * @param request 跨服务注册请求
     * @return 新建的论坛用户
     */
    public User registerExternal(AuthRegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("invalid_request");
        }
        String username = StringUtils.trimToNull(request.getUsername());
        String password = StringUtils.trimToNull(request.getPassword());
        if (username == null || password == null) {
            throw new IllegalArgumentException("invalid_request");
        }

        User existingUser = userMapper.selectByName(username);
        if (existingUser != null) {
            throw new IllegalArgumentException("username_exists");
        }

        String email = StringUtils.trimToNull(request.getEmail());
        if (email == null) {
            email = username + "@" + EXTERNAL_EMAIL_DOMAIN;
        }

        User emailOwner = userMapper.selectByEmail(email);
        if (emailOwner != null) {
            email = username + "+" + CommunityUtil.generateUUID().substring(0, 6) + "@" + EXTERNAL_EMAIL_DOMAIN;
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(encodePassword(password));
        user.setSalt("");
        user.setEmail(email);
        user.setType(0);
        user.setHeaderUrl(generateRandomHeaderUrl());
        user.setCreateTime(new Date());
        if (request.isAutoActivate()) {
            user.setStatus(1);
            user.setActivationCode("");
        } else {
            user.setStatus(0);
            user.setActivationCode(CommunityUtil.generateUUID().substring(0, 6));
        }
        userMapper.insertUser(user);
        return user;
    }

    /**
     * 激活论坛账号。
     *
     * @param userId 用户 ID
     * @param code 激活码
     * @return 激活结果
     */
    public int activation(int userId, String code) {
        User user = findUserById(userId);
        if (user == null) {
            return ACTIVATION_FAILURE;
        }
        if (user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        }
        if (StringUtils.equals(user.getActivationCode(), code)) {
            userMapper.updateStatus(userId, 1);
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        }
        return ACTIVATION_FAILURE;
    }

    /**
     * 处理论坛原生登录流程，并签发新的登录凭证。
     *
     * <p>登录成功后会主动废弃该用户之前仍有效的 ticket，避免同一账号长期保留多个活跃会话。</p>
     *
     * @param username 用户名
     * @param password 明文密码
     * @param expiredSeconds ticket 有效期（秒）
     * @return 登录结果
     */
    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> result = new HashMap<>();
        if (StringUtils.isBlank(username)) {
            result.put("usernameMsg", "账号不能为空！");
            return result;
        }
        if (StringUtils.isBlank(password)) {
            result.put("passwordMsg", "密码不能为空！");
            return result;
        }

        User user = userMapper.selectByName(username);
        if (user == null) {
            result.put("usernameMsg", "账号不存在！");
            return result;
        }
        if (user.getStatus() == 0) {
            result.put("usernameMsg", "账号未激活！");
            return result;
        }
        if (!matchesPassword(user, password)) {
            result.put("passwordMsg", "密码错误！");
            return result;
        }

        upgradePasswordIfNeeded(user, password);
        invalidateActiveLoginTicket(user.getId());

        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + 1000L * expiredSeconds));
        cacheLoginTicket(loginTicket, expiredSeconds);

        result.put("ticket", loginTicket.getTicket());
        return result;
    }

    /**
     * 注销登录凭证。
     *
     * @param ticket 登录凭证值
     */
    public void logout(String ticket) {
        if (StringUtils.isBlank(ticket)) {
            return;
        }
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        Object cachedValue = redisTemplate.opsForValue().get(ticketKey);
        if (!(cachedValue instanceof LoginTicket)) {
            return;
        }

        LoginTicket loginTicket = (LoginTicket) cachedValue;
        loginTicket.setStatus(1);
        long ttlMillis = loginTicket.getExpired().getTime() - System.currentTimeMillis();
        if (ttlMillis > 0) {
            redisTemplate.opsForValue().set(ticketKey, loginTicket, ttlMillis, TimeUnit.MILLISECONDS);
        } else {
            redisTemplate.delete(ticketKey);
        }
        deleteUserTicketMappingIfMatch(loginTicket.getUserId(), ticket);
    }

    /**
     * 查询登录凭证。
     *
     * @param ticket 登录凭证值
     * @return 登录凭证；不存在时返回 {@code null}
     */
    public LoginTicket findLoginTicket(String ticket) {
        if (StringUtils.isBlank(ticket)) {
            return null;
        }
        Object cachedValue = redisTemplate.opsForValue().get(RedisKeyUtil.getTicketKey(ticket));
        return cachedValue instanceof LoginTicket ? (LoginTicket) cachedValue : null;
    }

    /**
     * 更新用户头像地址，并清理缓存。
     *
     * @param userId 用户 ID
     * @param headerUrl 头像地址
     * @return 影响行数
     */
    public int updateHeader(int userId, String headerUrl) {
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }

    /**
     * 按用户名查询用户。
     *
     * @param username 用户名
     * @return 用户实体；不存在时返回 {@code null}
     */
    public User findUserByName(String username) {
        return userMapper.selectByName(username);
    }

    /**
     * 批量查询用户，并对未命中缓存的数据进行回填。
     *
     * @param ids 用户 ID 集合
     * @return 用户 Map，Key 为用户 ID
     */
    public Map<Integer, User> findUsersByIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Integer> distinctIds = new ArrayList<>();
        for (Integer id : ids) {
            if (id != null && !distinctIds.contains(id)) {
                distinctIds.add(id);
            }
        }
        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> keys = new ArrayList<>();
        for (Integer id : distinctIds) {
            keys.add(RedisKeyUtil.getUserKey(id));
        }

        List<Object> cachedValues = redisTemplate.opsForValue().multiGet(keys);
        Map<Integer, User> result = new HashMap<>();
        List<Integer> missedIds = new ArrayList<>();
        for (int i = 0; i < distinctIds.size(); i++) {
            Object cachedValue = cachedValues == null ? null : cachedValues.get(i);
            if (cachedValue instanceof User) {
                result.put(distinctIds.get(i), (User) cachedValue);
            } else {
                missedIds.add(distinctIds.get(i));
            }
        }

        if (!missedIds.isEmpty()) {
            List<User> dbUsers = userMapper.selectByIds(missedIds);
            if (dbUsers != null) {
                for (User dbUser : dbUsers) {
                    result.put(dbUser.getId(), dbUser);
                    redisTemplate.opsForValue().set(
                            RedisKeyUtil.getUserKey(dbUser.getId()),
                            dbUser,
                            USER_CACHE_TTL_SECONDS,
                            TimeUnit.SECONDS
                    );
                }
            }
        }
        return result;
    }

    /**
     * 直接从 Redis 读取用户缓存。
     *
     * @param userId 用户 ID
     * @return 用户缓存；不存在时返回 {@code null}
     */
    public User getCache(int userId) {
        Object cachedValue = redisTemplate.opsForValue().get(RedisKeyUtil.getUserKey(userId));
        return cachedValue instanceof User ? (User) cachedValue : null;
    }

    /**
     * 从数据库加载用户并初始化缓存。
     *
     * @param userId 用户 ID
     * @return 用户实体；不存在时返回 {@code null}
     */
    public User initCache(int userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        redisTemplate.opsForValue().set(
                RedisKeyUtil.getUserKey(userId),
                user,
                USER_CACHE_TTL_SECONDS,
                TimeUnit.SECONDS
        );
        return user;
    }

    /**
     * 清理用户缓存。
     *
     * @param userId 用户 ID
     */
    public void clearCache(int userId) {
        redisTemplate.delete(RedisKeyUtil.getUserKey(userId));
    }

    /**
     * 使用 BCrypt 对明文密码进行编码。
     *
     * @param rawPassword 明文密码
     * @return BCrypt 哈希值
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 校验明文密码与存储密码是否匹配。
     *
     * <p>兼容历史 MD5+salt 密码和新的 BCrypt 密码。</p>
     *
     * @param user 用户实体
     * @param rawPassword 明文密码
     * @return 是否匹配
     */
    public boolean matchesPassword(User user, String rawPassword) {
        if (user == null || rawPassword == null) {
            return false;
        }
        String storedPassword = user.getPassword();
        if (isBcryptHash(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        String salt = user.getSalt() == null ? "" : user.getSalt();
        return storedPassword != null && storedPassword.equals(CommunityUtil.md5(rawPassword + salt));
    }

    /**
     * 在用户成功登录后，把历史密码哈希升级为 BCrypt。
     *
     * @param user 已认证用户
     * @param rawPassword 本次登录输入的明文密码
     */
    public void upgradePasswordIfNeeded(User user, String rawPassword) {
        if (user == null || rawPassword == null) {
            return;
        }
        if (!isBcryptHash(user.getPassword())) {
            String encodedPassword = encodePassword(rawPassword);
            updatePassword(user.getId(), encodedPassword);
            user.setPassword(encodedPassword);
        }
    }

    /**
     * 返回指定用户的 Spring Security 权限集合。
     *
     * @param userId 用户 ID
     * @return 权限列表
     */
    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = findUserById(userId);
        if (user == null) {
            return Collections.emptyList();
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(() -> {
            switch (user.getType()) {
                case 1:
                    return AUTHORITY_ADMIN;
                case 2:
                    return AUTHORITY_MODERATOR;
                default:
                    return AUTHORITY_USER;
            }
        });
        return authorities;
    }

    /**
     * 判断密码是否已经是 BCrypt 格式。
     *
     * @param hash 密码哈希
     * @return 是否为 BCrypt 哈希
     */
    private boolean isBcryptHash(String hash) {
        if (hash == null) {
            return false;
        }
        return hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$");
    }

    /**
     * 生成随机头像地址。
     *
     * @return 头像 URL
     */
    private String generateRandomHeaderUrl() {
        return String.format("https://images.nowcoder.com/head/%dt.png", ThreadLocalRandom.current().nextInt(1000));
    }

    /**
     * 发送激活邮件。
     *
     * @param user 新注册用户
     */
    private void sendActivationMail(User user) {
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation.html", context);
        mailClient.sendMail(user.getEmail(), "Activate account", content);
    }

    /**
     * 将新的 ticket 和“用户当前活跃 ticket”映射同时写入 Redis。
     *
     * @param loginTicket 登录凭证对象
     * @param expiredSeconds 凭证有效期（秒）
     */
    private void cacheLoginTicket(LoginTicket loginTicket, int expiredSeconds) {
        redisTemplate.opsForValue().set(
                RedisKeyUtil.getTicketKey(loginTicket.getTicket()),
                loginTicket,
                expiredSeconds,
                TimeUnit.SECONDS
        );
        redisTemplate.opsForValue().set(
                RedisKeyUtil.getUserTicketKey(loginTicket.getUserId()),
                loginTicket.getTicket(),
                expiredSeconds,
                TimeUnit.SECONDS
        );
    }

    /**
     * 废弃用户当前仍然有效的旧 ticket。
     *
     * @param userId 用户 ID
     */
    private void invalidateActiveLoginTicket(int userId) {
        String userTicketKey = RedisKeyUtil.getUserTicketKey(userId);
        Object ticketValue = redisTemplate.opsForValue().get(userTicketKey);
        if (!(ticketValue instanceof String)) {
            redisTemplate.delete(userTicketKey);
            return;
        }

        String activeTicket = (String) ticketValue;
        String ticketKey = RedisKeyUtil.getTicketKey(activeTicket);
        Object loginTicketValue = redisTemplate.opsForValue().get(ticketKey);
        if (loginTicketValue instanceof LoginTicket) {
            LoginTicket loginTicket = (LoginTicket) loginTicketValue;
            loginTicket.setStatus(1);
            long ttlMillis = loginTicket.getExpired().getTime() - System.currentTimeMillis();
            if (ttlMillis > 0) {
                redisTemplate.opsForValue().set(ticketKey, loginTicket, ttlMillis, TimeUnit.MILLISECONDS);
            } else {
                redisTemplate.delete(ticketKey);
            }
        } else {
            redisTemplate.delete(ticketKey);
        }
        redisTemplate.delete(userTicketKey);
    }

    /**
     * 仅当用户映射仍然指向当前 ticket 时，才删除用户的活跃 ticket 映射。
     *
     * @param userId 用户 ID
     * @param ticket 登录凭证
     */
    private void deleteUserTicketMappingIfMatch(int userId, String ticket) {
        String userTicketKey = RedisKeyUtil.getUserTicketKey(userId);
        Object mappedTicket = redisTemplate.opsForValue().get(userTicketKey);
        if (ticket.equals(mappedTicket)) {
            redisTemplate.delete(userTicketKey);
        }
    }
}
