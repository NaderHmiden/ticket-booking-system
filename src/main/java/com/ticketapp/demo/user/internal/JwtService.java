package com.ticketapp.demo.user.internal;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtService(
            @Value("${app.security.jwt-secret:ticket-booking-dev-secret-change-me-32b}") String secret,
            @Value("${app.security.jwt-ttl-seconds:86400}") long ttlSeconds) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            // HS256 requires >= 256-bit key
            bytes = java.util.Arrays.copyOf(bytes, 32);
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.ttlSeconds = ttlSeconds;
    }

    public String issueToken(UUID userId, String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
