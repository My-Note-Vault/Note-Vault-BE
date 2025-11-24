package com.example.gateway;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class WhiteListRule {
    private final String pattern;
    private final String method;  // GET, POST, PUT, DELETE, OPTIONS
}
