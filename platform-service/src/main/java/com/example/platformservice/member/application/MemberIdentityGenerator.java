package com.example.platformservice.member.application;

import com.example.platformservice.member.infra.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MemberIdentityGenerator {

    private static final List<String> ADJECTIVES = List.of(
            "차분한", "용감한", "즐거운", "빛나는", "부지런한", "따뜻한", "산뜻한", "다정한", "씩씩한", "기민한",
            "성실한", "유쾌한", "슬기로운", "포근한", "자유로운", "명랑한"
    );
    private static final List<String> NOUNS = List.of(
            "여우", "수달", "고래", "참새", "판다", "토끼", "해달", "사슴",
            "부엉이", "다람쥐", "펭귄", "돌고래", "알파카", "고슴도치", "두루미", "북극곰"
    );
    private static final char[] TAG_CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int TAG_LENGTH = 6;
    private static final int MAX_TAG_ATTEMPTS = 20;

    private final MemberRepository memberRepository;
    private final SecureRandom random = new SecureRandom();

    public String generateNickname() {
        return randomItem(ADJECTIVES) + randomItem(NOUNS);
    }

    public String generateUniqueTag() {
        for (int attempt = 0; attempt < MAX_TAG_ATTEMPTS; attempt++) {
            String candidate = generateTag();
            if (!memberRepository.existsByMemberTag(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("회원 식별 태그를 생성하지 못했습니다");
    }

    private String generateTag() {
        StringBuilder tag = new StringBuilder(TAG_LENGTH);
        for (int index = 0; index < TAG_LENGTH; index++) {
            tag.append(TAG_CHARACTERS[random.nextInt(TAG_CHARACTERS.length)]);
        }
        return tag.toString();
    }

    private <T> T randomItem(List<T> values) {
        return values.get(random.nextInt(values.size()));
    }
}
