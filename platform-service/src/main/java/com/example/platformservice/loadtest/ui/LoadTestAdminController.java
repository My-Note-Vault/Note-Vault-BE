package com.example.platformservice.loadtest.ui;

import com.example.common.AuthMemberId;
import com.example.platformservice.loadtest.application.LoadTestAdminGate;
import com.example.platformservice.loadtest.application.LoadTestPoolResponse;
import com.example.platformservice.loadtest.application.LoadTestPoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@ConditionalOnProperty(name = "load-test.admin-api.enabled", havingValue = "true")
@RestController
@RequestMapping("/api/v1/internal/load-tests")
@RequiredArgsConstructor
public class LoadTestAdminController {
    private static final String LOAD_TEST_KEY_HEADER = "X-Load-Test-Key";

    private final LoadTestAdminGate adminGate;
    private final LoadTestPoolService loadTestPoolService;

    @PostMapping("/pools")
    public ResponseEntity<LoadTestPoolResponse> createPool(
            @AuthMemberId final Long memberId,
            @RequestHeader(LOAD_TEST_KEY_HEADER) final String loadTestKey,
            @RequestBody final CreateLoadTestPoolRequest request) {
        adminGate.verify(memberId, loadTestKey);
        return ResponseEntity.ok(loadTestPoolService.createPool(request.poolId(), request.userCount()));
    }

    @GetMapping("/pools/{poolId}")
    public ResponseEntity<LoadTestPoolResponse> findPool(
            @AuthMemberId final Long memberId,
            @RequestHeader(LOAD_TEST_KEY_HEADER) final String loadTestKey,
            @PathVariable final String poolId) {
        adminGate.verify(memberId, loadTestKey);
        return ResponseEntity.ok(loadTestPoolService.findPool(poolId));
    }

    @PostMapping("/pools/{poolId}/credentials")
    public ResponseEntity<byte[]> issueCredentials(
            @AuthMemberId final Long memberId,
            @RequestHeader(LOAD_TEST_KEY_HEADER) final String loadTestKey,
            @PathVariable final String poolId,
            @RequestBody final IssueLoadTestCredentialsRequest request) {
        adminGate.verify(memberId, loadTestKey);
        byte[] csv = loadTestPoolService.issueCredentialsCsv(poolId, request.userCount());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"load-test-users-" + poolId + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }
}
