package com.SplitPay.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // --- 1. VERIFICATION TOKEN (For Signup Email) ---
    public String generateVerificationToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey()) // Algorithm HS256 is auto-detected from key size
                .compact();
    }

    // --- 2. ACCESS TOKEN (Short lived: 15 mins) ---
    public String generateAccessToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15))
                .signWith(getSigningKey())
                .compact();
    }

    // --- 3. REFRESH TOKEN (Long lived: 7 days) ---
    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7))
                .signWith(getSigningKey())
                .compact();
    }

    // --- 4. PARSING & VALIDATION (Modern 0.12.x syntax) ---
    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // verifyWith replaces setSigningKey
                .build()
                .parseSignedClaims(token)    // parseSignedClaims replaces parseClaimsJws
                .getPayload()                // getPayload replaces getBody
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            // Catches ExpiredJwtException, MalformedJwtException, etc.
            return false;
        }
    }
}