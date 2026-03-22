package com.shixi.ecommerce.integration;

import com.shixi.ecommerce.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CommunityUserClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CommunityUserClient(RestTemplate restTemplate,
                               @Value("${community.user-service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public CommunityAuthUser login(String username, String password) {
        CommunityAuthRequest request = new CommunityAuthRequest(username, password, null, true);
        return post("/api/auth/login", request);
    }

    public CommunityAuthUser register(String username, String password, String email) {
        CommunityAuthRequest request = new CommunityAuthRequest(username, password, email, true);
        return post("/api/auth/register", request);
    }

    private CommunityAuthUser post(String path, CommunityAuthRequest request) {
        String url = baseUrl + path;
        ResponseEntity<CommunityApiResponse<CommunityAuthUser>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<CommunityApiResponse<CommunityAuthUser>>() {});
        CommunityApiResponse<CommunityAuthUser> body = response.getBody();
        if (body == null) {
            throw new BusinessException("Community auth response empty");
        }
        if (body.getCode() != 0) {
            throw new BusinessException(body.getMsg() == null ? "Community auth failed" : body.getMsg());
        }
        return body.getData();
    }
}
