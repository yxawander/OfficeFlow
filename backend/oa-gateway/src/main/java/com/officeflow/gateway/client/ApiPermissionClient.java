package com.officeflow.gateway.client;

import com.officeflow.common.constant.CommonConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class ApiPermissionClient {
    private final WebClient webClient;

    @Value("${officeflow.internal.secret:officeflow-internal-secret}")
    private String internalSecret;

    public ApiPermissionClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder.baseUrl("lb://user-service").build();
    }

    public Mono<PermissionDecision> check(Long userId, String method, String path) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/user/internal/permissions/check")
                        .queryParam("userId", userId)
                        .queryParam("method", method)
                        .queryParam("path", path)
                        .build())
                .header(CommonConstants.INTERNAL_TOKEN_HEADER, internalSecret)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .map(this::toDecision);
    }

    @SuppressWarnings("unchecked")
    private PermissionDecision toDecision(Map<String, Object> response) {
        Object code = response.get("code");
        if (!(code instanceof Number number) || number.intValue() != 200) {
            return new PermissionDecision(false, true, String.valueOf(response.getOrDefault("message", "权限校验失败")));
        }
        Object data = response.get("data");
        if (!(data instanceof Map<?, ?> rawData)) {
            return new PermissionDecision(false, true, "权限校验返回异常");
        }
        Map<String, Object> result = (Map<String, Object>) rawData;
        boolean allowed = Boolean.TRUE.equals(result.get("allowed"));
        boolean configured = Boolean.TRUE.equals(result.get("configured"));
        String message = String.valueOf(result.getOrDefault("message", allowed ? "接口权限校验通过" : "无接口访问权限"));
        return new PermissionDecision(allowed, configured, message);
    }

    public record PermissionDecision(boolean allowed, boolean configured, String message) {
    }
}
