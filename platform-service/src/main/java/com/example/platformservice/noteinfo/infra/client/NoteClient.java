package com.example.platformservice.noteinfo.infra.client;

import com.example.common.api.NoteSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "noteClient", url = "https://www.rootdomain.com")
public interface NoteClient {

    @GetMapping("/oauth2/v2/userinfo")
    NoteSummaryResponse getNote(@RequestHeader("Authorization") String bearerToken);
}
