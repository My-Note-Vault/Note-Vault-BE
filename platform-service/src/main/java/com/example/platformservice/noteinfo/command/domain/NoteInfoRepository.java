package com.example.platformservice.noteinfo.command.domain;

import com.example.platformservice.noteinfo.command.domain.value.Status;
import com.example.platformservice.noteinfo.infra.NoteInfoDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteInfoRepository extends JpaRepository<NoteInfo, Long>, NoteInfoDslRepository {

    Optional<NoteInfo> findByIdAndAuthorId(Long id, Long authorId);

    List<NoteInfo> findByIdAndStatusEquals(Long id, Status status);

    Optional<NoteInfo> findByIdAndStatusNot(Long id, Status status);
}
