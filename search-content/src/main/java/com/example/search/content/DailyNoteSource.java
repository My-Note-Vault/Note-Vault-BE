package com.example.search.content;

import com.example.search.indexing.ContentChunker;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Entity(name = "SearchDailyNoteSource")
@Table(name = "daily_note")
@Immutable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyNoteSource {
    @Id private Long id;
    @Column(name = "author_id") private Long authorId;
    @Column(name = "logical_date") private LocalDate logicalDate;
    @Column(columnDefinition = "TEXT") private String content;
    @Column(name = "content_revision") private Long revision;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    public ContentSourceSnapshot snapshot(List<String> orderedPlans) {
        StringBuilder body = new StringBuilder(Objects.requireNonNullElse(content, ""));
        // Must match the API reader's string_agg order, separator and empty-value rules.
        for (String plan : orderedPlans) {
            if (plan != null && !plan.isEmpty()) body.append("\n\n").append(plan);
        }
        String text = body.toString();
        return new ContentSourceSnapshot(ContentSourceType.DAILY_NOTE, id, null, authorId, "daily", id,
                Objects.toString(logicalDate, ""), text, revision, ContentChunker.sha256(text), updatedAt);
    }
}
