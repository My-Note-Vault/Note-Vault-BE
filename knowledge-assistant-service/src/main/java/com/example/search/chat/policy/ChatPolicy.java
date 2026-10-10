package com.example.search.chat.policy;

import com.example.search.chat.api.ChatErrorCode;
import com.example.search.chat.api.ChatException;

import java.time.LocalTime;
import java.time.ZoneId;

/** Rules shared by the legacy chat API and the session API. */
public final class ChatPolicy {
    public static final ZoneId QUOTA_ZONE = ZoneId.of("Asia/Seoul");
    public static final LocalTime RESET_TIME = LocalTime.MIDNIGHT;
    public static final int MAX_QUESTION_LENGTH = 4000;
    public static final int MAX_SESSION_TITLE_LENGTH = 100;
    public static final int MAX_CONCURRENT_RUNS_PER_SESSION = 1;

    private ChatPolicy() {
    }

    public static String normalizeQuestion(String question) {
        if (question == null || question.isBlank() || question.length() > MAX_QUESTION_LENGTH) {
            throw new ChatException(ChatErrorCode.INVALID_CHAT_REQUEST);
        }
        return question.strip();
    }

    /** Assign one purpose per API call; input/output subtotals must not be charged twice. */
    public enum UsagePurpose {
        MODEL_RESPONSE(true), // Includes decisions to call tools and final answers.
        QUERY_EMBEDDING(true),
        CONVERSATION_SUMMARY(true),
        DOCUMENT_INDEXING(false);

        private final boolean chargedToDailyQuota;

        UsagePurpose(boolean chargedToDailyQuota) {
            this.chargedToDailyQuota = chargedToDailyQuota;
        }

        public boolean chargedToDailyQuota() {
            return chargedToDailyQuota;
        }
    }
}
