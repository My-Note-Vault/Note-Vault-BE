package com.example.platformservice.draw.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface DailyDrawResultRepository extends JpaRepository<DailyDrawResult, Long> {
    boolean existsByDrawDate(LocalDate drawDate);
    List<DailyDrawResult> findTop60ByOrderByDrawDateDescCategoryAsc();
}
