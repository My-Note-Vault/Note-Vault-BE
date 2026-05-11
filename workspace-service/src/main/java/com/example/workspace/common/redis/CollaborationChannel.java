package com.example.workspace.common.redis;

public final class CollaborationChannel {

    private static final String PREFIX = "collab";

    private CollaborationChannel() {
    }

    public static String documentChannel(final String documentType, final Long documentId) {
        return PREFIX + ":" + documentType + ":" + documentId;
    }

    public static String pattern() {
        return PREFIX + ":*";
    }
}
