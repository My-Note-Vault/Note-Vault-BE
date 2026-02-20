package com.example.workspace.workspace.query;

import com.example.workspace.workspace.command.domain.WorkSpace;
import com.example.workspace.workspace.command.domain.WorkSpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class WorkSpaceQueryService {

    private final WorkSpaceRepository workSpaceRepository;

    public WorkSpace findWorkSpaceById(
            final Long authorId,
            final Long workSpaceId
    ) {
        WorkSpace workSpace = workSpaceRepository.findById(workSpaceId)
                .orElseThrow(() -> new NoSuchElementException("WorkSpace 를 찾을 수 없습니다"));

        if (!workSpace.getCreatorId().equals(authorId)) {
            throw new IllegalArgumentException("조회가 허용되지 않았습니다");
        }
        return workSpace;
    }

    public List<WorkSpace> findAllWorkSpacesByCreatorId(final Long memberId) {
        return workSpaceRepository.findAllByCreatorId(memberId);
    }


}
