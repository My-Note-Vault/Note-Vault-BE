package com.example.platformservice.viewed.application;

import com.example.platformservice.viewed.domain.RecentlyViewedId;
import com.example.platformservice.viewed.domain.RecentlyViewedInfo;
import com.example.platformservice.viewed.domain.RecentlyViewedInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class RecentlyViewedInfoService {

    private final RecentlyViewedInfoRepository recentlyViewedInfoRepository;

    @Transactional
    public RecentlyViewedId save(final Long noteInfoId, final Long memberId) {
        RecentlyViewedId id = new RecentlyViewedId(memberId, noteInfoId);
        try {
            recentlyViewedInfoRepository.save(new RecentlyViewedInfo(id));
        } catch (DataIntegrityViolationException e) {
            RecentlyViewedInfo recentlyViewedInfo = recentlyViewedInfoRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("최근 본 상품의 정합성이 깨졌습니다."));

            recentlyViewedInfo.update();
        }
        return id;
    }

    @Transactional
    public void delete(final Long noteInfoId, final Long memberId) {
        RecentlyViewedId id = new RecentlyViewedId(memberId, noteInfoId);
        recentlyViewedInfoRepository.deleteById(id);
    }


}
