package com.example.platformservice.auth.application;

import com.example.platformservice.auth.component.JwtProvider;
import com.example.platformservice.auth.component.dto.OAuthUserInfo;
import com.example.platformservice.auth.feignclient.GoogleTokenClient;
import com.example.platformservice.auth.feignclient.GoogleUserClient;
import com.example.platformservice.member.domain.Member;
import com.example.platformservice.member.infra.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Service
public class OAuthService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    private final GoogleTokenClient googleTokenClient;
    private final GoogleUserClient googleUserClient;
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    private final Map<String, String> sessionStore = new ConcurrentHashMap<>();

    public Map<String, String> processLogin() throws NoSuchAlgorithmException {
        String state = UUID.randomUUID().toString();

        String codeVerifier = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(SecureRandom.getInstanceStrong().generateSeed(32));

        String codeChallenge = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(
                        MessageDigest.getInstance("SHA-256").digest(codeVerifier.getBytes(StandardCharsets.US_ASCII))
                );

        sessionStore.put(state, codeVerifier);

        String url = UriComponentsBuilder
                .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", googleRedirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build()
                .toUriString();

        return Map.of("url", url);
    }


    public OAuthUserInfo handleGoogleCallback(String code, String state) {

        String codeVerifier = sessionStore.remove(state);
        if (codeVerifier == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid state");
        }

        // 1. authorization code로 access_token 요청
        Map<String, Object> tokenResponse = googleTokenClient.getToken(Map.of(
                "code", code,
                "client_id", googleClientId,
                "client_secret", googleClientSecret,
                "redirect_uri", googleRedirectUri,
                "grant_type", "authorization_code",
                "code_verifier", codeVerifier
        ));

        String accessToken = (String) tokenResponse.get("access_token");
        if (accessToken == null) {
            throw new IllegalStateException("Google access_token 발급 실패");
        }

        // 2. access_token으로 사용자 정보 조회
        Map<String, Object> userInfo = googleUserClient.getUserInfo("Bearer " + accessToken);

        String email = (String) userInfo.get("email");
        String name = (String) userInfo.get("name");
        String providerUserId = (String) userInfo.get("id");

        // 3. 회원 조회 또는 가입
        Member member = memberRepository.findByEmail(email)
                .orElseGet(() ->
                        memberRepository.save(
                                Member.googleSignUp(email, name, providerUserId)
                        )
                );

        return new OAuthUserInfo(
                member.getId(),
                email,
                name,
                "google"
        );
    }

    public String issueJwt(OAuthUserInfo userInfo) {
        return jwtProvider.createToken(
                userInfo.getUserId(),
                userInfo.getEmail()
        );
    }
}
