package com.example.platformservice.review.command.domain;

import com.example.platformservice.review.infra.ReviewDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewDslRepository {
}
