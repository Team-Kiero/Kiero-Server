package com.kiero.schedule.domain;

import com.kiero.schedule.enums.DayOfWeek;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = ScheduleRepeatDaysTableConstants.TABLE_SCHEDULE_REPEAT_DAYS)
public class ScheduleRepeatDays {

	@Id
	@Column(name = ScheduleRepeatDaysTableConstants.COLUMN_ID)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = ScheduleRepeatDaysTableConstants.COLUMN_DAY_OF_WEEK, nullable = false)
	private DayOfWeek dayOfWeek;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = ScheduleRepeatDaysTableConstants.COLUMN_SCHEDULE_ID, nullable = false)
	private Schedule schedule;

	public static ScheduleRepeatDays create(
		DayOfWeek dayOfWeek,
		Schedule schedule
	) {
		return ScheduleRepeatDays.builder()
			.dayOfWeek(dayOfWeek)
			.schedule(schedule)
			.build();
	}
}
