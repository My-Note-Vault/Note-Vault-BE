package com.example.common.api;

public interface NoteReader {

    NoteSummaryResponse getNoteSummary(Long noteId, Long memberId);

    SnapshotDetailResponse getSnapshotDetail(Long noteId);
}
