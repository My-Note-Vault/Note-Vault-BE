package com.example.search.retrieval;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** Selects bounded, non-overlapping passages without persisting chunks or generating vectors. */
@Component
public class SourceExcerptSelector {
    private static final int MAX_CODE_POINTS = 1800;
    private static final int MAX_EXCERPTS = 2;

    public List<String> select(String content, String question, List<String> terms, int limit) {
        if (content == null || content.isBlank() || limit < 1) return List.of();
        String text = content.strip();
        int count = Math.min(limit, MAX_EXCERPTS);
        List<Range> selected = new ArrayList<>();
        List<String> needles = Stream.concat(Stream.of(question), terms.stream())
                .filter(Objects::nonNull).map(String::strip).filter(value -> !value.isEmpty())
                .distinct().toList();
        for (String needle : needles) {
            int from = 0;
            while (selected.size() < count) {
                int match = findIgnoreCase(text, needle, from);
                if (match < 0) break;
                Range covered = containing(selected, match);
                if (covered != null) {
                    from = covered.end();
                    continue;
                }
                Range range = window(text, match, needle.length(), selected);
                selected.add(range);
                from = range.end();
            }
            if (selected.size() == count) break;
        }
        if (selected.isEmpty()) {
            selected.add(new Range(0, advance(text, 0, MAX_CODE_POINTS)));
        }
        return selected.stream().map(range -> text.substring(range.start(), range.end()).strip())
                .filter(value -> !value.isEmpty()).toList();
    }

    private Range window(String text, int match, int matchLength, List<Range> selected) {
        int before = text.codePointCount(0, match);
        int start = text.offsetByCodePoints(match, -Math.min(MAX_CODE_POINTS / 3, before));
        int paragraphStart = text.lastIndexOf("\n\n", Math.max(0, match - 1));
        if (paragraphStart >= start && paragraphStart + 2 <= match) start = paragraphStart + 2;
        for (Range range : selected) {
            if (range.end() <= match) start = Math.max(start, range.end());
        }
        int end = advance(text, start, MAX_CODE_POINTS);
        int paragraphEnd = text.indexOf("\n\n", match + matchLength);
        if (paragraphEnd >= 0 && paragraphEnd < end) end = paragraphEnd;
        for (Range range : selected) {
            if (range.start() > match) end = Math.min(end, range.start());
        }
        return new Range(start, end);
    }

    private int advance(String text, int from, int codePoints) {
        return text.offsetByCodePoints(from, Math.min(codePoints, text.codePointCount(from, text.length())));
    }

    private Range containing(List<Range> ranges, int position) {
        for (Range range : ranges) {
            if (position >= range.start() && position < range.end()) return range;
        }
        return null;
    }

    private int findIgnoreCase(String text, String needle, int from) {
        for (int index = from; index <= text.length() - needle.length(); index++) {
            if (text.regionMatches(true, index, needle, 0, needle.length())) return index;
        }
        return -1;
    }

    private record Range(int start, int end) { }
}
