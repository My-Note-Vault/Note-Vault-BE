package com.example.platformservice.like.application;

import com.example.platformservice.like.domain.Like;
import com.example.platformservice.like.domain.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class LikeService {

    private final LikeRepository likeRepository;

    @Transactional
    public Long like(final Long memberId, final Long noteInfoId) {
        Like like = new Like(memberId, noteInfoId);
        likeRepository.save(like);
        return like.getId();
    }

    @Transactional
    public void deleteLike(final Long noteInfoId, final Long memberId) {
        Like like = likeRepository.findByNoteInfoIdAndMemberId(noteInfoId, memberId)
                .orElseThrow(() -> new NoSuchElementException("좋아요가 존재하지 않습니다."));

        likeRepository.delete(like);
    }

    @Transactional(readOnly = true)
    public Long countLikesByNoteInfoId(final Long noteInfoId) {
        return likeRepository.countByNoteInfoId(noteInfoId);
    }

}
