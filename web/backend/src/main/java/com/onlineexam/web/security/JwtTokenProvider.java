package com.onlineexam.web.security;

import com.onlineexam.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for generating, signing, and validating JSON Web Tokens (JWT).
 */
@Component
public class JwtTokenProvider {

    private static final Logger LOGGER = Logger.getLogger(JwtTokenProvider.class.getName());

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        if (secret != null && !secret.trim().isEmpty()) {
            byte[] bytes = secret.trim().getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                // Pad to 32 bytes using SHA-256 for backward compatibility with short development secrets
                try {
                    java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                    bytes = md.digest(bytes);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to initialize secret key", e);
                }
            }
            this.key = Keys.hmacShaKeyFor(bytes);
        } else {
            LOGGER.warning("JWT_SECRET environment variable is not configured. An ephemeral HMAC-SHA256 key was generated for this session.");
            this.key = Jwts.SIG.HS256.key().build();
        }
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .claim("fullName", user.getFullName())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Number id = (Number) getClaims(token).get("userId");
        return id != null ? id.longValue() : null;
    }

    public String getRoleFromToken(String token) {
        return (String) getClaims(token).get("role");
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid JWT Token: " + e.getMessage());
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
