package com.example.platformservice.dailynote.query;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TomorrowTodo {
    private final Long authorId;
    private final String todo;
}
