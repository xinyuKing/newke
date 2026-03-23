package com.shixi.ecommerce.internal;

import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class InternalAuthRestTemplateInterceptor implements ClientHttpRequestInterceptor {
    private final InternalAuthSigner signer;

    public InternalAuthRestTemplateInterceptor(InternalAuthSigner signer) {
        this.signer = signer;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        signer.applySignature(request.getHeaders(), request.getMethod(), request.getURI(), body);
        return execution.execute(request, body);
    }
}
