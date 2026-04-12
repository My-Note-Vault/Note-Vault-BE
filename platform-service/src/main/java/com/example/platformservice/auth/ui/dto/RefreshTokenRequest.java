package com.example.platformservice.auth.ui.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RefreshTokenRequest {

    private final String refreshToken;
}
