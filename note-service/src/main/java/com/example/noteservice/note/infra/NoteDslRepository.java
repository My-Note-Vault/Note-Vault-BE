package com.example.noteservice.note.infra;

import com.example.common.api.NoteSummaryResponse;

import java.util.List;

public interface NoteDslRepository {

    List<NoteSummaryResponse> findMyAllNotes(Long memberId);
}
