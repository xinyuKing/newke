package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.dto.AuthRegisterRequest;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Core user domain service of the forum application.
 *
 * <p>This service handles forum registration, activation, login ticket management, cache
 * population and permission lookup. After the forum and mall systems were merged, it also became
 * the source of truth for cross-service account registration and password verification.</p>
 */
@Service
public class UserService implements CommunityConstant {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    //优化后存在redis中
/*    @Autowired
    private LoginTicketMapper loginTicketMapper;*/

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * BCrypt encoder used by newly created passwords and by legacy password migration.
     */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Placeholder mail domain used when an external caller does not provide a real email address.
     */
    private static final String EXTERNAL_EMAIL_DOMAIN = "ecommerce.local";

    //修改密码
    /**
     * Updates the stored password and clears the cached user snapshot.
     *
     * @param id user ID
     * @param password encoded password to persist
     * @return affected row count
     */
    public int updatePassword(int id,String password){
        int rows = userMapper.updatePassword(id, password);
        clearCache(id);
        return rows;
    }

    /**
     * Queries a user by ID using a cache-first strategy.
     *
     * @param id user ID
     * @return user entity, or {@code null} when no record exists
     */
    public User findUserById(int id){
        //优化后先从redis中查询
//        return userMapper.selectById(id);
        User user = getCache(id);
        if(user!=null){
            return user;
        }else {
            return initCache(id);
        }
    }

    /*注册*/
    /**
     * Handles the original forum registration flow.
     *
     * <p>This path validates the basic fields, writes the user record, and sends the activation
     * email used by the forum web application.</p>
     *
     * @param user forum registration entity
     * @return validation error map, empty when registration succeeds
     */
    public Map<String,Object> register(User user){
        Map<String,Object> map=new HashMap<>();

        // 空值处理
        if(user==null){
            throw new IllegalArgumentException("参数不能为空！");
        }
        if (StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg","账号不能为空！");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg","密码不能为空！");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())){
            map.put("emailMsg","邮箱不能为空！");
            return map;
        }

        //验证账号
        User u = userMapper.selectByName(user.getUsername());
        if(u!=null){
            map.put("usernameMsg","账号已存在！");
            return map;
        }

        //验证邮箱
        //判断条件：可以通过邮箱查到用户
        u = userMapper.selectByEmail(user.getEmail());
        if(u!=null){
            map.put("emailMsg","邮箱已被注册！");
            return map;
        }
        //验证邮箱
        //判断条件：可以通过邮箱查到激活的用户(自己的)
//        u = userMapper.selectByEmail(user.getEmail());
//        if(u!=null){
//            if(u.getStatus()==1){
//                map.put("emailMsg","邮箱已被注册！");
//                return map;
//            }else {//再次发送邮件
//                用户激活
        //        //给用户发送激活邮件,用户通过前往邮件发送的网址进行激活
        //        Context context=new Context();
        //        context.setVariable("email",user.getEmail());
        //        //http://localhost:8080/community/activation/101/code
        //        String url=domain+contextPath+"/activation/"+user.getId()+"/"+user.getActivationCode();
        //        context.setVariable("url",url);
        //
        //        String content = templateEngine.process("/mail/activation.html", context);
        //
        //        mailClient.sendMail(user.getEmail(),"Activate account",content);
        //
        //        return map;
