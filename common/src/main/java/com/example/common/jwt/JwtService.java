package com.example.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Service
public class JwtService {

    private final Key key;

    @Value("${jwt.access-token-expiration}")
    private Duration accessTokenExpiration;
    @Value("${jwt.refresh-token-expiration}")
    private Duration refreshTokenExpiration;

    private static final String MEMBER_ID_CLAIM = "memberId";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    public JwtService(@Value("${jwt.secret-key}") String secretKey) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String createAccessToken(final Long memberId, final String email) {
        return createToken(memberId, email, ACCESS_TOKEN_TYPE, accessTokenExpiration.toMillis());
    }

    public String createRefreshToken(final Long memberId, final String email) {
        return createToken(memberId, email, REFRESH_TOKEN_TYPE, refreshTokenExpiration.toMillis());
    }

    public boolean isInvalidToken(final String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isAccessToken(final String token) {
        return ACCESS_TOKEN_TYPE.equals(getTokenType(token));
    }

    public boolean isRefreshToken(final String token) {
        return REFRESH_TOKEN_TYPE.equals(getTokenType(token));
    }

    public Long getMemberId(final String token) {
        return getClaims(token).get(MEMBER_ID_CLAIM, Long.class);
    }

    public LocalDateTime getExpiration(final String token) {
        Instant expiration = getClaims(token).getExpiration().toInstant();
        return LocalDateTime.ofInstant(expiration, ZoneOffset.UTC);
    }

    private String createToken(
            final Long memberId,
            final String email,
            final String tokenType,
            final long expiration
    ) {
        return Jwts.builder()
                .setSubject(email)
                .claim(MEMBER_ID_CLAIM, memberId)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    private String getTokenType(final String token) {
        return getClaims(token).get(TOKEN_TYPE_CLAIM, String.class);
    }

    private Claims getClaims(final String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}
