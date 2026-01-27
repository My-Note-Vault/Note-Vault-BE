package com.example.platformservice.auth.ui;

import com.example.platformservice.auth.application.OAuthService;
import com.example.platformservice.auth.component.dto.OAuthUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/oauth")
@RestController
public class OAuthController {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    private final OAuthService oAuthService;

    @GetMapping("/login/google")
    public Map<String, String> loginGoogle() throws Exception {
        return oAuthService.processLogin();
    }




    @GetMapping("/callback/google")
    public ResponseEntity<Map<String, Object>> callbackFromGoogle(
            @RequestParam("code") String code,
            @RequestParam("state") String state
    ) {

        OAuthUserInfo userInfo = oAuthService.handleGoogleCallback(code, state);
        String jwt = oAuthService.issueJwt(userInfo);

        return ResponseEntity.ok(
                Map.of(
                        "token", jwt,
                        "user", userInfo
                )
        );
    }


}
