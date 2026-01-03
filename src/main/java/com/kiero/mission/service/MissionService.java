package com.kiero.mission.service;

import com.kiero.child.domain.Child;
import com.kiero.child.repository.ChildRepository;
import com.kiero.global.exception.KieroException;
import com.kiero.mission.domain.Mission;
import com.kiero.mission.exception.MissionErrorCode;
import com.kiero.mission.presentation.dto.MissionCreateRequest;
import com.kiero.mission.presentation.dto.MissionResponse;
import com.kiero.mission.repository.MissionRepository;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.repository.ParentChildRepository;
import com.kiero.parent.repository.ParentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private final ParentRepository parentRepository;
    private final ChildRepository childRepository;
    private final ParentChildRepository parentChildRepository;

    @Transactional
    public MissionResponse createMission(Long parentId, MissionCreateRequest request) {
        // 1. 부모-자녀 관계 검증
        boolean isValidRelation = parentChildRepository.existsByParentIdAndChildId(
                parentId,
                request.childId()
        );

        if (!isValidRelation) {
            throw new KieroException(MissionErrorCode.NOT_YOUR_CHILD);
        }

        // 2. 부모 엔티티 조회
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new KieroException(MissionErrorCode.NOT_YOUR_CHILD));

        // 3. 자녀 엔티티 조회
        Child child = childRepository.findById(request.childId())
                .orElseThrow(() -> new KieroException(MissionErrorCode.CHILD_NOT_FOUND));

        // 4. 미션 생성
        Mission mission = Mission.create(
                parent,
                child,
                request.name(),
                request.reward(),
                request.dueAt()
        );

        Mission savedMission = missionRepository.save(mission);

        log.info("Mission created: missionId={}, parentId={}, childId={}, name={}",
                savedMission.getId(), parentId, request.childId(), request.name());

        return MissionResponse.from(savedMission);
    }

    public List<MissionResponse> getMissionsByParent(Long parentId, Long childId) {
        // Case1. 특정 자녀 조회
        if (childId != null) {
            validateParentChildRelation(parentId, childId);
            return missionRepository.findAllByChildId(childId).stream()
                    .map(MissionResponse::from)
                    .toList();
        }

        // Case2. 전체 자녀 조회
        return missionRepository.findAllByParentId(parentId).stream()
                .map(MissionResponse::from)
                .toList();
    }

    public List<MissionResponse> getMissionsByChild(Long childId) {
        return missionRepository.findAllByChildId(childId).stream()
                .map(MissionResponse::from)
                .toList();
    }

    private void validateParentChildRelation(Long parentId, Long childId) {
        if (!parentChildRepository.existsByParentIdAndChildId(parentId, childId)) {
            throw new KieroException(MissionErrorCode.NOT_YOUR_CHILD);
        }
    }
}
