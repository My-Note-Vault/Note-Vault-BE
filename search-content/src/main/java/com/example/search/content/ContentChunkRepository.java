package com.example.search.content;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ContentChunkRepository extends JpaRepository<ContentChunk,Long> {
    List<ContentChunk> findAllBySourceTypeAndSourceIdOrderByChunkIndexAsc(ContentSourceType type,Long sourceId);
    @Query("select c from ContentChunk c where c.sourceType=:type and c.sourceId=:sourceId and (c.embeddingStatus<>:ready or c.embeddingModel is null or c.embeddingModel<>:model) order by c.chunkIndex")
    List<ContentChunk> findEmbeddingTargets(@Param("type") ContentSourceType type,@Param("sourceId") Long sourceId,@Param("ready") EmbeddingStatus ready,@Param("model") String model);
}
