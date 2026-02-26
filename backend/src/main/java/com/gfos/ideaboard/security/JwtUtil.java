package com.gfos.ideaboard.security;

import com.gfos.ideaboard.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.enterprise.context.ApplicationScoped;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class JwtUtil {

    // In der Produktion soll dies aus einer Umgebungsvariablen oder sicheren Konfiguration geladen werden
    private static final String DEFAULT_SECRET = "gfos-ideaboard-jwt-secret-key-2026-must-be-at-least-256-bits-long-for-hs256";
    private static final long ACCESS_TOKEN_EXPIRATION = 24 * 60 * 60 * 1000; // 24 Stunden
    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000; // 7 Tage

    private final SecretKey key;

    public JwtUtil() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.trim().isEmpty()) {
            secret = System.getProperty("JWT_SECRET");
        }
        
        if (secret == null || secret.trim().isEmpty()) {
            // Wenn kein Secret konfiguriert ist, generiere eines beim Start.
            // Dies führt dazu, dass alle Sessions nach einem Neustart ungültig werden (Sicherheitsfeature).
            this.key = Jwts.SIG.HS256.key().build();
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(key)
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isTokenValid(String token) {
        return validateToken(token) != null;
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims == null) {
            return null;
        }
        return claims.get("userId", Long.class);
    }

    public String getUsernameFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims == null) {
            return null;
        }
        return claims.getSubject();
    }

    public String getRoleFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims == null) {
            return null;
        }
        return claims.get("role", String.class);
    }

    public boolean isRefreshToken(String token) {
        Claims claims = validateToken(token);
        if (claims == null) {
            return false;
        }
        return "refresh".equals(claims.get("type", String.class));
    }

    public long getAccessTokenExpiration() {
        return ACCESS_TOKEN_EXPIRATION;
    }
}
