package com.example.platformservice.like.infra;

public interface LikeDslRepository {
    Long countByNoteInfoId(Long noteInfoId);
}
