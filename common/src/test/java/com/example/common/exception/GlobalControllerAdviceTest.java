package com.example.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalControllerAdviceTest {

    private final GlobalControllerAdvice globalControllerAdvice = new GlobalControllerAdvice();

    @Test
    @DisplayName("IllegalArgumentException은 400으로 응답한다")
    void handleIllegalArgumentException_returnsBadRequest() {
        IllegalArgumentException exception = new IllegalArgumentException("잘못된 입력");

        ResponseEntity<GlobalErrorFormat> response =
                globalControllerAdvice.handleGeneralServerExceptions(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(GlobalErrorCode.INVALID_INPUT_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("잘못된 입력");
    }
}
