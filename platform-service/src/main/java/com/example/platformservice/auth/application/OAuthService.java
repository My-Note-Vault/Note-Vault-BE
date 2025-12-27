package com.example.platformservice.auth.application;

import com.example.platformservice.auth.component.JwtProvider;
import com.example.platformservice.auth.component.dto.OAuthUserInfo;
import com.example.platformservice.member.domain.Member;
import com.example.platformservice.member.infra.MemberRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@RequiredArgsConstructor
@Service
public class OAuthService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    /**
     * Google OAuth callback 처리
     */
    public OAuthUserInfo handleGoogleCallback(String code) {
        try {
            // 1️⃣ code → token (Google SDK)
            GoogleTokenResponse tokenResponse =
                    new GoogleAuthorizationCodeTokenRequest(
                            new NetHttpTransport(),
                            GsonFactory.getDefaultInstance(),
                            googleClientId,
                            googleClientSecret,
                            code,
                            googleRedirectUri
                    ).execute();

            String idTokenString = tokenResponse.getIdToken();
            if (idTokenString == null) {
                throw new IllegalStateException("Google id_token 발급 실패");
            }


            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new IllegalStateException("Google id_token 검증 실패");
            }


            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String providerUserId = payload.getSubject(); // Google user id


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

        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Google OAuth 처리 실패", e);
        }
    }

    public String issueJwt(OAuthUserInfo userInfo) {
        return jwtProvider.createToken(
                userInfo.getUserId(),
                userInfo.getEmail()
        );
    }
}
