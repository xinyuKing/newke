package com.shixi.ecommerce.integration;

import com.shixi.ecommerce.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client used by the mall auth service to talk to the forum user center.
 *
 * <p>This client hides the API contract details of the forum service and converts transport-level
 * responses into domain-oriented exceptions and objects used by the mall.</p>
 */
@Component
public class CommunityUserClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CommunityUserClient(RestTemplate restTemplate,
                               @Value("${community.user-service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Delegates login verification to the forum user service.
     *
     * @param username username to authenticate
     * @param password clear text password submitted by the client
     * @return normalized forum user payload
     */
    public CommunityAuthUser login(String username, String password) {
        CommunityAuthRequest request = new CommunityAuthRequest(username, password, null, true);
        return post("/api/auth/login", request);
    }

    /**
     * Delegates account creation to the forum user service.
     *
     * @param username username to create
     * @param password clear text password submitted by the client
     * @param email optional email, can be synthesized by the forum if absent
     * @return normalized forum user payload
     */
    public CommunityAuthUser register(String username, String password, String email) {
        CommunityAuthRequest request = new CommunityAuthRequest(username, password, email, true);
        return post("/api/auth/register", request);
    }

    /**
     * Sends a POST request to the forum user service and unwraps the common response envelope.
     *
     * @param path relative forum API path
     * @param request forum authentication payload
     * @return forum user payload
     */
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
        // Promote remote business failures to local exceptions so the controller layer can keep
        // a single error handling strategy.
        if (body.getCode() != 0) {
            throw new BusinessException(body.getMsg() == null ? "Community auth failed" : body.getMsg());
        }
        return body.getData();
    }
}
