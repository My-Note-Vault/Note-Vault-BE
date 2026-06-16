package com.example.workspace.note.command.application;

import com.example.common.file.image.ImageUtils;
import com.example.workspace.note.command.domain.Note;
import com.example.workspace.note.command.domain.NoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NoteCommandServiceTest {

    private static final String NOTE_NOT_FOUND_MESSAGE = "DailyNote 를 찾을 수 없습니다";

    @InjectMocks
    private NoteCommandService noteCommandService;

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private ImageUtils imageUtils;

    @Nested
    @DisplayName("createNote 메소드는")
    class CreateNoteTest {

        @Test
        @DisplayName("기본 Note를 저장한다")
        void createNote_success() {
            noteCommandService.createNote(1L, 2L);

            ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
            verify(noteRepository).save(noteCaptor.capture());

            Note savedNote = noteCaptor.getValue();
            assertThat(savedNote.getAuthorId()).isEqualTo(1L);
            assertThat(savedNote.getSubTaskId()).isEqualTo(2L);
            assertThat(savedNote.getTitle()).isEqualTo("새 Note");
            assertThat(savedNote.getContent()).isEmpty();
            assertThat(savedNote.getIsPublic()).isFalse();
        }
    }

    @Nested
    @DisplayName("editNote 메소드는")
    class EditNoteTest {

        @Test
        @DisplayName("작성자가 Note를 수정하면 변경 내용을 저장하고 제거된 이미지를 정리한다")
        void editNote_success() {
            Note note = new Note(1L, 2L, "기존 Note", "old-content", false);
            given(noteRepository.findById(3L)).willReturn(Optional.of(note));

            noteCommandService.editNote(1L, 3L, 4L, "수정된 Note", "new-content", true);

            assertThat(note.getSubTaskId()).isEqualTo(4L);
            assertThat(note.getTitle()).isEqualTo("수정된 Note");
            assertThat(note.getContent()).isEqualTo("new-content");
            assertThat(note.getIsPublic()).isTrue();
            verify(imageUtils).deleteRemovedContentImages("old-content", "new-content");
            verify(noteRepository).save(note);
        }

        @Test
        @DisplayName("존재하지 않는 Note면 예외가 발생한다")
        void editNote_noteNotFound() {
            given(noteRepository.findById(3L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> noteCommandService.editNote(1L, 3L, 4L, "수정된 Note", "new-content", true))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage(NOTE_NOT_FOUND_MESSAGE);

            verify(imageUtils, never()).deleteRemovedContentImages("old-content", "new-content");
        }
    }

    @Nested
    @DisplayName("deleteNote 메소드는")
    class DeleteNoteTest {

        @Test
        @DisplayName("작성자가 Note를 삭제하면 본문 이미지를 모두 정리하고 삭제한다")
        void deleteNote_success() {
            Note note = new Note(1L, 2L, "Note", "content", false);
            given(noteRepository.findById(3L)).willReturn(Optional.of(note));

            noteCommandService.deleteNote(1L, 3L);

            verify(imageUtils).deleteAllContentImages("content");
            verify(noteRepository).delete(note);
        }

        @Test
        @DisplayName("작성자가 아니면 삭제할 수 없다")
        void deleteNote_forbidden() {
            Note note = new Note(1L, 2L);
            given(noteRepository.findById(3L)).willReturn(Optional.of(note));

            assertThatThrownBy(() -> noteCommandService.deleteNote(9L, 3L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("삭제할 권한이 없습니다.");

            verify(imageUtils, never()).deleteAllContentImages(note.getContent());
            verify(noteRepository, never()).delete(note);
        }
    }
}
