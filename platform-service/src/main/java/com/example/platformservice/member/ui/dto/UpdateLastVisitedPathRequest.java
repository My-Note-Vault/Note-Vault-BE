package com.example.platformservice.member.ui.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UpdateLastVisitedPathRequest {
    private final String lastVisitedPath;
}
