package com.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.common.api.NoteReader;
import com.example.common.api.SnapshotClient;
import com.example.common.file.ImageUtils;
import com.example.platformservice.auth.feignclient.GoogleTokenClient;
import com.example.platformservice.auth.feignclient.GoogleUserClient;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {

    @MockitoBean
    private GoogleTokenClient googleTokenClient;

    @MockitoBean
    private GoogleUserClient googleUserClient;

    @MockitoBean
    private SnapshotClient snapshotClient;

    @MockitoBean
    private NoteReader noteReader;

    @MockitoBean
    private ImageUtils imageUtils;

    @Test
    @DisplayName("애플리케이션 컨텍스트가 정상적으로 로드된다")
    void contextLoads() {
    }

}
