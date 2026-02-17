package com.example.demo.Authentication;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
public class JWTService {

    private static final String SECRET_KEY =
            "your-256-bit-secret-your-256-bit-secretuhefueshflmwlahdniuahfkjnaksfuhlauee,snckhabsfjkhajbdhsbkauhkjsndbayhegfjansdbauhfnu"; // ≥ 32 bytes

    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 24 hours

    // ---------------- CREATE TOKEN ----------------
    public String generateToken(UserModel user) {
        return Jwts.builder()
                .setSubject(user.getEmail())           // email as subject
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ---------------- EXTRACT EMAIL ----------------
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // ---------------- VALIDATE TOKEN ----------------
    public boolean isTokenValid(String token, UserModel user) {
        String email = extractEmail(token);
        return email.equals(user.getEmail()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    // ---------------- GENERIC CLAIM EXTRACTION ----------------
    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return resolver.apply(claims);
    }

    // ---------------- SIGNING KEY ----------------
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(
                SECRET_KEY.getBytes(StandardCharsets.UTF_8)
        );
    }
}
