package com.domaindns.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expire-minutes}")
    private long expireMinutes;

    @Value("${jwt.admin-expire-minutes:120}")
    private long adminExpireMinutes;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issueToken(String subject, String role) {
        return issueToken(subject, role, expireMinutes);
    }

    public String issueAdminToken(String subject) {
        return issueToken(subject, "ADMIN", adminExpireMinutes);
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token);
    }

    private String issueToken(String subject, String role, long minutes) {
        long now = System.currentTimeMillis();
        Date exp = new Date(now + minutes * 60_000);
        return Jwts.builder()
                .setIssuer(issuer)
                .setId(UUID.randomUUID().toString())
                .setSubject(subject)
                .claim("role", role)
                .setIssuedAt(new Date(now))
                .setExpiration(exp)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }
}
