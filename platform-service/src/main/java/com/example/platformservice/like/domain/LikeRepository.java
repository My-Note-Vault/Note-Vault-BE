package com.example.platformservice.like.domain;

import com.example.platformservice.like.infra.LikeDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long>, LikeDslRepository {

    Optional<Like> findByNoteInfoIdAndMemberId(Long noteInfoId, Long memberId);
}
