package com.example.platformservice.auth.ui;

import com.example.platformservice.auth.component.dto.OAuthUserInfo;
import com.example.platformservice.auth.application.OAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
@RequestMapping("/api/v1/oauth")
@RestController
public class OAuthController {

    private final OAuthService oAuthService;

    // 로그인 시작 (Google redirect)
    @GetMapping("/google")
    public void redirectToGoogle(final HttpServletResponse response) throws IOException {
        response.sendRedirect(oAuthService.buildAuthUrl());
    }

    // Google callback 처리 (code -> JWT)
    @GetMapping("/callback/google")
    public ResponseEntity<?> callbackFromGoogle(final @RequestParam String code) {
        OAuthUserInfo userInfo = oAuthService.handlerCallback(code);
        String jwt = oAuthService.issueJwt(userInfo);
        return ResponseEntity.ok(Map.of("token", jwt, "user", userInfo));
    }
}
