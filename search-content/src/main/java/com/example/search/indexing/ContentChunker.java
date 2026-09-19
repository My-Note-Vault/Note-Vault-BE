package com.example.search.indexing;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/** Markdown chunking performed only in the worker. */
@Component
public class ContentChunker {
    public List<ChunkDraft> chunk(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> sections = new ArrayList<>();
        StringBuilder section = new StringBuilder();
        boolean fencedCode = false;
        for (String line : raw.replace("\r\n", "\n").split("\n", -1)) {
            if (line.stripLeading().startsWith("```") || line.stripLeading().startsWith("~~~")) {
                fencedCode = !fencedCode;
            }
            if (!fencedCode && line.matches("^#{1,6}\\s+.+$") && !section.isEmpty()) {
                sections.add(section.toString().strip());
                section.setLength(0);
            }
            if (!section.isEmpty()) {
                section.append('\n');
            }
            section.append(line);
        }
        if (!section.isEmpty()) {
            sections.add(section.toString().strip());
        }

        List<ChunkDraft> result = new ArrayList<>();
        StringBuilder accumulated = new StringBuilder();
        for (String value : sections) {
            if (!accumulated.isEmpty()) {
                accumulated.append('\n');
            }
            accumulated.append(value);
            if (accumulated.codePointCount(0, accumulated.length()) >= 700) {
                addDrafts(result, accumulated.toString());
                accumulated.setLength(0);
            }
        }
        if (!accumulated.isEmpty()) {
            addDrafts(result, accumulated.toString());
        }
        return result;
    }

    private void addDrafts(List<ChunkDraft> result, String content) {
        int start = 0;
        while (start < content.length()) {
            int end = content.offsetByCodePoints(
                    start, Math.min(1800, content.codePointCount(start, content.length())));
            String part = content.substring(start, end).strip();
            if (!part.isEmpty()) {
                result.add(new ChunkDraft(result.size(), part, sha256(part)));
            }
            start = end;
        }
    }

    public static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

}
