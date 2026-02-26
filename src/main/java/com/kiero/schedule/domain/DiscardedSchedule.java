package com.kiero.schedule.domain;

import java.time.LocalDate;

import com.kiero.schedule.domain.enums.DayOfWeek;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = DiscardedScheduleTableConstants.TABLE_DISCARDED_SCHEDULE)
public class DiscardedSchedule {

	@Id
	@Column(name = DiscardedScheduleTableConstants.COLUMN_ID)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;

	@Column(name = DiscardedScheduleTableConstants.COLUMN_DATE, nullable = false)
	LocalDate date;

	@Column(name = DiscardedScheduleTableConstants.COLUMN_DAY_OF_WEEK, nullable = false)
	DayOfWeek dayOfWeek;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = DiscardedScheduleTableConstants.COLUMN_SCHEDULE_ID, nullable = false)
	private Schedule schedule;

	public static DiscardedSchedule create(
		LocalDate date,
		Schedule schedule
	) {
		return DiscardedSchedule.builder()
			.date(date)
			.schedule(schedule)
			.dayOfWeek(DayOfWeek.from(date.getDayOfWeek()))
			.build();
	}
}
