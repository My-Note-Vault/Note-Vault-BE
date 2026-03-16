package com.example.platformservice.member.infra;

import com.example.platformservice.member.domain.value.DayStartTime;
import com.example.platformservice.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    List<Member> findAllByDayStartTime(DayStartTime dayStartTime);
}
