package com.example.workspace.unfolded;

import com.example.workspace.subtask.command.domain.SubTaskRepository;
import com.example.workspace.task.command.domain.Task;
import com.example.workspace.task.command.domain.TaskRepository;
import com.example.workspace.trivia.command.domain.TriviaRepository;
import com.example.workspace.unfolded.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UnfoldedNoteService {

    private final UnfoldedNoteRepository unfoldedNoteRepository;
    private final UnfoldedNoteJdbcRepository unfoldedNoteJdbcRepository;

    private final TaskRepository taskRepository;
    private final SubTaskRepository subTaskRepository;
    private final TriviaRepository triviaRepository;

    @Transactional(readOnly = true)
    public List<UnfoldedNoteId> findAllUnfoldedNotes(final Long authorId) {
        List<UnfoldedNote> allByAuthorId = unfoldedNoteRepository.findAllByAuthorId(authorId);

        return allByAuthorId.stream()
                .map(UnfoldedNote::getId)
                .toList();
    }

    // 각 Repository 에서 따로 주입받는 것이 더 나을 것 같다
    // 아니 시바 들어간게 없는데 뭘 쳐 꺼내고있어
    @Transactional(readOnly = true)
    public List<TaskOverviewResponse> findAllNotesInfo(final Long authorId, final Long workSpaceId) {
        return unfoldedNoteJdbcRepository.findAllNotesInfoByWorkSpaceId(authorId, workSpaceId);
    }

    @Transactional
    public void addSidebarInfo(final UnfoldedNoteId unfoldedNoteId, final Long authorId) {
        UnfoldedNote unfoldedNote = new UnfoldedNote(unfoldedNoteId, authorId);
        unfoldedNoteRepository.save(unfoldedNote);
    }

    @Transactional
    public void addSidebarInfo(final UnfoldedNoteId unfoldedNoteId, final String title, final Long authorId) {
        UnfoldedNote unfoldedNote = unfoldedNoteRepository.findById(unfoldedNoteId)
                .orElseThrow(() -> new IllegalArgumentException("Cannot Find Unfolded Note!"));


    }
}
