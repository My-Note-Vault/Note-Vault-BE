package com.example.platformservice.auth.application;

import com.example.common.exception.UnauthorizedException;
import com.example.common.jwt.JwtService;
import com.example.platformservice.auth.component.RefreshToken;
import com.example.platformservice.auth.component.RefreshTokenRepository;
import com.example.platformservice.auth.component.dto.OAuthUserInfo;
import com.example.platformservice.auth.feignclient.GoogleTokenClient;
import com.example.platformservice.auth.feignclient.GoogleUserClient;
import com.example.platformservice.auth.feignclient.KakaoTokenClient;
import com.example.platformservice.auth.feignclient.KakaoUserClient;
import com.example.platformservice.auth.ui.dto.TokenResponse;
import com.example.platformservice.member.domain.Member;
import com.example.platformservice.member.application.MemberIdentityGenerator;
import com.example.platformservice.member.domain.value.Provider;
import com.example.platformservice.member.infra.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.NoSuchElementException;
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

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    private final GoogleTokenClient googleTokenClient;
    private final GoogleUserClient googleUserClient;
    private final KakaoTokenClient kakaoTokenClient;
    private final KakaoUserClient kakaoUserClient;
    private final MemberRepository memberRepository;
    private final MemberIdentityGenerator memberIdentityGenerator;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    private static final Duration OAUTH_SESSION_TTL = Duration.ofMinutes(10);
    private final Map<String, OAuthSession> sessionStore = new ConcurrentHashMap<>();

    public Map<String, String> processGoogleLogin() throws NoSuchAlgorithmException {
        OAuthLoginRequest request = createLoginRequest(Provider.GOOGLE);

        String url = UriComponentsBuilder
                .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", googleRedirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", request.state())
                .queryParam("code_challenge", request.codeChallenge())
                .queryParam("code_challenge_method", "S256")
                .build()
                .toUriString();

        return Map.of("url", url);
    }

    public Map<String, String> processKakaoLogin() throws NoSuchAlgorithmException {
        OAuthLoginRequest request = createLoginRequest(Provider.KAKAO);

        String url = UriComponentsBuilder
                .fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("client_id", kakaoClientId)
                .queryParam("redirect_uri", kakaoRedirectUri)
                .queryParam("response_type", "code")
                .queryParam("state", request.state())
                .queryParam("code_challenge", request.codeChallenge())
                .queryParam("code_challenge_method", "S256")
                .build()
                .toUriString();

        return Map.of("url", url);
    }

    private OAuthLoginRequest createLoginRequest(Provider provider) throws NoSuchAlgorithmException {
        String state = UUID.randomUUID().toString();

        String codeVerifier = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(SecureRandom.getInstanceStrong().generateSeed(32));

        String codeChallenge = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(
                        MessageDigest.getInstance("SHA-256").digest(codeVerifier.getBytes(StandardCharsets.US_ASCII))
                );

        sessionStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
        sessionStore.put(state, new OAuthSession(provider, codeVerifier, Instant.now()));
        return new OAuthLoginRequest(state, codeChallenge);
    }


    @Transactional
    public OAuthUserInfo handleGoogleCallback(String code, String state) {
        String codeVerifier = consumeSession(state, Provider.GOOGLE);

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
        Member member = memberRepository.findByProviderAndProviderUserId(Provider.GOOGLE, providerUserId).orElse(null);
        if (member == null) {
            member = memberRepository.save(
                    Member.googleSignUp(
                            email,
                            name,
                            providerUserId,
                            memberIdentityGenerator.generateNickname(),
                            memberIdentityGenerator.generateUniqueTag()
                    )
            );
        } else if (member.getMemberTag() == null || member.getMemberTag().isBlank()) {
            member.assignMemberTag(memberIdentityGenerator.generateUniqueTag());
        }

        return new OAuthUserInfo(
                member.getId(),
                email,
                name,
                "google"
        );
    }

    @Transactional
    public OAuthUserInfo handleKakaoCallback(String code, String state) {
        String codeVerifier = consumeSession(state, Provider.KAKAO);

        Map<String, Object> tokenRequest = new java.util.HashMap<>();
        tokenRequest.put("code", code);
        tokenRequest.put("client_id", kakaoClientId);
        tokenRequest.put("redirect_uri", kakaoRedirectUri);
        tokenRequest.put("grant_type", "authorization_code");
        tokenRequest.put("code_verifier", codeVerifier);
        if (kakaoClientSecret != null && !kakaoClientSecret.isBlank()) {
            tokenRequest.put("client_secret", kakaoClientSecret);
        }

        Map<String, Object> tokenResponse = kakaoTokenClient.getToken(tokenRequest);
        String accessToken = (String) tokenResponse.get("access_token");
        if (accessToken == null) {
            throw new IllegalStateException("Kakao access_token 발급 실패");
        }

        Map<String, Object> userInfo = kakaoUserClient.getUserInfo("Bearer " + accessToken);
        String providerUserId = String.valueOf(userInfo.get("id"));
        if (providerUserId == null || "null".equals(providerUserId)) {
            throw new IllegalStateException("Kakao 사용자 식별자 조회 실패");
        }

        Map<String, Object> account = asMap(userInfo.get("kakao_account"));
        Map<String, Object> profile = asMap(account.get("profile"));
        String email = validKakaoEmail(account)
                ? (String) account.get("email")
                : "kakao-" + providerUserId + "@oauth.local";
        String name = stringValue(profile.get("nickname"), "카카오 사용자");

        Member member = memberRepository.findByProviderAndProviderUserId(Provider.KAKAO, providerUserId).orElse(null);
        if (member == null) {
            member = memberRepository.save(
                    Member.kakaoSignUp(
                            email,
                            name,
                            providerUserId,
                            memberIdentityGenerator.generateNickname(),
                            memberIdentityGenerator.generateUniqueTag()
                    )
            );
        } else if (member.getMemberTag() == null || member.getMemberTag().isBlank()) {
            member.assignMemberTag(memberIdentityGenerator.generateUniqueTag());
        }

        return new OAuthUserInfo(member.getId(), member.getEmail(), member.getName(), "kakao");
    }

    private String consumeSession(String state, Provider expectedProvider) {
        OAuthSession session = sessionStore.remove(state);
        if (session == null || session.provider() != expectedProvider || session.isExpired()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired state");
        }
        return session.codeVerifier();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
    }

    private boolean validKakaoEmail(Map<String, Object> account) {
        return account.get("email") instanceof String email
                && !email.isBlank()
                && Boolean.TRUE.equals(account.get("is_email_valid"))
                && Boolean.TRUE.equals(account.get("is_email_verified"));
    }

    private String stringValue(Object value, String fallback) {
        return value instanceof String string && !string.isBlank() ? string : fallback;
    }

    @Transactional
    public TokenResponse issueTokens(OAuthUserInfo userInfo) {
        String accessToken = jwtService.createAccessToken(userInfo.getUserId(), userInfo.getEmail());
        String refreshToken = jwtService.createRefreshToken(userInfo.getUserId(), userInfo.getEmail());
        String refreshTokenHash = hashToken(refreshToken);

        refreshTokenRepository.findByMemberId(userInfo.getUserId())
                .ifPresentOrElse(
                        savedToken -> savedToken.update(refreshTokenHash, jwtService.getExpiration(refreshToken)),
                        () -> refreshTokenRepository.save(
                                RefreshToken.create(userInfo.getUserId(), refreshTokenHash, jwtService.getExpiration(refreshToken))
                        )
                );
        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public TokenResponse refreshTokens(final String refreshToken) {
        if (jwtService.isInvalidToken(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("유효하지 않은 refresh 토큰입니다");
        }
        Long memberId = jwtService.getMemberId(refreshToken);
        RefreshToken savedRefreshToken = refreshTokenRepository.findByMemberId(memberId)
                .orElseThrow(() -> new UnauthorizedException("저장된 refresh 토큰이 없습니다"));

        if (!MessageDigest.isEqual(
                savedRefreshToken.getToken().getBytes(StandardCharsets.US_ASCII),
                hashToken(refreshToken).getBytes(StandardCharsets.US_ASCII)
        )) {
            throw new UnauthorizedException("저장된 refresh 토큰과 일치하지 않습니다");
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UnauthorizedException("회원 정보를 찾을 수 없습니다"));

        String newAccessToken = jwtService.createAccessToken(member.getId(), member.getEmail());
        String newRefreshToken = jwtService.createRefreshToken(member.getId(), member.getEmail());

        savedRefreshToken.update(hashToken(newRefreshToken), jwtService.getExpiration(newRefreshToken));
        refreshTokenRepository.save(savedRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void revokeRefreshToken(final String refreshToken) {
        if (refreshToken == null || jwtService.isInvalidToken(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            return;
        }

        Long memberId = jwtService.getMemberId(refreshToken);
        refreshTokenRepository.findByMemberId(memberId)
                .filter(saved -> MessageDigest.isEqual(
                        saved.getToken().getBytes(StandardCharsets.US_ASCII),
                        hashToken(refreshToken).getBytes(StandardCharsets.US_ASCII)
                ))
                .ifPresent(refreshTokenRepository::delete);
    }

    private String hashToken(final String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다", exception);
        }
    }

    private record OAuthLoginRequest(String state, String codeChallenge) {
    }

    private record OAuthSession(Provider provider, String codeVerifier, Instant createdAt) {
        private boolean isExpired() {
            return createdAt.plus(OAUTH_SESSION_TTL).isBefore(Instant.now());
        }
    }
}
