package com.example.platformservice.auth.ui;

import com.example.platformservice.auth.application.OAuthService;
import com.example.platformservice.auth.component.dto.OAuthUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequiredArgsConstructor
@RequestMapping("/api/v1/oauth")
@RestController
public class OAuthController {

    private final OAuthService oAuthService;

    /**
     * Google OAuth callback
     * code → id_token → login / register 분기
     */
    @GetMapping("/callback/google")
    public ResponseEntity<Map<String, Object>> callbackFromGoogle(
            @RequestParam("code") String code
    ) {
        OAuthUserInfo userInfo = oAuthService.handleGoogleCallback(code);
        String jwt = oAuthService.issueJwt(userInfo);

        // 👉 실무에서는 body 반환보다 redirect + cookie 권장
        return ResponseEntity.ok(
                Map.of(
                        "token", jwt,
                        "user", userInfo
                )
        );
    }
}
