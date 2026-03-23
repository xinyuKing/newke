package com.nowcoder.community.service.moderation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

class ModerationServiceTest {

    @Test
    void rejectsAdvertisingContent() {
        ModerationService service = createService();

        ModerationResult result = service.reviewPost("标题", "加微信领取福利", null);

        assertFalse(result.isPass());
        assertTrue(result.getTags().contains("广告引流"));
    }

    @Test
    void passesNormalContent() {
        ModerationService service = createService();

        ModerationResult result = service.reviewPost("欢迎", "这是一次正常的社区交流。", null);

        assertTrue(result.isPass());
    }

    private ModerationService createService() {
        ObjectProvider<LlmModerationClient> provider =
                new StaticListableBeanFactory().getBeanProvider(LlmModerationClient.class);
        ModerationService service = new ModerationService(true, 3, "rules", false, provider);
        service.init();
        return service;
    }
}
