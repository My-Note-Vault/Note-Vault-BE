package com.example.platformservice.auth.ui;

import com.example.platformservice.auth.application.OAuthService;
import com.example.platformservice.auth.component.dto.OAuthUserInfo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RequiredArgsConstructor
@RequestMapping("/api/v1/oauth")
@RestController
public class OAuthController {

    private final OAuthService oAuthService;

    // Google callback 처리 (code -> JWT)
    @GetMapping("/callback/google")
    public void callbackFromGoogle(
            final @RequestParam String code,
            final HttpServletResponse response
    ) throws IOException {

        OAuthUserInfo userInfo = oAuthService.handlerCallback(code);
        String jwt = oAuthService.issueJwt(userInfo);

        Cookie cookie = new Cookie("accessToken", jwt);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60);

        response.addCookie(cookie);
        response.sendRedirect("https://note-vault.cloud");
    }
}
