package com.election.evm.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String userId, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("uid", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractUserId(String token) {
        return parseClaims(token).get("uid", String.class);
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token, String email) {
        Claims claims = parseClaims(token);
        return email.equalsIgnoreCase(claims.getSubject()) && claims.getExpiration().after(new Date());
    }

    private Claims parseClaims(String token) {
        try {
            // First try the newer parserBuilder API via reflection (works with jjwt 0.11+ / 0.12+)
            java.lang.reflect.Method parserBuilderMethod = Jwts.class.getMethod("parserBuilder");
            Object builder = parserBuilderMethod.invoke(null);
            java.lang.reflect.Method setSigningKeyMethod = builder.getClass().getMethod("setSigningKey", java.security.Key.class);
            Object parserBuilder = setSigningKeyMethod.invoke(builder, key);
            java.lang.reflect.Method buildMethod = parserBuilder.getClass().getMethod("build");
            Object parser = buildMethod.invoke(parserBuilder);
            java.lang.reflect.Method parseClaimsJws = parser.getClass().getMethod("parseClaimsJws", String.class);
            Object jws = parseClaimsJws.invoke(parser, token);
            java.lang.reflect.Method getBody = jws.getClass().getMethod("getBody");
            return (Claims) getBody.invoke(jws);
        } catch (NoSuchMethodException e) {
            try {
                // Fallback to older parser() API
                java.lang.reflect.Method parserMethod = Jwts.class.getMethod("parser");
                Object parserObj = parserMethod.invoke(null);
                java.lang.reflect.Method setSigningKey = parserObj.getClass().getMethod("setSigningKey", byte[].class);
                Object parserWithKey = setSigningKey.invoke(parserObj, (Object) key.getEncoded());
                java.lang.reflect.Method parseClaimsJws = parserWithKey.getClass().getMethod("parseClaimsJws", String.class);
                Object jws = parseClaimsJws.invoke(parserWithKey, token);
                java.lang.reflect.Method getBody = jws.getClass().getMethod("getBody");
                return (Claims) getBody.invoke(jws);
            } catch (Exception ex) {
                throw new RuntimeException("Unable to parse JWT token", ex);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Unable to parse JWT token", ex);
        }
    }
}
