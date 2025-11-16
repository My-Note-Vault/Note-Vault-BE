package com.example.platformservice.noteinfo.command.application.request;

import com.example.platformservice.noteinfo.command.domain.value.Category;
import com.example.platformservice.noteinfo.command.domain.value.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class CreateNoteInfoRequest {

    private final Long noteId;
    private final String title;
    private final String description;

    private final List<Category> categories;

    private final Status status;
    private final BigDecimal price;

}
