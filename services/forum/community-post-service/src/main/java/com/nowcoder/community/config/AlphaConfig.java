package com.nowcoder.community.config;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.text.SimpleDateFormat;

/**
 * Alpha 示例配置。
 */
@Configuration
public class AlphaConfig {

    /**
     * 返回原型作用域的 {@link SimpleDateFormat}。
     *
     * <p>该类型本身不是线程安全的，因此不能作为共享单例 Bean 暴露给多线程场景。</p>
     *
     * @return 日期格式化器
     */
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SimpleDateFormat simpleDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }
}
