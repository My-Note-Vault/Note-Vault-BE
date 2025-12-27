package com.example.platformservice.auth.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@Component
@FeignClient(name = "googleTokenClient", url = "https://oauth2.googleapis.com")
public interface GoogleTokenClient {

    @PostMapping(value = "/token", consumes = "application/x-www-form-urlencoded")
    Map<String, Object> getToken(@RequestBody Map<String, ?> body);
}
