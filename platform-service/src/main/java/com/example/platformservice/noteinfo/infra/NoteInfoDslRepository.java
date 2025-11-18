package com.example.platformservice.noteinfo.infra;

import com.example.platformservice.noteinfo.command.domain.value.Category;
import com.example.platformservice.noteinfo.query.response.InfoSummaryResponse;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteInfoDslRepository {

    List<InfoSummaryResponse> findPagedInfoSummaries(int page);

    List<InfoSummaryResponse> findPagedInfoSummariesByCategory(Category category, int page);
    List<InfoSummaryResponse> findBestInfoSummariesByCategory(Category category);
    List<InfoSummaryResponse> findHisBestInfoSummariesByCategory(Long authorId, Category category);
}
