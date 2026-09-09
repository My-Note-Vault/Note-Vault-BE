package com.example.platformservice.loadtest.application;

import com.example.common.jwt.JwtService;
import com.example.platformservice.dailynote.domain.DailyNote;
import com.example.platformservice.dailynote.domain.DailyNoteRepository;
import com.example.platformservice.loadtest.domain.LoadTestMember;
import com.example.platformservice.loadtest.domain.LoadTestMemberRepository;
import com.example.platformservice.loadtest.domain.LoadTestPool;
import com.example.platformservice.loadtest.domain.LoadTestPoolRepository;
import com.example.platformservice.loadtest.domain.LoadTestPoolStatus;
import com.example.platformservice.member.application.MemberIdentityGenerator;
import com.example.platformservice.member.domain.Member;
import com.example.platformservice.member.domain.value.DayStartTime;
import com.example.platformservice.member.infra.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoadTestPoolService {
    private static final String POOL_ID_PATTERN = "[a-z0-9][a-z0-9-]{2,79}";

    private final LoadTestPoolRepository loadTestPoolRepository;
    private final LoadTestMemberRepository loadTestMemberRepository;
    private final MemberRepository memberRepository;
    private final DailyNoteRepository dailyNoteRepository;
    private final MemberIdentityGenerator memberIdentityGenerator;
    private final JwtService jwtService;

    @Value("${load-test.admin-api.max-users}")
    private int maxUsers;

    @Transactional
    public LoadTestPoolResponse createPool(final String poolId, final int userCount) {
        validatePoolId(poolId);
        validateUserCount(userCount);
        return loadTestPoolRepository.findByPoolId(poolId)
                .map(pool -> existingPool(pool, userCount))
                .orElseGet(() -> createNewPool(poolId, userCount));
    }

    @Transactional(readOnly = true)
    public LoadTestPoolResponse findPool(final String poolId) {
        validatePoolId(poolId);
        return LoadTestPoolResponse.existing(findPoolOrThrow(poolId));
    }

    @Transactional(readOnly = true)
    public byte[] issueCredentialsCsv(final String poolId, final int userCount) {
        validatePoolId(poolId);
        validateUserCount(userCount);
        LoadTestPool pool = findPoolOrThrow(poolId);
        if (pool.getStatus() != LoadTestPoolStatus.READY) {
            throw new IllegalStateException("테스트 사용자 풀이 아직 준비되지 않았습니다");
        }
        if (userCount > pool.getUserCount()) {
            throw new IllegalArgumentException("요청 인원이 테스트 사용자 풀 크기를 초과합니다");
        }

        List<LoadTestMember> testMembers = loadTestMemberRepository.findByPoolIdOrderBySequenceNumber(
                poolId, PageRequest.of(0, userCount));
        if (testMembers.size() != userCount) {
            throw new IllegalStateException("테스트 사용자 풀 데이터가 완전하지 않습니다");
        }

        Map<Long, Member> membersById = memberRepository.findAllById(
                        testMembers.stream().map(LoadTestMember::getMemberId).toList())
                .stream().collect(Collectors.toMap(Member::getId, Function.identity()));

        StringBuilder csv = new StringBuilder("userId,accessToken,dailyNoteId\n");
        for (LoadTestMember testMember : testMembers) {
            Member member = membersById.get(testMember.getMemberId());
            String expectedProviderPrefix = "loadtest:" + poolId + ":";
            if (member == null || !member.getProviderUserId().startsWith(expectedProviderPrefix)) {
                throw new IllegalStateException("테스트 사용자 이외의 회원이 풀에 포함되어 있습니다");
            }
            csv.append(member.getId()).append(',')
                    .append(jwtService.createAccessToken(member.getId(), member.getEmail())).append(',')
                    .append(testMember.getDailyNoteId()).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private LoadTestPoolResponse createNewPool(final String poolId, final int userCount) {
        LoadTestPool pool = loadTestPoolRepository.save(new LoadTestPool(poolId));
        for (int sequence = 1; sequence <= userCount; sequence++) {
            String suffix = String.format("%06d", sequence);
            Member member = memberRepository.save(Member.googleSignUp(
                    "loadtest+" + poolId + "-" + suffix + "@example.invalid",
                    "LOAD_TEST_" + suffix,
                    "loadtest:" + poolId + ":" + suffix,
                    "부하테스트" + suffix,
                    memberIdentityGenerator.generateUniqueTag()));
            DailyNote dailyNote = dailyNoteRepository.save(new DailyNote(
                    member.getId(), "# Load Test Daily Note\n\nuser=" + suffix, DayStartTime.MIDNIGHT));
            loadTestMemberRepository.save(new LoadTestMember(
                    poolId, sequence, member.getId(), dailyNote.getId()));
        }
        pool.markReady(userCount);
        return LoadTestPoolResponse.created(pool);
    }

    private LoadTestPoolResponse existingPool(final LoadTestPool pool, final int requestedUserCount) {
        if (pool.getUserCount() != requestedUserCount) {
            throw new IllegalArgumentException("이미 존재하는 풀의 사용자 수와 요청 값이 다릅니다");
        }
        return LoadTestPoolResponse.existing(pool);
    }

    private LoadTestPool findPoolOrThrow(final String poolId) {
        return loadTestPoolRepository.findByPoolId(poolId)
                .orElseThrow(() -> new IllegalArgumentException("테스트 사용자 풀을 찾을 수 없습니다"));
    }

    private void validatePoolId(final String poolId) {
        if (poolId == null || !poolId.matches(POOL_ID_PATTERN)) {
            throw new IllegalArgumentException("poolId는 영문 소문자, 숫자, 하이픈으로 된 3~80자여야 합니다");
        }
    }

    private void validateUserCount(final int userCount) {
        if (userCount < 1 || userCount > maxUsers) {
            throw new IllegalArgumentException("userCount는 1 이상 " + maxUsers + " 이하여야 합니다");
        }
    }
}
