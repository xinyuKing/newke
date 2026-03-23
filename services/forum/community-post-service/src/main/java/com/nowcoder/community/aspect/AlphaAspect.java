package com.nowcoder.community.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP 示例切面。
 *
 * <p>当前默认不启用，只保留为示例代码。若需启用，请打开 {@link Component} 和 {@link Aspect} 注解。</p>
 */
// @Component
// @Aspect
public class AlphaAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlphaAspect.class);

    @Pointcut("execution(* com.nowcoder.community.service.*.*(..))")
    public void pointcut() {}

    @Before("pointcut()")
    public void before() {
        LOGGER.debug("alpha aspect before");
    }

    @After("pointcut()")
    public void after() {
        LOGGER.debug("alpha aspect after");
    }

    @AfterReturning("pointcut()")
    public void afterReturning() {
        LOGGER.debug("alpha aspect afterReturning");
    }

    @AfterThrowing("pointcut()")
    public void afterThrowing() {
        LOGGER.debug("alpha aspect afterThrowing");
    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        LOGGER.debug("alpha aspect around before");
        Object result = joinPoint.proceed();
        LOGGER.debug("alpha aspect around after");
        return result;
    }
}
