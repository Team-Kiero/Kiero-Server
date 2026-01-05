package com.kiero.mission.service;

import com.kiero.child.domain.Child;
import com.kiero.child.exception.ChildErrorCode;
import com.kiero.child.repository.ChildRepository;
import com.kiero.global.exception.KieroException;
import com.kiero.mission.domain.Mission;
import com.kiero.mission.exception.MissionErrorCode;
import com.kiero.mission.presentation.dto.MissionBulkCreateRequest;
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

import java.time.LocalDate;
import java.util.ArrayList;
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
        boolean isValidRelation = parentChildRepository.existsByParent_IdAndChild_Id(
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
                .orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

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

    @Transactional(readOnly = true)
    public List<MissionResponse> getMissionsByParent(Long parentId, Long childId) {
        LocalDate today = LocalDate.now();

        // Case1. 특정 자녀 조회
        if (childId != null) {
            validateParentChildRelation(parentId, childId);
            return missionRepository.findAllByChild_IdAndDueAtGreaterThanEqual(childId, today).stream()
                    .map(MissionResponse::from)
                    .toList();
        }

        // Case2. 전체 자녀 조회
        return missionRepository.findAllByParent_IdAndDueAtGreaterThanEqual(parentId, today).stream()
                .map(MissionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MissionResponse> getMissionsByChild(Long childId) {
        LocalDate today = LocalDate.now();

        return missionRepository.findAllByChild_IdAndDueAtGreaterThanEqual(childId, today).stream()
                .map(MissionResponse::from)
                .toList();
    }

    @Transactional
    public MissionResponse completeMission(Long childId, Long missionId) {
        // 1. 미션 조회
        Mission mission = missionRepository.findByIdWithLock(missionId)
                .orElseThrow(() -> new KieroException(MissionErrorCode.MISSION_NOT_FOUND));

        // 2. 소유 여부 검증
        if (!mission.getChild().getId().equals(childId)) {
            throw new KieroException(MissionErrorCode.NOT_YOUR_MISSION);
        }

        // 3. 중복 완료 방지
        if (mission.isCompleted()) {
            throw new KieroException(MissionErrorCode.MISSION_ALREADY_COMPLETED);
        }

        // 4. 마감일 체크
        if (mission.getDueAt().isBefore(LocalDate.now())) {
            throw new KieroException(MissionErrorCode.MISSION_EXPIRED);
        }

        // 5. 미션 완료처리
        mission.complete();

        // 6. 코인 지급
        mission.getChild().addCoin(mission.getReward());

        log.info("Mission completed: missionId={}, childId={}, reward={}", missionId, childId, mission.getReward());

        return MissionResponse.from(mission);
    }

    @Transactional
    public List<MissionResponse> bulkCreateMissions(Long parentId, MissionBulkCreateRequest request) {
        // 1. 부모-자녀 관계 검증
        validateParentChildRelation(parentId, request.childId());

        // 2. 부모 엔티티 조회
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new KieroException(MissionErrorCode.NOT_YOUR_CHILD));

        // 3. 자녀 엔티티 조회
        Child child = childRepository.findById(request.childId())
                .orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

        // 4. 미션 일괄 생성
        List<Mission> missions = new ArrayList<>();
        for (MissionBulkCreateRequest.MissionItem item : request.missions()) {
            Mission mission = Mission.create(
                    parent,
                    child,
                    item.name(),
                    item.reward(),
                    item.dueAt()
            );
            missions.add(mission);
        }

        // 5. 일괄 저장
        List<Mission> savedMissions = missionRepository.saveAll(missions);

        log.info("Bulk created {} missions for parentId={}, childId={}",
                savedMissions.size(), parentId, request.childId());

        return savedMissions.stream()
                .map(MissionResponse::from)
                .toList();
    }

    private void validateParentChildRelation(Long parentId, Long childId) {
        if (!parentChildRepository.existsByParent_IdAndChild_Id(parentId, childId)) {
            throw new KieroException(MissionErrorCode.NOT_YOUR_CHILD);
        }
    }
}
