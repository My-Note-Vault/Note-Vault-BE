package com.example.platformservice.auth.application;

import com.example.platformservice.auth.component.JwtProvider;
import com.example.platformservice.auth.component.dto.OAuthUserInfo;
import com.example.platformservice.auth.feignclient.GoogleTokenClient;
import com.example.platformservice.auth.feignclient.GoogleUserClient;
import com.example.platformservice.member.domain.Member;
import com.example.platformservice.member.infra.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

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

    public OAuthUserInfo handleGoogleCallback(String code) {
        // 1. authorization code로 access_token 요청
        Map<String, Object> tokenResponse = googleTokenClient.getToken(Map.of(
                "code", code,
                "client_id", googleClientId,
                "client_secret", googleClientSecret,
                "redirect_uri", googleRedirectUri,
                "grant_type", "authorization_code"
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
