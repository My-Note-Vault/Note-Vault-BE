package com.example.platformservice.auth.component;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RefreshTokenStore {

    private final Map<Long, String> refreshTokens = new ConcurrentHashMap<>();

    public void save(final Long memberId, final String refreshToken) {
        refreshTokens.put(memberId, refreshToken);
    }

    public boolean matches(final Long memberId, final String refreshToken) {
        return refreshToken.equals(refreshTokens.get(memberId));
    }
}
