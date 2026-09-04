package com.example.workspace.document.command.domain;

public enum DocumentType {
    WORKSPACE_HOME,
    TASK,
    NOTE;

    public String notFoundMessage() {
        return this + " 문서를 찾을 수 없습니다";
    }

    public String parentNotFoundMessage() {
        return "부모 문서를 찾을 수 없습니다";
    }

    public String accessDeniedMessage() {
        return this + " 문서에 접근할 권한이 없습니다";
    }

    public String deleteDeniedMessage() {
        return this + " 문서를 삭제할 권한이 없습니다";
    }
}
