package com.example.workspace.unfolded;

import com.example.workspace.unfolded.domain.NoteType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NoteInfoResponse {
    private final Long id;
    private final NoteType type;
    private final Long parentId;
    private final String title;
}
