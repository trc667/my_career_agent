package com.example.aimaster.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：生成与解析 Token。
 */
@Component
public class JwtUtil {

    @Value("${app.jwt.secret:ai-love-master-jwt-secret-key-at-least-32-chars}")
    private String secret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long expirationMs; // 默认 24 小时

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username) {
        return generateToken(username, "USER");
    }

    /** 生成 Token，附带角色 claim（USER / ADMIN） */
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role != null ? role : "USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        Object role = parseClaims(token).get("role");
        return role != null ? role.toString() : "USER";
    }

    public boolean validateToken(String token, String username) {
        try {
            String sub = extractUsername(token);
            return sub != null && sub.equals(username);
        } catch (ExpiredJwtException e) {
            return false;
        }
    }

    public boolean isValidToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException | io.jsonwebtoken.security.SignatureException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
