package com.kiero.parent.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.application.port.out.ChildLoadPort;
import com.kiero.child.domain.Child;
import com.kiero.global.exception.KieroException;
import com.kiero.mission.application.port.in.MissionQueryUseCase;
import com.kiero.parent.application.dto.TodayProgressForParentResponse;
import com.kiero.parent.application.exception.ParentErrorCode;
import com.kiero.schedule.application.port.in.ScheduleQueryUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleMissionFacade {

	private final ScheduleQueryUseCase scheduleQueryUseCase;
	private final MissionQueryUseCase missionQueryUseCase;
	private final ChildLoadPort childLoadPort;

	public TodayProgressForParentResponse getProgress(Long parentId, Long childId) {

		Child child = childLoadPort.findById(childId)
			.orElseThrow(() -> new KieroException(ParentErrorCode.CHILD_NOT_FOUND));

		var schedulePart = scheduleQueryUseCase.getScheduleTodayProgressForParent(parentId, childId);
		var missionPart = missionQueryUseCase.getMissionTodayProgressForParent(childId);

		return new TodayProgressForParentResponse(
			child.getFirstName(),
			schedulePart.isFireLitToday(),
			missionPart.completeMissions(),
			missionPart.incompleteMissions(),
			schedulePart.schedules()
		);
	}
}