//            }
//        }

        // 注册用户
        // 对密码进行加密
        user.setSalt("");
        user.setPassword(encodePassword(user.getPassword()));
        //设置为普通用户
        user.setType(0);
        //用户未激活
        user.setStatus(0);
        // 随机生成注册码
        user.setActivationCode(CommunityUtil.generateUUID().substring(0,6));
        // 设置一个随机头像
        user.setHeaderUrl(String.format("https://images.nowcoder.com/head/%dt.png",new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        // 插入用户
        userMapper.insertUser(user);

        //用户激活
        //给用户发送激活邮件,用户通过前往邮件发送的网址进行激活
        Context context=new Context();
        context.setVariable("email",user.getEmail());
        //http://localhost:8080/community/activation/101/code
        String url=domain+contextPath+"/activation/"+user.getId()+"/"+user.getActivationCode();
        context.setVariable("url",url);

        String content = templateEngine.process("/mail/activation.html", context);
        mailClient.sendMail(user.getEmail(),"Activate account",content);

        return map;
    }

    /**
     * Registers a user on behalf of an external service such as the mall auth module.
     *
     * <p>The forum remains the single source of truth for account creation, so external services
     * must call this method instead of creating their own user IDs locally. When an email is not
     * supplied, a stable placeholder email is generated so the forum schema constraints remain
     * satisfied.</p>
     *
     * @param request cross-service registration request
     * @return newly created forum user
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
        User existing = userMapper.selectByName(username);
        if (existing != null) {
            throw new IllegalArgumentException("username_exists");
        }
        String email = StringUtils.trimToNull(request.getEmail());
        if (email == null) {
            // External callers may not own an email yet, but the forum schema still requires one.
            email = username + "@" + EXTERNAL_EMAIL_DOMAIN;
        }
        User emailOwner = userMapper.selectByEmail(email);
        if (emailOwner != null) {
            // Keep email uniqueness intact even when we synthesize the placeholder address.
            email = username + "+" + CommunityUtil.generateUUID().substring(0, 6) + "@" + EXTERNAL_EMAIL_DOMAIN;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(encodePassword(password));
        user.setSalt("");
        user.setEmail(email);
        user.setType(0);
        if (request.isAutoActivate()) {
            user.setStatus(1);
            user.setActivationCode("");
        } else {
            user.setStatus(0);
            user.setActivationCode(CommunityUtil.generateUUID().substring(0, 6));
        }
        user.setHeaderUrl(String.format("https://images.nowcoder.com/head/%dt.png", new java.util.Random().nextInt(1000)));
        user.setCreateTime(new java.util.Date());
        userMapper.insertUser(user);
        return user;
    }

    /*激活*/
    /**
     * Activates a forum account by activation code.
     *
     * @param userId user ID
     * @param code activation code from the email link
     * @return activation result defined in {@link CommunityConstant}
     */
    public int activation(int userId,String code){
        //这一步调用该类中优化后的方法findUserById和直接读那个更好一些???????
        User user = this.findUserById(userId);
//        User user = userMapper.selectById(userId);
        if(user.getStatus()==1){
            return ACTIVATION_REPEAT;
        }else if(user.getActivationCode().equals(code)){
            // 把用户状态改为1
            userMapper.updateStatus(userId,1);
            //清除redis中的该用户
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        }else{
            return ACTIVATION_FAILURE;
        }
    }

    /*登录*/
    /**
     * Handles the original forum login flow and creates a login ticket.
     *
     * <p>The password check supports both legacy hashes and the newer BCrypt format. Legacy
     * passwords are upgraded in place after a successful login to reduce future compatibility
     * branches.</p>
     *
     * @param username username
     * @param password clear text password
     * @param expiredSeconds login ticket validity period in seconds
     * @return login result map, including the login ticket when successful
     */
    public Map<String,Object> login(String username,String password,int expiredSeconds){
        Map<String,Object> map=new HashMap<>();

        //空值处理
        if (username==null) {
            map.put("usernameMsg","账号不能为空！");
            return map;
        }
        if (password==null) {
            map.put("passwordMsg","密码不能为空！");
            return map;
        }

        //验证账号
        User u = userMapper.selectByName(username);
        if(u==null){
            map.put("usernameMsg","账号不存在！");
            return map;
        }
        //验证状态
        if(u.getStatus()==0){
            map.put("usernameMsg","账号未激活！");
            return map;
        }
        //验证密码
        // Support both historical hashes and the new BCrypt format during the transition period.
        if(!matchesPassword(u, password)){
            map.put("passwordMsg","密码错误！");
            return map;
        }
        // ????MD5???????BCrypt
        if(!isBcryptHash(u.getPassword())){
            String encoded = encodePassword(password);
            updatePassword(u.getId(), encoded);
            u.setPassword(encoded);
        }

        //账号（已激活）密码正确
        // TODO: 2024/3/13  先查询是否有该用户没有过期的登录凭证，有的话：1.删除再重新加入（可以防止一个账号被两个人使用，别忘记清除HostHolder）。2.修改
        //生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(u.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis()+1000*expiredSeconds));
        //插入登录凭证，优化后存在redis中
//        loginTicketMapper.insertLoginTicket(loginTicket);

        String ticketKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        //redis会自动把对象序列化成json字符串
        //别忘记设置存活时间
        redisTemplate.opsForValue().set(ticketKey,loginTicket,expiredSeconds,TimeUnit.SECONDS);

        map.put("ticket",loginTicket.getTicket());

        return map;
    }

    /*退出*/
    /**
     * Invalidates a login ticket.
     *
     * @param ticket login ticket value
     */
    public void logout(String ticket){
        //已经把登录凭证存入Redis中
//        loginTicketMapper.updateStatus(ticket,1);
        //从redis中读出登录凭证，修改状态后再存入
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket=(LoginTicket)redisTemplate.opsForValue().get(ticketKey);
        if(loginTicket==null){
            return;
        }
        loginTicket.setStatus(1);//修改登录状态
        redisTemplate.opsForValue().set(ticketKey,loginTicket);
    }

    /*查询登录凭证*/
    /**
     * Queries a login ticket from Redis.
     *
     * @param ticket login ticket value
     * @return login ticket entity, or {@code null} when absent
     */
    public LoginTicket findLoginTicket(String ticket){
//        return loginTicketMapper.selectByTicket(ticket);
        //优化后把登录凭证存入了Redis中
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket)redisTemplate.opsForValue().get(ticketKey);
    }

    /*更新用户头像路径*/
    /**
     * Updates the user avatar and clears the cached snapshot.
     *
     * @param userId user ID
     * @param headerUrl avatar URL
     * @return affected row count
     */
    public int updateHeader(int userId,String headerUrl){
//        return userMapper.updateHeader(userId,headerUrl);
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }

    /*通过用户姓名寻找用户*/
    /**
     * Queries a user by username.
     *
     * @param username username
     * @return matched user, or {@code null} when absent
     */
    public User findUserByName(String username){
        return userMapper.selectByName(username);
    }

    /**
     * Batch-queries users by ID with Redis warm-up.
     *
     * <p>This method first attempts a bulk cache lookup and only queries the database for missing
     * IDs. Any records loaded from the database are written back to Redis so subsequent requests
     * can be served entirely from cache.</p>
     *
     * @param ids user IDs
     * @return map keyed by user ID
     */
    public Map<Integer, User> findUsersByIds(Collection<Integer> ids){
        if(ids==null||ids.isEmpty()){
            return Collections.emptyMap();
        }
        List<Integer> idList=new ArrayList<>();
        for(Integer id:ids){
            if(id!=null && !idList.contains(id)){
                idList.add(id);
            }
        }
        if(idList.isEmpty()){
            return Collections.emptyMap();
        }
        List<String> keys=new ArrayList<>();
        for(Integer id:idList){
            keys.add(RedisKeyUtil.getUserKey(id));
        }
        List<User> cached = redisTemplate.opsForValue().multiGet(keys);
        Map<Integer, User> result=new HashMap<>();
        List<Integer> missedIds=new ArrayList<>();
        for(int i=0;i<idList.size();i++){
            User user = cached==null?null:cached.get(i);
            if(user!=null){
                result.put(idList.get(i), user);
            }else{
                missedIds.add(idList.get(i));
            }
        }
        if(!missedIds.isEmpty()){
            // Back-fill Redis for the misses so later reads can reuse the populated cache.
            List<User> dbUsers = userMapper.selectByIds(missedIds);
            if(dbUsers!=null){
                for(User user:dbUsers){
                    result.put(user.getId(), user);
                    String userKey = RedisKeyUtil.getUserKey(user.getId());
                    redisTemplate.opsForValue().set(userKey,user,3600, TimeUnit.SECONDS);
                }
            }
        }
        return result;
    }

    //优先从缓存中取值
    /**
     * Reads a user snapshot directly from Redis.
     *
     * @param userId user ID
     * @return cached user, or {@code null} when the cache entry is absent
     */
    public User getCache(int userId){
        String userKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(userKey);
    }

    //取不到时初始化缓存数据
    /**
     * Loads a user from the database and initializes the Redis cache entry.
     *
     * @param userId user ID
     * @return user entity loaded from the database
     */
    public User initCache(int userId){
        //从数据表中读取user
        User user = userMapper.selectById(userId);
        //把user存入redis中
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(userKey,user,3600, TimeUnit.SECONDS);
        return user;
    }

    //数据变更时清除缓存数据
    /**
     * Removes a cached user snapshot after user data changes.
     *
     * @param userId user ID
     */
    public void clearCache(int userId){
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(userKey);
    }

    /**
     * Encodes a clear text password with BCrypt.
     *
     * @param rawPassword clear text password
     * @return BCrypt hash
     */
    public String encodePassword(String rawPassword){
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Verifies a password against the stored hash.
     *
     * <p>The forum still needs to support historical password records, so the method detects the
     * hash format first and then executes either BCrypt verification or the legacy MD5-plus-salt
     * check.</p>
     *
     * @param user user to verify
     * @param rawPassword clear text password
     * @return {@code true} when the password matches
     */
    public boolean matchesPassword(User user,String rawPassword){
        if(user==null||rawPassword==null){
            return false;
        }
        String stored = user.getPassword();
        if(isBcryptHash(stored)){
            return passwordEncoder.matches(rawPassword, stored);
        }
        String salt = user.getSalt()==null?"":user.getSalt();
        return stored!=null && stored.equals(CommunityUtil.md5(rawPassword+salt));
    }

    /**
     * Upgrades a legacy password hash to BCrypt after a successful authentication.
     *
     * @param user authenticated user
     * @param rawPassword clear text password that has just been verified
     */
    public void upgradePasswordIfNeeded(User user, String rawPassword) {
        if (user == null || rawPassword == null) {
            return;
        }
        if (!isBcryptHash(user.getPassword())) {
            // Upgrade lazily to avoid a disruptive one-time migration for all historical users.
            String encoded = encodePassword(rawPassword);
            updatePassword(user.getId(), encoded);
            user.setPassword(encoded);
        }
    }

    /**
     * Determines whether the stored hash already uses the BCrypt format.
     *
     * @param hash stored password hash
     * @return {@code true} when the hash prefix matches a supported BCrypt variant
     */
    private boolean isBcryptHash(String hash){
        if(hash==null){
            return false;
        }
        return hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$");
    }

    // 在认证过程中提供用户的权限信息，以便Spring Security可以根据用户的权限决定哪些资源和操作是允许的
    /**
     * Returns the Spring Security authorities of the specified user.
     *
     * @param userId user ID
     * @return granted authority collection used by Spring Security
     */
    public Collection<? extends GrantedAuthority> getAuthorities(int userId){
        User user = this.findUserById(userId);

        List<GrantedAuthority> list=new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()){
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }

            }
        });

        return list;
    }
}
