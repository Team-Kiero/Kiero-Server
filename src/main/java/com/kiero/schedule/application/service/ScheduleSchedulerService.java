package com.kiero.schedule.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.parent.application.port.out.ParentChildLoadPort;
import com.kiero.parent.domain.Parent;
import com.kiero.schedule.application.dto.ScheduleStatusUpdatedEvent;
import com.kiero.schedule.application.port.in.ScheduleSchedulerUseCase;
import com.kiero.schedule.application.port.out.DiscardedSchedulePersistencePort;
import com.kiero.schedule.application.port.out.ScheduleDetailPersistencePort;
import com.kiero.schedule.application.port.out.ScheduleEventPort;
import com.kiero.schedule.application.port.out.ScheduleRepeatDaysPersistencePort;
import com.kiero.schedule.domain.Schedule;
import com.kiero.schedule.domain.ScheduleDetail;
import com.kiero.schedule.domain.enums.DayOfWeek;
import com.kiero.schedule.domain.enums.ScheduleStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduleSchedulerService implements ScheduleSchedulerUseCase {

	private final Clock clock;

	private final ScheduleRepeatDaysPersistencePort scheduleRepeatDaysPersistencePort;
	private final DiscardedSchedulePersistencePort discardedSchedulePersistencePort;
	private final ScheduleDetailPersistencePort scheduleDetailPersistencePort;
	private final ScheduleEventPort scheduleEventPort;

	private final ScheduleCommandService scheduleCommandService;
	private final ParentChildLoadPort parentChildLoadPort;

	@Override
	@Transactional
	public void createTodayScheduleDetail() {
		LocalDate today = LocalDate.now(clock);
		DayOfWeek customDayOfWeek = DayOfWeek.from(today.getDayOfWeek());

		List<Schedule> schedules = scheduleRepeatDaysPersistencePort.findSchedulesToCreateTodayDetail(customDayOfWeek, today);

		Set<Long> discardedSchedules = discardedSchedulePersistencePort.findAllByDate(today).stream()
			.map(ds -> ds.getSchedule().getId())
			.collect(Collectors.toSet());

		List<ScheduleDetail> scheduleDetails = schedules.stream()
			.filter(schedule -> !discardedSchedules.contains(schedule.getId()))
			.map(schedule -> ScheduleDetail.create(today, null, null, ScheduleStatus.PENDING, null, schedule))
			.toList();

		scheduleDetailPersistencePort.saveAll(scheduleDetails);

		List<ScheduleDetail> allScheduleDetails = scheduleDetailPersistencePort.findAllByDate(today);

		scheduleCommandService.calculateStoneTypePerScheduleDetail(allScheduleDetails);
	}

	@Override
	@Transactional
	public void bulkMarkAndPushEventIfUpdateExists(LocalDate today, LocalTime now) {
		List<Long> childIds = scheduleDetailPersistencePort.findChildIdsToMark(today, now);

		if (!childIds.isEmpty()) {
			scheduleDetailPersistencePort.bulkMarkPendingAsFailed(today, now);
			scheduleDetailPersistencePort.bulkMarkVerifiedAsCompleted(today, now);

			for (Long childId : childIds) {

				List<Long> parentIds = parentChildLoadPort.findParentsByChildId(childId).stream()
					.map(Parent::getId)
					.toList();

				scheduleEventPort.publish(new ScheduleStatusUpdatedEvent(childId, parentIds));
			}
		}
	}
}
