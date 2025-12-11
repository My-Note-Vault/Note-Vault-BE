package com.example.platformservice.viewed.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecentlyViewedInfoRepository extends JpaRepository<RecentlyViewedInfo, RecentlyViewedId> {

    RecentlyViewedId id(RecentlyViewedId id);
}
