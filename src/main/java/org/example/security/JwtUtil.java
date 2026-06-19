package org.example.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public final class JwtUtil {
    private final byte[] secretBytes;
    private static final long EXPIRATION_MS = 86400000;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        try {
            this.secretBytes = Base64.getDecoder().decode(secret);
            System.out.println("Decoded secret key length (bytes): " + secretBytes.length);
            System.out.println("Decoded secret key length (bits): " + (secretBytes.length * 8));
            if (secretBytes.length < 64) {
                throw new IllegalArgumentException("Decoded secret key is too short for HS512. Must be at least 512 bits (64 bytes).");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64-encoded secret key in application.properties: " + secret, e);
        }
    }

    @PostConstruct
    public void init() {
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        String encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());
        System.out.println("Generated HS512 Key (Base64): " + encodedKey);
    }

    public String generateToken(String email,String fullName) {
        return generateToken(email, fullName, "ROLE_USER"); // Default to ROLE_USER for backward compatibility
    }

    public String generateToken(String email, String fullName, String role) {
        SecretKey key = Keys.hmacShaKeyFor(secretBytes);
        return Jwts.builder()
                .setSubject(email)
                .claim("fullName", fullName)
                .claim("role", role)
                .setIssuer("Klaro API")
                .setAudience("Klaro Client")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public String extractEmail(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secretBytes);
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
    }

    public String extractFullName(String token){
        SecretKey key = Keys.hmacShaKeyFor(secretBytes);
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("fullName",String.class);
    }

    public String extractRole(String token){
        SecretKey key = Keys.hmacShaKeyFor(secretBytes);
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretBytes);
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
            return "Klaro API".equals(claims.getIssuer()) && "Klaro Client".equals(claims.getAudience());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}