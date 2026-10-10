package com.example.search.chat.api;

import org.springframework.http.HttpStatus;

public enum ChatErrorCode {
    INVALID_CHAT_REQUEST(HttpStatus.BAD_REQUEST, "채팅 요청의 입력값을 확인해 주세요."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "대화방을 찾을 수 없습니다."),
    RUN_NOT_FOUND(HttpStatus.NOT_FOUND, "실행 기록을 찾을 수 없습니다."),
    SESSION_BUSY(HttpStatus.CONFLICT, "이 대화방에서 이전 질문을 처리하고 있습니다."),
    REQUEST_ID_CONFLICT(HttpStatus.CONFLICT, "같은 요청 ID를 다른 요청에 사용할 수 없습니다."),
    DAILY_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "오늘 사용할 수 있는 토큰이 부족합니다."),
    RUN_TOKEN_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "질문 한 건의 토큰 한도에 도달했습니다."),
    TOOL_CALL_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "도구 호출 횟수 한도에 도달했습니다."),
    MODEL_CALL_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "모델 호출 횟수 한도에 도달했습니다."),
    RUN_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "질문 처리 제한 시간을 초과했습니다."),
    SEARCH_FAILED(HttpStatus.BAD_GATEWAY, "문서 검색을 준비하는 중 오류가 발생했습니다."),
    TOOL_EXECUTION_FAILED(HttpStatus.BAD_GATEWAY, "도구를 실행하는 중 오류가 발생했습니다."),
    MODEL_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "답변을 생성하는 중 오류가 발생했습니다."),
    MODEL_RESPONSE_INCOMPLETE(HttpStatus.BAD_GATEWAY, "모델 응답이 완료되기 전에 종료되었습니다."),
    INTERNAL_CHAT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "질문을 처리하는 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ChatErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String message() {
        return message;
    }
}
