package com.officeflow.gateway.filter;

import com.officeflow.common.constant.CommonConstants;
import com.officeflow.common.security.JwtUtil;
import com.officeflow.common.security.SecurityConstants;
import com.officeflow.gateway.cache.CachedPermissionChecker;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    private final CachedPermissionChecker permissionChecker;

    private final List<String> whiteList = List.of(
            "/api/user/login",
            "/api/user/auth/login",
            "/api/user/health",
            "/api/attendance/health",
            "/api/flow/health",
            "/api/notice/health",
            "/api/report/health",
            "/api/ai/health",
            "/actuator"
    );

    private final List<String> permissionWhiteList = List.of(
            "/api/user/logout",
            "/api/user/profile",
            "/api/user/password",
            "/api/user/menus/current",
            "/api/flow/attachments",
            "/api/notice/attachments"
    );

    @Value("${officeflow.jwt.secret:officeflow-secret-key-must-be-at-least-32-bytes}")
    private String jwtSecret;

    @Value("${officeflow.permission.enabled:true}")
    private boolean permissionEnabled;

    public JwtAuthenticationFilter(CachedPermissionChecker permissionChecker) {
        this.permissionChecker = permissionChecker;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();
        if ((method != null && method.matches("OPTIONS")) || isWhitePath(path)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(CommonConstants.TOKEN_PREFIX)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authorization.substring(CommonConstants.TOKEN_PREFIX.length());
        Claims claims;
        try {
            claims = JwtUtil.parseClaims(token, jwtSecret);
        } catch (RuntimeException ex) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        Long userId = toLong(claims.get(SecurityConstants.CLAIM_USER_ID));
        if (userId == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        ServerWebExchange authenticatedExchange = exchange.mutate()
                .request(builder -> builder
                        .header(CommonConstants.LOGIN_USER_ID_HEADER, String.valueOf(userId))
                        .header(CommonConstants.LOGIN_USERNAME_HEADER, String.valueOf(claims.get(SecurityConstants.CLAIM_USERNAME)))
                        .header(CommonConstants.LOGIN_ROLES_HEADER, String.valueOf(claims.get(SecurityConstants.CLAIM_ROLES)))
                        .header(CommonConstants.LOGIN_DEPT_ID_HEADER, String.valueOf(claims.get(SecurityConstants.CLAIM_DEPT_ID))))
                .build();
        if (!permissionEnabled || isPermissionWhitePath(path)) {
            return chain.filter(authenticatedExchange);
        }

        String methodName = method == null ? "" : method.name();
        return permissionChecker.check(userId, methodName, path)
                .flatMap(allowed -> {
                    if (allowed) {
                        return chain.filter(authenticatedExchange);
                    }
                    authenticatedExchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return authenticatedExchange.getResponse().setComplete();
                })
                .onErrorResume(ex -> {
                    authenticatedExchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                    return authenticatedExchange.getResponse().setComplete();
                });
    }

    private boolean isWhitePath(String path) {
        return whiteList.stream().anyMatch(path::startsWith);
    }

    private boolean isPermissionWhitePath(String path) {
        return permissionWhiteList.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
