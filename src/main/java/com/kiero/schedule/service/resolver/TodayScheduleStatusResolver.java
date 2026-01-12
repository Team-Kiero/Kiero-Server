package com.kiero.schedule.service.resolver;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.kiero.schedule.domain.ScheduleDetail;
import com.kiero.schedule.domain.enums.ScheduleStatus;
import com.kiero.schedule.domain.enums.TodayScheduleStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TodayScheduleStatusResolver {
	private TodayScheduleStatusResolver() {
	}

	public static TodayScheduleStatus resolve(
		ScheduleDetail todoScheduleDetail,
		List<ScheduleDetail> filteredAllScheduleDetails,
		LocalDateTime earliestStoneUsedAt
	) {
		int totalSchedule = filteredAllScheduleDetails == null ? 0 : filteredAllScheduleDetails.size();

		// 오늘의 일정 자체가 존재하지 않을 때
		if (totalSchedule == 0 && todoScheduleDetail == null) {
			return TodayScheduleStatus.NO_SCHEDULE;
		}

		if (filteredAllScheduleDetails != null && todoScheduleDetail != null) {
			// 첫번째 일정일 때
			int index = filteredAllScheduleDetails.indexOf(todoScheduleDetail);
			int order = index + 1;

			if (order == 1) {
				return TodayScheduleStatus.FIRST_SCHEDULE;
			}

			LocalTime now = LocalTime.now();
			LocalTime start = todoScheduleDetail.getSchedule().getStartTime();
			LocalTime end = todoScheduleDetail.getSchedule().getEndTime();
			ScheduleStatus status = todoScheduleDetail.getScheduleStatus();

			// 현재 일정을 완료하고, 다음 일정이 존재할 때
			if (now.isBefore(start) && status == ScheduleStatus.PENDING) {
				return TodayScheduleStatus.NEXT_SCHEDULE_EXIST;
			}

			// 현재 진행해야 하는 일정이 있을 때
			if (!now.isBefore(start) && now.isBefore(end)) {
				return TodayScheduleStatus.NOW_SCHEDULE_EXIST;
			}
		}

		if (filteredAllScheduleDetails != null) {
			int passedScheduleCount = (int)filteredAllScheduleDetails.stream()
				.filter(sd -> sd.getScheduleStatus() != ScheduleStatus.PENDING)
				.count();

			// 일정을 모두 완료하고, 불피우기는 진행하지 않았을 때
			if (passedScheduleCount == totalSchedule && earliestStoneUsedAt == null) {
				log.info("totalSchedule" + totalSchedule + "passedScheduleCount" + passedScheduleCount);
				return TodayScheduleStatus.FIRE_NOT_LIT;
			}

			// 일정을 모두 완료하고, 불피우기까지 완료했을 때
			if (earliestStoneUsedAt != null) {
				return TodayScheduleStatus.FIRE_LIT;
			}
		}

		return TodayScheduleStatus.NEXT_SCHEDULE_EXIST;
	}
}
