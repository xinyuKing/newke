锘縫ackage com.nowcoder.community.util;

/**
 * Redis Key ?????
 *
 * <p>?????????Redis ??????????????????????????
 * ???? Key ??????????????????</p>
 */
public final class RedisKeyUtil {

    private static final String SPLIT = ":";
    private static final String PREFIX_ENTITY_LIKE = "like:entity";
    private static final String PREFIX_USER_LIKE = "like:user";
    private static final String PREFIX_FOLLOWEE = "followee";
    private static final String PREFIX_FOLLOWER = "follower";
    private static final String PREFIX_KAPTCHA = "kaptcha";
    private static final String PREFIX_TICKET = "ticket";
    private static final String PREFIX_USER_TICKET = "ticket:user";
    private static final String PREFIX_USER = "user";
    private static final String PREFIX_UV = "uv";
    private static final String PREFIX_DAU = "dau";
    private static final String PREFIX_POST = "post";

    private RedisKeyUtil() {
    }

    /**
     * ?????????
     *
     * <p>Key ???{@code like:entity:1:100}?Value ????? ID ???</p>
     *
     * @param entityType ????
     * @param id ?? ID
     * @return Redis Key
     */
    public static String getEntityLikeKey(int entityType, int id) {
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + id;
    }

    /**
     * ???????????
     *
     * @param userId ?? ID
     * @return Redis Key
     */
    public static String getUserLikeKey(int userId) {
        return PREFIX_USER_LIKE + SPLIT + userId;
    }

    /**
     * ????????????
     *
     * <p>Key ???{@code followee:userId:entityType}?</p>
     *
     * @param userId ?? ID
     * @param entityType ????
     * @return Redis Key
     */
    public static String getFolloweeKey(int userId, int entityType) {
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
    }

    /**
     * ????????????
     *
     * <p>Key ???{@code follower:entityType:entityId}?</p>
     *
     * @param entityType ????
     * @param entityId ?? ID
     * @return Redis Key
     */
    public static String getFollowerKey(int entityType, int entityId) {
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }

    /**
     * ????? Key?
     *
     * @param owner ???????
     * @return Redis Key
     */
    public static String getKaptchaKey(String owner) {
        return PREFIX_KAPTCHA + SPLIT + owner;
    }

    /**
     * ???? Key?
     *
     * @param ticket ?????
     * @return Redis Key
     */
    public static String getTicketKey(String ticket) {
        return PREFIX_TICKET + SPLIT + ticket;
    }

    /**
     * ???????????? Key?
     *
     * <p>? Key ???????????????????????? ticket ????????</p>
     *
     * @param userId ?? ID
     * @return Redis Key
     */
    public static String getUserTicketKey(int userId) {
        return PREFIX_USER_TICKET + SPLIT + userId;
    }

    /**
     * ???? Key?
     *
     * @param userId ?? ID
     * @return Redis Key
     */
    public static String getUserKey(int userId) {
        return PREFIX_USER + SPLIT + userId;
    }

    /**
     * ?? UV ?? Key?
     *
     * @param date ??
     * @return Redis Key
     */
    public static String getUVKey(String date) {
        return PREFIX_UV + SPLIT + date;
    }

    /**
     * ?? UV ?? Key?
     *
     * @param startDate ????
     * @param endDate ????
     * @return Redis Key
     */
    public static String getUVKey(String startDate, String endDate) {
        return PREFIX_UV + SPLIT + startDate + SPLIT + endDate;
    }

    /**
     * ?? DAU ?? Key?
     *
     * @param date ??
     * @return Redis Key
     */
    public static String getDAUKey(String date) {
        return PREFIX_DAU + SPLIT + date;
    }

    /**
     * ?? DAU ?? Key?
     *
     * @param startDate ????
     * @param endDate ????
     * @return Redis Key
     */
    public static String getDAUKey(String startDate, String endDate) {
        return PREFIX_DAU + SPLIT + startDate + SPLIT + endDate;
    }

    /**
     * ????????? Key?
     *
     * @return Redis Key
     */
    public static String getPostScoreKey() {
        return PREFIX_POST + SPLIT + "score";
    }
}
