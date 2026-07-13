package com.example.workspace.document.command.domain;

public enum DocumentType {
    TASK(null),
    SUBTASK(TASK),
    NOTE(SUBTASK);

    private final DocumentType parentType;

    DocumentType(final DocumentType parentType) {
        this.parentType = parentType;
    }

    public boolean requiresParent() {
        return parentType != null;
    }

    public DocumentType parentType() {
        return parentType;
    }

    public String notFoundMessage() {
        return this + " 문서를 찾을 수 없습니다";
    }

    public String parentNotFoundMessage() {
        if (!requiresParent()) {
            return this + " 문서는 부모 문서가 필요하지 않습니다";
        }
        return parentType + " 문서를 찾을 수 없습니다";
    }

    public String accessDeniedMessage() {
        return this + " 문서에 접근할 권한이 없습니다";
    }

    public String deleteDeniedMessage() {
        return this + " 문서를 삭제할 권한이 없습니다";
    }
}
