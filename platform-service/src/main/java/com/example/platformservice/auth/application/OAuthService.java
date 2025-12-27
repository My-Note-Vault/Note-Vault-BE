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
    private String googleCallbackUrl;

    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;
    private final GoogleTokenClient googleTokenClient;
    private final GoogleUserClient googleUserClient;


    public OAuthUserInfo handleCallback(final String code) {

        Map<String, Object> tokenResponse = googleTokenClient.getToken(
                googleClientId,
                googleClientSecret,
                code,
                googleCallbackUrl,
                "authorization_code"
        );

        String accessToken = (String) tokenResponse.get("access_token");
        if (accessToken == null) {
            throw new IllegalStateException("Google access_token 발급 실패");
        }

        Map<String, Object> userResponse =
                googleUserClient.getUserInfo("Bearer " + accessToken);

        String email = (String) userResponse.get("email");
        String name = (String) userResponse.get("name");
        String providerUserId = (String) userResponse.get("id");

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


    public String issueJwt(final OAuthUserInfo userInfo) {
        return jwtProvider.createToken(
                userInfo.getUserId(),
                userInfo.getEmail()
        );
    }
}
