package com.kiero.mission.service;

import com.kiero.child.domain.Child;
import com.kiero.child.exception.ChildErrorCode;
import com.kiero.child.repository.ChildRepository;
import com.kiero.global.exception.KieroException;
import com.kiero.mission.domain.Mission;
import com.kiero.mission.exception.MissionErrorCode;
import com.kiero.mission.presentation.dto.MissionBulkCreateRequest;
import com.kiero.mission.presentation.dto.MissionCompleteEvent;
import com.kiero.mission.presentation.dto.MissionCreateRequest;
import com.kiero.mission.presentation.dto.MissionResponse;
import com.kiero.mission.repository.MissionRepository;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.exception.ParentErrorCode;
import com.kiero.parent.repository.ParentChildRepository;
import com.kiero.parent.repository.ParentRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionService {

    private final ApplicationEventPublisher eventPublisher;

    private final MissionRepository missionRepository;
    private final ParentRepository parentRepository;
    private final ChildRepository childRepository;
    private final ParentChildRepository parentChildRepository;

    private final EntityManager em;
    private final ResourceLoader resourceLoader;

    @Transactional
    public MissionResponse createMission(Long parentId, Long childId, MissionCreateRequest request) {
        // 1. 부모-자녀 관계 검증
        validateParentChildRelation(parentId, childId);

        // 2. 부모 엔티티 조회
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));

        // 3. 자녀 엔티티 조회
        Child child = childRepository.findById(childId)
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
                savedMission.getId(), parentId, childId, request.name());

        return MissionResponse.from(savedMission);
    }

    @Transactional(readOnly = true)
    public List<MissionResponse> getMissionsByParent(Long parentId, Long childId) {
        LocalDate today = LocalDate.now();

        // Case1. 특정 자녀 조회
        if (childId != null) {
            validateParentChildRelation(parentId, childId);
            return missionRepository.findAllByChildIdAndDueAtGreaterThanEqual(childId, today).stream()
                    .map(MissionResponse::from)
                    .toList();
        }

        // Case2. 전체 자녀 조회
        return missionRepository.findAllByParentIdAndDueAtGreaterThanEqual(parentId, today).stream()
                .map(MissionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MissionResponse> getMissionsByChild(Long childId) {
        LocalDate today = LocalDate.now();

        return missionRepository.findAllByChildIdAndDueAtGreaterThanEqual(childId, today).stream()
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

        // 5. 자녀 조회
        Child child = childRepository.findByIdWithLock(childId)
                .orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

        // 6. 미션 완료처리
        mission.complete();

        // 7. 코인 지급
        child.addCoin(mission.getReward());

        eventPublisher.publishEvent(new MissionCompleteEvent(
            child.getId(),
            mission.getReward(),
            mission.getName(),
            LocalDateTime.now()
        ));

        log.info("Mission completed: missionId={}, childId={}, reward={}, newCoinAmount={}",
                missionId, childId, mission.getReward(), child.getCoinAmount());

        return MissionResponse.from(mission);
    }

    @Transactional
    public List<MissionResponse> bulkCreateMissions(Long parentId, Long childId, MissionBulkCreateRequest request) {
        // 1. 부모-자녀 관계 검증
        validateParentChildRelation(parentId, childId);

        // 2. 부모 엔티티 조회
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));

        // 3. 자녀 엔티티 조회
        Child child = childRepository.findById(childId)
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
                savedMissions.size(), parentId, childId);

        return savedMissions.stream()
                .map(MissionResponse::from)
                .toList();
    }

    /*
    데모데이용 임시 메서드
    */
    @Transactional
    public void deleteMissionsByChildIds(List<Long> childIds) {
        missionRepository.deleteByChildIdIn(childIds);
    }
    /*
     */

    /*
    솝트 데모데이 때 더미데이터를 넣기 위한 메서드
    */
    @Transactional
    public void insertDummy(Long parentId, Long childId, String env) {
        String sqlPath = "sql/" + env + "_mission_insert_dummy.sql";
        String sql = loadSql(sqlPath);

        Query missionQuery = em.createNativeQuery(sql);

        missionQuery.setParameter("parentId", parentId);
        missionQuery.setParameter("childId", childId);
        log.info("mission query: " + missionQuery);
        missionQuery.executeUpdate();
    }

    private String loadSql(String path) {
        try {
            Resource resource = resourceLoader.getResource("classpath:" + path);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("더미 SQL 로딩 실패", e);
        }
    }
    /*
     */

    private void validateParentChildRelation(Long parentId, Long childId) {
        if (!parentChildRepository.existsByParentIdAndChildId(parentId, childId)) {
            throw new KieroException(MissionErrorCode.NOT_YOUR_CHILD);
        }
    }
}
