package com.nowcoder.community.actuator;

import com.nowcoder.community.util.CommunityUtil;
import java.sql.Connection;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

/**
 * 数据库健康检查端点。
 */
@Component
@Endpoint(id = "database")
public class DatabaseEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseEndpoint.class);

    private final DataSource dataSource;

    public DatabaseEndpoint(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 检查数据库连接是否可用。
     *
     * @return JSON 字符串结果
     */
    @ReadOperation
    public String checkConnection() {
        try (Connection ignored = dataSource.getConnection()) {
            return CommunityUtil.getJSONString(0, "获取连接成功");
        } catch (Exception ex) {
            LOGGER.error("数据库连接检查失败", ex);
            return CommunityUtil.getJSONString(1, "获取连接失败: " + ex.getMessage());
        }
    }
}
