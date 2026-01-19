package com.kiero.global.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kiero.child.service.ChildService;
import com.kiero.feed.service.FeedService;
import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.mission.service.MissionService;
import com.kiero.parent.service.ParentService;
import com.kiero.schedule.service.ScheduleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/dummy")
@RestController
public class DummyDataInsertController {

	private final ScheduleService scheduleService;
	private final MissionService missionService;
	private final ParentService parentService;
	private final FeedService feedService;
	private final ChildService childService;

	// 아이의 '여정 시작하기'버튼 클릭 시 추가로 호출되는 api
	@PostMapping()
	ResponseEntity<Void> insertDummyData(
		@CurrentMember CurrentAuth currentAuth,
		@RequestParam String env
	) {
		Long childId = currentAuth.memberId();
		List<Long> parentIds = parentService.findParentIdByChildId(childId);
		log.info("parentId = {}의 더미데이터 처리를 시작합니다. (ง •̀ω•́)ง✧", childId);

		missionService.insertDummy(parentIds, childId, env);
		feedService.insertDummy(parentIds, childId, env);
		scheduleService.insertDummy(parentIds, childId);

		return ResponseEntity.ok()
			.body(null);
	}

	// 부모의 '로그아웃'버튼 클릭 시 추가로 호출되는 api
	@DeleteMapping
	ResponseEntity<Void> deleteChildDataByParent(
		@CurrentMember CurrentAuth currentAuth
	) {
		Long parentId = currentAuth.memberId();
		log.info("parentId = {}에 연결된 child 정보, 관련 데이터를 삭제합니다. (ง •̀ω•́)ง✧", parentId);

		List<Long> childIds = parentService.getMyChildIds(parentId);
		scheduleService.deleteSchedulesDataByParentAndChild(childIds);
		feedService.deleteFeedsByChildIds(childIds);
		missionService.deleteMissionsByChildIds(childIds);
		parentService.deleteParentChildByChildIds(childIds);

		childService.deleteChildByIds(childIds);

		return ResponseEntity.ok()
			.body(null);
	}
}
