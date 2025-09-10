package com.example.platformservice.auth.application;

import com.example.platformservice.auth.component.dto.OAuthUserInfo;
import com.example.platformservice.auth.feignclient.GoogleTokenClient;
import com.example.platformservice.auth.feignclient.GoogleUserClient;
import com.example.platformservice.auth.component.JwtProvider;
import com.example.platformservice.member.domain.Member;
import com.example.platformservice.member.infra.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@RequiredArgsConstructor
@Service
public class OAuthService {

    private final MemberRepository memberRepository;

    private final JwtProvider jwtProvider;

    private final GoogleTokenClient googleTokenClient;
    private final GoogleUserClient googleUserClient;

    public String buildAuthUrl() {
        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=GOOGLE_CLIENT_ID"
                + "&redirect_uri=http://localhost:8080/oauth2/callback/google"
                + "&response_type=code"
                + "&scope=openid%20email%20profile";
    }

    public OAuthUserInfo handlerCallback(final String code) {
        Map<String, Object> tokenResponse = googleTokenClient.getToken(Map.of(
                "client_id", "GOOGLE_CLIENT_ID",
                "client_secret", "GOOGLE_CLIENT_SECRET",
                "code", code,
                "grant_type", "authorization_code",
                "redirect_uri", "http://localhost:8080/oauth2/callback/google"
        ));

        String accessToken = (String) tokenResponse.get("access_token");

        Map<String, Object> userResponse = googleUserClient.getUserInfo(String.format("Bearer %s", accessToken));

        String email = (String) userResponse.get("email");
        String name = (String) userResponse.get("name");
        String providerUserId = (String) userResponse.get("id");

        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> memberRepository.save(Member.googleSignUp(email, name, providerUserId)));

        return new OAuthUserInfo(member.getId(), email, name, "google");
    }

    public String issueJwt(final OAuthUserInfo userInfo) {
        return jwtProvider.createToken(userInfo.getUserId(), userInfo.getEmail());
    }

}
