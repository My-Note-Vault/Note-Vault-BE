package com.example.search.chat.api;

import com.example.common.AuthMemberId;
import com.example.search.chat.policy.ChatPolicy;
import com.example.search.chat.policy.ChatPolicyProperties;
import com.example.search.chat.policy.ChatQuotaCalendar;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@RestController
public class ChatPolicyController {
    private final ChatPolicyProperties properties;
    private final ChatQuotaCalendar calendar;
    private final int maxOutputTokensPerCall;

    public ChatPolicyController(ChatPolicyProperties properties, ChatQuotaCalendar calendar,
                                @Value("${openai.chat.max-output-tokens:1200}") int maxOutputTokensPerCall) {
        if (maxOutputTokensPerCall <= 0) {
            throw new IllegalArgumentException("openai.chat.max-output-tokens must be positive");
        }
        this.properties = properties;
        this.calendar = calendar;
        this.maxOutputTokensPerCall = maxOutputTokensPerCall;
    }

    @GetMapping("/api/v1/chat/policy")
    public ResponseEntity<PolicyResponse> policy(@AuthMemberId Long memberId) {
        ChatQuotaCalendar.QuotaDay day = calendar.currentDay();
        PolicyResponse response = new PolicyResponse("USER", ChatPolicy.QUOTA_ZONE.getId(),
                ChatPolicy.RESET_TIME, day.date(), day.resetsAt(), properties.dailyTokenLimit(),
                ChatPolicy.MAX_QUESTION_LENGTH, ChatPolicy.MAX_SESSION_TITLE_LENGTH,
                ChatPolicy.MAX_CONCURRENT_RUNS_PER_SESSION,
                new RunLimits(properties.maxRunTokens(), properties.maxToolCalls(), properties.maxModelCalls(),
                        properties.runTimeout().toMillis(), maxOutputTokensPerCall),
                Arrays.stream(ChatPolicy.UsagePurpose.values())
                        .filter(ChatPolicy.UsagePurpose::chargedToDailyQuota).toList(),
                Arrays.stream(ChatPolicy.UsagePurpose.values())
                        .filter(purpose -> !purpose.chargedToDailyQuota()).toList());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    public record PolicyResponse(String quotaScope, String timezone, LocalTime resetTime,
                                 LocalDate quotaDate, Instant resetAt, long dailyTokenLimit,
                                 int maxQuestionLength, int maxSessionTitleLength,
                                 int maxConcurrentRunsPerSession, RunLimits runLimits,
                                 List<ChatPolicy.UsagePurpose> chargedPurposes,
                                 List<ChatPolicy.UsagePurpose> excludedPurposes) {
    }

    public record RunLimits(long maxTokens, int maxToolCalls, int maxModelCalls,
                            long timeoutMillis, int maxOutputTokensPerCall) {
    }
}
