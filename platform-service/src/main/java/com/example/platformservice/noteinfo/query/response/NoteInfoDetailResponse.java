package com.example.platformservice.noteinfo.query.response;

import com.example.platformservice.noteinfo.command.domain.value.Category;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@RequiredArgsConstructor
public class NoteInfoDetailResponse {

    private final Long infoId;
    private final String infoTitle;
    private final String infoDescription;

    private final List<Category> categories;

    private final Long authorId;

    private final Long snapshotId;

    private final BigDecimal price;

    /**
     * 아래의 것들은 클라이언트에서 Review 쪽으로 추가 요청을 보내서 해결하는 편이 효과적이다
     */
//    private final List<ReviewResponse> reviews;
//    private final Float averageRating;
//    private final Long reviewCount;

    /**
     * 이건 클라이언트에서 Like 를 호출해야겠지
     */
//    private final Long likeCount;

    /**
     * 이것 또한 클라이언트에서 Member 를 호출해야겠지?
     */
    // private final String authorNickname;
    // private final String authorImageKey;

    private final String snapshotImageKey;

    private final List<String> infoImageKeys;


}
