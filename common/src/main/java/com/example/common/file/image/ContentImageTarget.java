package com.example.common.file.image;

import java.util.Locale;

public enum ContentImageTarget {
    DAILY_NOTE("daily-note"),
    WORKSPACE("workspace"),
    TASK("task"),
    NOTE("note"),
    TRIVIA("trivia");

    private final String path;

    ContentImageTarget(final String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public static ContentImageTarget from(final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("이미지 대상 타입은 필수입니다.");
        }

        String normalized = normalize(value);
        for (ContentImageTarget target : values()) {
            if (normalize(target.name()).equals(normalized) || normalize(target.path).equals(normalized)) {
                return target;
            }
        }

        throw new IllegalArgumentException("지원하지 않는 이미지 대상 타입입니다.");
    }

    private static String normalize(final String value) {
        return value.trim()
                .replace("-", "")
                .replace("_", "")
                .toUpperCase(Locale.ROOT);
    }
}
