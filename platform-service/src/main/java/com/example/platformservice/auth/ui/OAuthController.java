package com.example.platformservice.auth.ui;

import com.example.platformservice.auth.application.OAuthService;
import com.example.platformservice.auth.component.dto.OAuthUserInfo;
import com.example.platformservice.auth.ui.dto.RefreshTokenRequest;
import com.example.platformservice.auth.ui.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/oauth")
@RestController
public class OAuthController {

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
        TokenResponse tokenResponse = oAuthService.issueTokens(userInfo);

        return ResponseEntity.ok(
                Map.of(
                        "token", tokenResponse,
                        "user", userInfo
                )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @RequestBody RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(oAuthService.refreshTokens(request.getRefreshToken()));
    }

}
