package com.nowcoder.community.internal;

import feign.RequestInterceptor;
import feign.RequestTemplate;

public class CommunityInternalAuthFeignInterceptor implements RequestInterceptor {
    private final CommunityInternalAuthSigner signer;

    public CommunityInternalAuthFeignInterceptor(CommunityInternalAuthSigner signer) {
        this.signer = signer;
    }

    @Override
    public void apply(RequestTemplate template) {
        signer.applySignature(template);
    }
}
