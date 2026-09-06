package com.example.platformservice.member.infra;

import com.example.platformservice.member.domain.value.DayStartTime;
import com.example.platformservice.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByMemberTag(String memberTag);

    List<Member> findAllByDayStartTime(DayStartTime dayStartTime);

    @Query("""
            select m from Member m
            where m.createdAt >= :from and m.createdAt < :to and m.isDeleted = false
            """)
    List<Member> findActiveMembersCreatedBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("select count(m) from Member m where m.isDeleted = false")
    long countActiveMembers();
}
