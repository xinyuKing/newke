package com.shixi.ecommerce.security;

import com.shixi.ecommerce.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final long validityMs;

    public JwtTokenProvider(JwtSecurityProperties properties) {
        String secret = SecuritySecretValidator.requireStrongSecret(properties.getSecret(), 32, "security.jwt.secret");
        long expireMinutes = properties.getExpireMinutes();
        if (expireMinutes < 1) {
            throw new IllegalStateException("security.jwt.expire-minutes must be greater than 0");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.validityMs = expireMinutes * 60 * 1000;
    }

    public String createToken(String username, Long userId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(username)
                .claim("uid", userId)
                .claim("role", role.name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(validityMs)))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Role getRole(String token) {
        String role = String.valueOf(parseClaims(token).get("role"));
        return Role.valueOf(role);
    }

    public Long getUserId(String token) {
        Object value = parseClaims(token).get("uid");
        if (value == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
