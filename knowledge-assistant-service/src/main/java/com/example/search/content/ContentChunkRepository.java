package com.example.search.content;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ContentChunkRepository extends JpaRepository<ContentChunk,Long> {
    List<ContentChunk> findAllBySourceTypeAndSourceIdOrderByChunkIndexAsc(ContentSourceType type,Long sourceId);
    @Query("select c from ContentChunk c where c.sourceType=:type and c.sourceId=:sourceId and (c.embeddingStatus<>:ready or c.embeddingModel is null or c.embeddingModel<>:model) order by c.chunkIndex")
    List<ContentChunk> findEmbeddingTargets(@Param("type") ContentSourceType type,@Param("sourceId") Long sourceId,@Param("ready") EmbeddingStatus ready,@Param("model") String model);
    @Query(value="""
            SELECT c.* FROM content_chunk c
            WHERE c.embedding_status='READY' AND c.embedding_model=:model
              AND ((c.source_type='DOCUMENT'
                    AND EXISTS (SELECT 1 FROM workspace_member wm WHERE wm.workspace_id=c.workspace_id AND wm.member_id=:memberId)
                    AND EXISTS (SELECT 1 FROM document d WHERE d.id=c.source_id AND CAST(d.search_revision AS VARCHAR)=c.source_version))
                OR (c.source_type='DAILY_NOTE' AND c.owner_id=:memberId
                    AND EXISTS (SELECT 1 FROM daily_note n WHERE n.id=c.source_id AND n.updated_at=c.source_updated_at)))
            ORDER BY c.id DESC
            """,nativeQuery=true)
    List<ContentChunk> findAccessibleCandidates(@Param("memberId") Long memberId,@Param("model") String model,Pageable pageable);
}
