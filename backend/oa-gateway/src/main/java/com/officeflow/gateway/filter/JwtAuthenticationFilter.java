package com.officeflow.gateway.filter;

import com.officeflow.common.constant.CommonConstants;
import com.officeflow.common.security.JwtUtil;
import com.officeflow.common.security.SecurityConstants;
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
    private final List<String> whiteList = List.of(
            "/api/user/auth/login",
            "/api/user/health",
            "/api/attendance/health",
            "/api/flow/health",
            "/api/notice/health",
            "/api/report/health",
            "/actuator"
    );

    @Value("${officeflow.jwt.secret:officeflow-secret-key-must-be-at-least-32-bytes}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod()) || isWhitePath(path)) {
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

        ServerWebExchange authenticatedExchange = exchange.mutate()
                .request(builder -> builder
                        .header(CommonConstants.LOGIN_USER_ID_HEADER, String.valueOf(claims.get(SecurityConstants.CLAIM_USER_ID)))
                        .header(CommonConstants.LOGIN_USERNAME_HEADER, String.valueOf(claims.get(SecurityConstants.CLAIM_USERNAME))))
                .build();
        return chain.filter(authenticatedExchange);
    }

    private boolean isWhitePath(String path) {
        return whiteList.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
