package com.officeflow.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

public final class JwtUtil {
    private JwtUtil() {
    }

    public static String generateToken(String subject, Map<String, Object> claims, String secret, long expireSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(secretKey(secret))
                .compact();
    }

    public static Claims parseClaims(String token, String secret) {
        return Jwts.parser()
                .verifyWith(secretKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static boolean isValid(String token, String secret) {
        try {
            parseClaims(token, secret);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static SecretKey secretKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}

