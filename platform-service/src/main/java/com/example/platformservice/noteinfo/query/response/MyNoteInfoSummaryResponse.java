package com.example.platformservice.noteinfo.query.response;


import com.example.platformservice.noteinfo.command.domain.value.Category;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class MyNoteInfoSummaryResponse {

    private final Long id;
    private final String title;
    private final String imageUrl;

    private final Float averageScore;
    private final String totalReviewCount;
    private final List<Category> categories;


}
