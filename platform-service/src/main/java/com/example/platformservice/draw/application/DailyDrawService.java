package com.example.platformservice.draw.application;

import com.example.platformservice.draw.domain.*;
import com.example.platformservice.member.domain.Member;
import com.example.platformservice.member.infra.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

@RequiredArgsConstructor
@Service
public class DailyDrawService {
    private final DailyDrawResultRepository resultRepository;
    private final MemberRepository memberRepository;
    private final JdbcTemplate jdbcTemplate;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public void draw(LocalDate date) {
        if (resultRepository.existsByDrawDate(date)) return;
        long total = memberRepository.countActiveMembers();
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        List<Member> joined = memberRepository
                .findActiveMembersCreatedBetween(
                        date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        List<Member> writers = membersByIds(writerMemberIds(date));
        resultRepository.save(result(date, DrawCategory.NEW_MEMBER, joined, total, now));
        resultRepository.save(result(date, DrawCategory.DOCUMENT_WRITER, writers, total, now));
    }

    private List<Long> writerMemberIds(LocalDate date) {
        return jdbcTemplate.queryForList("""
                select a.member_id
                from document_edit_activity a
                join member m on m.id = a.member_id
                where a.activity_date = ? and m.is_deleted = false
                group by a.member_id
                having sum(a.inserted_character_count) >= 200
                order by a.member_id
                """, Long.class, date);
    }

    @Transactional(readOnly = true)
    public DrawOverviewResponse overview(Long memberId) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        List<Member> newMembers = memberRepository.findActiveMembersCreatedBetween(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        List<Long> documentWriterIds = writerMemberIds(today);
        List<DrawOverviewResponse.EligibleCount> eligibleCounts = List.of(
                new DrawOverviewResponse.EligibleCount(DrawCategory.NEW_MEMBER, newMembers.size(),
                        newMembers.stream().anyMatch(member -> member.getId().equals(memberId))),
                new DrawOverviewResponse.EligibleCount(DrawCategory.DOCUMENT_WRITER, documentWriterIds.size(),
                        documentWriterIds.contains(memberId)));
        Map<LocalDate, List<DailyDrawResult>> grouped = new LinkedHashMap<>();
        resultRepository.findTop60ByOrderByDrawDateDescCategoryAsc().forEach(r ->
                grouped.computeIfAbsent(r.getDrawDate(), key -> new ArrayList<>()).add(r));
        List<DrawOverviewResponse.DrawDay> days = grouped.values().stream().map(results ->
                new DrawOverviewResponse.DrawDay(
                        results.getFirst().getDrawDate(), results.getFirst().getDrawnAt(),
                        results.stream().map(r ->
                                new DrawOverviewResponse.DrawResult(r.getCategory(), r.getWinnerMemberId(),
                                        r.getWinnerNameSnapshot(), r.getEligibleCount())).toList()
                )).toList();
        return new DrawOverviewResponse(eligibleCounts, days);
    }

    private List<Member> membersByIds(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        Map<Long, Member> byId = new HashMap<>();
        memberRepository.findAllById(ids).forEach(member -> byId.put(member.getId(), member));
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    private DailyDrawResult result(LocalDate date, DrawCategory category, List<Member> candidates,
                                   long total, LocalDateTime now) {
        Member winner = candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
        String name = winner == null ? null : winner.getNickname() == null || winner.getNickname().isBlank()
                ? winner.getName() : winner.getNickname();
        return new DailyDrawResult(date, category, winner == null ? null : winner.getId(), name,
                candidates.size(), total, now);
    }
}
