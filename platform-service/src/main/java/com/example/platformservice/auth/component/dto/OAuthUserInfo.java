package com.example.platformservice.auth.component.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OAuthUserInfo {

    private final Long userId;
    private final String email;
    private final String name;
    private final String provider;

}
