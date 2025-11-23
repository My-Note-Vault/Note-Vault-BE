package com.example.noteservice.snapshot.ui;

import com.example.noteservice.snapshot.command.application.SnapshotCommandService;
import com.example.noteservice.snapshot.query.SnapshotQueryService;
import com.example.noteservice.snapshot.query.response.ValidSnapshotsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/v1/snapshot")
@RestController
public class SnapshotController {

    private final SnapshotCommandService snapshotCommandService;
    private final SnapshotQueryService snapshotQueryService;

    /**
     * NoteInfo UPDATE 시 보여줄 적용 가능한 모든 Snapshot 들을 나열..
     * 여기서는 당연히 내가 작성한 애들을 보여주는 거니까 Blurred 가 아닌 Preview 로 올려야겠지?
     */
    @GetMapping
    public ResponseEntity<List<ValidSnapshotsResponse>> findAllValidSnapshots(
            @RequestHeader("X-Member-Id") final Long memberId
    ) {
        List<ValidSnapshotsResponse> responses = snapshotQueryService.findAllValidSnapshot(memberId);
        return ResponseEntity.ok(responses);
    }


}
