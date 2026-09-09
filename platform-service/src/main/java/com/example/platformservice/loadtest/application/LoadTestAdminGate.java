package com.example.platformservice.loadtest.application;

import com.example.common.exception.ForbiddenException;
import com.example.platformservice.member.domain.Member;
import com.example.platformservice.member.domain.value.Role;
import com.example.platformservice.member.infra.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
public class LoadTestAdminGate {
    private final MemberRepository memberRepository;

    @Value("${load-test.admin-api.key}")
    private String configuredKey;

    public void verify(final Long memberId, final String suppliedKey) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ForbiddenException("부하 테스트 관리 권한이 없습니다"));
        if (member.getRole() != Role.ADMIN) {
            throw new ForbiddenException("부하 테스트 관리 권한이 없습니다");
        }
        if (configuredKey.isBlank() || suppliedKey == null || !constantTimeEquals(configuredKey, suppliedKey)) {
            throw new ForbiddenException("부하 테스트 관리 권한이 없습니다");
        }
    }

    private boolean constantTimeEquals(final String expected, final String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
