package com.kiero.schedule.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.global.notification.application.port.in.PushNotificationUseCase;
import com.kiero.global.notification.domain.PushNotificationType;
import com.kiero.mission.application.port.out.MissionPersistencePort;
import com.kiero.parent.application.port.out.ParentChildLoadPort;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.domain.ParentChild;
import com.kiero.schedule.application.dto.ScheduleEventTarget;
import com.kiero.schedule.application.dto.ScheduleStatusUpdatedEvent;
import com.kiero.schedule.application.port.in.ScheduleSchedulerUseCase;
import com.kiero.schedule.application.port.out.DiscardedSchedulePersistencePort;
import com.kiero.schedule.application.port.out.ScheduleDetailPersistencePort;
import com.kiero.schedule.application.port.out.ScheduleEventPort;
import com.kiero.schedule.application.port.out.SchedulePersistencePort;
import com.kiero.schedule.application.port.out.ScheduleRepeatDaysPersistencePort;
import com.kiero.schedule.domain.Schedule;
import com.kiero.schedule.domain.ScheduleDetail;
import com.kiero.schedule.domain.enums.DayOfWeek;
import com.kiero.schedule.domain.enums.ScheduleStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleSchedulerService implements ScheduleSchedulerUseCase {

	private final Clock clock;

	private final ScheduleRepeatDaysPersistencePort scheduleRepeatDaysPersistencePort;
	private final DiscardedSchedulePersistencePort discardedSchedulePersistencePort;
	private final ScheduleDetailPersistencePort scheduleDetailPersistencePort;
	private final ScheduleEventPort scheduleEventPort;
	private final PushNotificationUseCase pushNotificationUseCase;
	private final MissionPersistencePort missionPersistencePort;
	private final ParentChildLoadPort parentChildLoadPort;

	private final ScheduleCommandService scheduleCommandService;
	private final SchedulePersistencePort schedulePersistencePort;

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
		List<ScheduleEventTarget> targets = scheduleDetailPersistencePort.findScheduleUpdateEventTarget(today, now);

		if (!targets.isEmpty()) {
			scheduleDetailPersistencePort.bulkMarkPendingAsFailed(today, now);
			scheduleDetailPersistencePort.bulkMarkVerifiedAsCompleted(today, now);

			Map<Long, List<Long>> parentIdsByChildId = targets.stream()
				.collect(Collectors.groupingBy(
					ScheduleEventTarget::childId,
					Collectors.mapping(ScheduleEventTarget::parentId, Collectors.toList())
				));

			for (var entry : parentIdsByChildId.entrySet()) {
				scheduleEventPort.publish(new ScheduleStatusUpdatedEvent(entry.getKey(), entry.getValue()));
			}
		}
	}

	@Override
	@Transactional
	public void pushEventIfScheduleStart(LocalDate today, LocalTime now) {
		List<ScheduleEventTarget> targets = scheduleDetailPersistencePort.findScheduleStartEventTarget(today, now);

		if (!targets.isEmpty()) {
			Map<Long, List<Long>> parentIdsByChildId = targets.stream()
				.collect(Collectors.groupingBy(
					ScheduleEventTarget::childId,
					Collectors.mapping(ScheduleEventTarget::parentId, Collectors.toList())
				));

			for (var entry : parentIdsByChildId.entrySet()) {
				scheduleEventPort.publish(new ScheduleStatusUpdatedEvent(entry.getKey(), entry.getValue()));
			}
		}
	}

	@Override
	@Transactional
	public void deleteObsoleteNonRecurringSchedules() {
		schedulePersistencePort.deleteObsoleteNonRecurringSchedules();
	}

	@Override
	@Transactional(readOnly = true)
	public void sendDailyStartNotifications(LocalDate today) {
		List<ParentChild> allPairs = parentChildLoadPort.findAll();
		for (ParentChild pc : allPairs) {
			pushNotificationUseCase.pushToParent(
				pc.getParent().getId(), pc.getChild().getId(), PushNotificationType.PARENT_DAILY_START, "");
		}

		Set<Long> childIds = new HashSet<>(scheduleDetailPersistencePort.findDistinctChildIdsByDate(today));
		childIds.addAll(missionPersistencePort.findDistinctChildIdsByDate(today));

		for (Long childId : childIds) {
			pushNotificationUseCase.pushToChild(childId, PushNotificationType.CHILD_DAILY_START);
		}
	}

	@Override
	@Transactional
	public void sendNextJourneyNotifications(LocalDate today, LocalTime now) {
		LocalTime targetStart = now.plusMinutes(10).withSecond(0).withNano(0);
		LocalTime targetEnd = targetStart.plusMinutes(1);

		// 08:00~08:15 시작 여정은 하루 시작 알림과 중복 → 스킵
		if (targetStart.isBefore(LocalTime.of(8, 15))) return;

		List<ScheduleDetail> targets = scheduleDetailPersistencePort.findPendingByStartTimeWindow(today, targetStart, targetEnd);

		for (ScheduleDetail sd : targets) {
			// 당일 여정 추가/변경 후 5분 이내 → 스킵 (createdAt 기준)
			if (sd.getCreatedAt() != null &&
				sd.getCreatedAt().isAfter(LocalDateTime.now(clock).minusMinutes(5))) {
				continue;
			}
			pushNotificationUseCase.pushToChild(sd.getSchedule().getChild().getId(), PushNotificationType.CHILD_NEXT_JOURNEY);
		}
	}

	@Override
	@Transactional
	public void sendParentReminderNotifications(LocalDate today, LocalTime now) {
		List<ScheduleDetail> targets = scheduleDetailPersistencePort.findPendingPastEndTimeWithoutReminder(today, now);

		for (ScheduleDetail sd : targets) {
			Long childId = sd.getSchedule().getChild().getId();
			List<Parent> parents = parentChildLoadPort.findParentsByChildId(childId);

			for (Parent parent : parents) {
				pushNotificationUseCase.pushToParent(parent.getId(), childId, PushNotificationType.PARENT_SCHEDULE_REMINDER, "");
			}

			sd.markParentReminderSent(LocalDateTime.now(clock));
			scheduleDetailPersistencePort.save(sd);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void sendMissionIncompleteNotifications(LocalDate today) {
		List<Long> childIds = missionPersistencePort.findChildIdsWithIncompleteMissionsByDate(today);
		for (Long childId : childIds) {
			pushNotificationUseCase.pushToChild(childId, PushNotificationType.CHILD_MISSION_INCOMPLETE);
		}
	}

}
