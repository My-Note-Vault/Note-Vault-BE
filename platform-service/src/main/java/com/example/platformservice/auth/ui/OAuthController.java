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

    // Google callback 처리 (code -> JWT)
    @GetMapping("/callback/google")
    public ResponseEntity<Map<String, Object>> callbackFromGoogle(final @RequestParam String code) {
        OAuthUserInfo userInfo = oAuthService.handlerCallback(code);
        String jwt = oAuthService.issueJwt(userInfo);
        return ResponseEntity.ok(Map.of("token", jwt, "user", userInfo));
    }
}
