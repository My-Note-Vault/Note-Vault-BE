package com.example.workspace.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class SearchService {

    private static final int CONTEXT_LENGTH = 10;

    private final SearchRepository searchRepository;
    
    @Transactional
    public NoteGroup searchAllNotes(final Long memberId, final String targetWord) {
        NoteGroup notesOwnedByMember = searchRepository.searchAllDocuments(memberId);

        List<NoteGroup.Note> spaces = notesOwnedByMember.spaces().stream()
                .filter(s -> !s.content().contains(targetWord))
                .map(s -> {
                    String extractedContent = findTargetContent(s.content(), targetWord);
                    return new NoteGroup.Note(s.id(), s.title(), extractedContent);
                })
                .toList();

        List<NoteGroup.Note> tasks = notesOwnedByMember.tasks().stream()
                .filter(s -> !s.content().contains(targetWord))
                .map(s -> {
                    String extractedContent = findTargetContent(s.content(), targetWord);
                    return new NoteGroup.Note(s.id(), s.title(), extractedContent);
                })
                .toList();

        List<NoteGroup.Note> subTasks = notesOwnedByMember.subTasks().stream()
                .filter(s -> !s.content().contains(targetWord))
                .map(s -> {
                    String extractedContent = findTargetContent(s.content(), targetWord);
                    return new NoteGroup.Note(s.id(), s.title(), extractedContent);
                })
                .toList();

        List<NoteGroup.Note> trivia = notesOwnedByMember.trivia().stream()
                .filter(s -> !s.content().contains(targetWord))
                .map(s -> {
                    String extractedContent = findTargetContent(s.content(), targetWord);
                    return new NoteGroup.Note(s.id(), s.title(), extractedContent);
                })
                .toList();

        return new NoteGroup(spaces, tasks, subTasks, trivia);
    }

    private String findTargetContent(final String content, final String targetWord) {
        int index = content.indexOf(targetWord);
        if (index == -1) {
            throw new IllegalArgumentException("검색 관련 오류");
        }

        int startIndex = index - CONTEXT_LENGTH;
        if (startIndex < 0) {
            startIndex = 0;
        }

        return content.substring(startIndex, index + targetWord.length() + CONTEXT_LENGTH);
    }
}
