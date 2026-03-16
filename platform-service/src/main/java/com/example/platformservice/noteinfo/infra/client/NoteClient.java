package com.example.platformservice.noteinfo.infra.client;

import com.example.common.api.NoteSummaryResponse;
import org.springframework.web.bind.annotation.RequestHeader;

public interface NoteClient {

    NoteSummaryResponse getNote(@RequestHeader("Authorization") String bearerToken);
}
