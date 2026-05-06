package com.example.workspace.search;

import java.util.List;

public record NoteGroup(

        List<Note> spaces,
        List<Note> tasks,
        List<Note> subTasks,
        List<Note> trivia

) {
    public record Note(Long id, String title, String content) {}
}
