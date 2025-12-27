package com.example.platformservice.auth.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Component
@FeignClient(name = "googleTokenClient", url = "https://oauth2.googleapis.com")
public interface GoogleTokenClient {

    @PostMapping(value = "/token", consumes = "application/x-www-form-urlencoded")
    Map<String, Object> getToken(
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret,
            @RequestParam("code") String code,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("grant_type") String grantType
    );
}
