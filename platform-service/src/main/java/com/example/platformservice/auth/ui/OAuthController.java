package com.example.platformservice.auth.ui;

import com.example.platformservice.auth.application.OAuthService;
import com.example.platformservice.auth.component.dto.OAuthUserInfo;
import com.example.platformservice.auth.ui.dto.RefreshTokenRequest;
import com.example.platformservice.auth.ui.dto.TokenResponse;
import com.example.platformservice.auth.ui.dto.AccessTokenResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.time.Duration;
import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/oauth")
@RestController
public class OAuthController {

    private final OAuthService oAuthService;

    @Value("${auth.refresh-cookie.name:refresh_token}")
    private String refreshCookieName;
    @Value("${auth.refresh-cookie.secure:true}")
    private boolean refreshCookieSecure;
    @Value("${auth.refresh-cookie.same-site:Lax}")
    private String refreshCookieSameSite;
    @Value("${auth.refresh-cookie.path:/api/v1/oauth}")
    private String refreshCookiePath;
    @Value("${auth.refresh-cookie.max-age:30d}")
    private Duration refreshCookieMaxAge;

    @GetMapping("/login/google")
    public Map<String, String> loginGoogle() throws Exception {
        return oAuthService.processGoogleLogin();
    }

    @GetMapping("/login/kakao")
    public Map<String, String> loginKakao() throws Exception {
        return oAuthService.processKakaoLogin();
    }

    @GetMapping("/callback/kakao")
    public ResponseEntity<Map<String, Object>> callbackFromKakao(
            @RequestParam("code") String code,
            @RequestParam("state") String state
    ) {
        OAuthUserInfo userInfo = oAuthService.handleKakaoCallback(code, state);
        TokenResponse tokenResponse = oAuthService.issueTokens(userInfo);

        return loginResponse(tokenResponse, userInfo);
    }

    @GetMapping("/callback/google")
    public ResponseEntity<Map<String, Object>> callbackFromGoogle(
            @RequestParam("code") String code,
            @RequestParam("state") String state
    ) {

        OAuthUserInfo userInfo = oAuthService.handleGoogleCallback(code, state);
        TokenResponse tokenResponse = oAuthService.issueTokens(userInfo);

        return loginResponse(tokenResponse, userInfo);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refreshToken(
            HttpServletRequest httpRequest,
            @RequestBody(required = false) RefreshTokenRequest request
    ) {
        String cookieRefreshToken = refreshTokenFrom(httpRequest);
        String refreshToken = cookieRefreshToken != null
                ? cookieRefreshToken
                : request == null ? null : request.getRefreshToken();
        TokenResponse tokens = oAuthService.refreshTokens(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.getRefreshToken()).toString())
                .body(new AccessTokenResponse(tokens.getAccessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String refreshToken = refreshTokenFrom(request);
        oAuthService.revokeRefreshToken(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .build();
    }

    private ResponseEntity<Map<String, Object>> loginResponse(
            TokenResponse tokens,
            OAuthUserInfo userInfo
    ) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.getRefreshToken()).toString())
                .body(Map.of(
                        "token", new AccessTokenResponse(tokens.getAccessToken()),
                        "user", userInfo
                ));
    }

    private ResponseCookie refreshCookie(String token) {
        return ResponseCookie.from(refreshCookieName, token)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path(refreshCookiePath)
                .maxAge(refreshCookieMaxAge)
                .build();
    }

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from(refreshCookieName, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path(refreshCookiePath)
                .maxAge(java.time.Duration.ZERO)
                .build();
    }

    private String refreshTokenFrom(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> refreshCookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

}
