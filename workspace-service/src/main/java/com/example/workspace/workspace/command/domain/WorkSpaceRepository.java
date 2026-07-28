package com.example.workspace.workspace.command.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkSpaceRepository extends JpaRepository<WorkSpace, Long> {

    List<WorkSpace> findAllByCreatorId(Long authorId);

    List<WorkSpace> findByIdIn(Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WorkSpace w where w.id = :id")
    Optional<WorkSpace> findWithWriteLockById(@Param("id") Long id);
}
