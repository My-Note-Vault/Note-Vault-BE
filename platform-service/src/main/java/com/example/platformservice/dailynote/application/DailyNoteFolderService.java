package com.example.platformservice.dailynote.application;

import com.example.platformservice.dailynote.domain.DailyNoteFolder;
import com.example.platformservice.dailynote.domain.DailyNoteFolderRepository;
import com.example.platformservice.dailynote.domain.DailyNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class DailyNoteFolderService {

    private final DailyNoteFolderRepository folderRepository;
    private final DailyNoteRepository dailyNoteRepository;

    @Transactional
    public Long create(final Long authorId, final String name) {
        String normalizedName = normalize(name);
        if (folderRepository.existsByAuthorIdAndName(authorId, normalizedName)) {
            throw new IllegalArgumentException("같은 이름의 DailyNote 폴더가 이미 있습니다");
        }
        return folderRepository.save(new DailyNoteFolder(authorId, normalizedName)).getId();
    }

    @Transactional
    public void rename(final Long authorId, final Long folderId, final String name) {
        DailyNoteFolder folder = findOwnedFolder(authorId, folderId);
        String normalizedName = normalize(name);
        if (folderRepository.existsByAuthorIdAndNameAndIdNot(authorId, normalizedName, folderId)) {
            throw new IllegalArgumentException("같은 이름의 DailyNote 폴더가 이미 있습니다");
        }
        folder.rename(normalizedName);
    }

    @Transactional
    public void delete(final Long authorId, final Long folderId) {
        DailyNoteFolder folder = findOwnedFolder(authorId, folderId);
        dailyNoteRepository.findAllByAuthorIdAndFolderId(authorId, folderId)
                .forEach(note -> note.moveToFolder(null));
        folderRepository.delete(folder);
    }

    private DailyNoteFolder findOwnedFolder(final Long authorId, final Long folderId) {
        return folderRepository.findByIdAndAuthorId(folderId, authorId)
                .orElseThrow(() -> new NoSuchElementException("일치하는 DailyNote 폴더가 없습니다"));
    }

    private String normalize(final String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("폴더 이름을 입력해야 합니다");
        }
        return name.trim();
    }
}
