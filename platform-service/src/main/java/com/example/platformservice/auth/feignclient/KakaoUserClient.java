package com.example.platformservice.auth.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "kakaoUserClient", url = "https://kapi.kakao.com")
public interface KakaoUserClient {

    @GetMapping("/v2/user/me")
    Map<String, Object> getUserInfo(@RequestHeader("Authorization") String bearerToken);
}
