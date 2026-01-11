package com.kiero.schedule.domain;

import java.time.LocalTime;

import com.kiero.child.domain.Child;
import com.kiero.global.entity.BaseTimeEntity;
import com.kiero.parent.domain.Parent;
import com.kiero.schedule.domain.enums.ScheduleColor;

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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = ScheduleTableConstants.TABLE_SCHEDULE)
public class Schedule extends BaseTimeEntity {

	@Id
	@Column(name = ScheduleTableConstants.COLUMN_ID)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = ScheduleTableConstants.COLUMN_START_AT, nullable = false)
	private LocalTime startTime;

	@Column(name = ScheduleTableConstants.COLUMN_END_AT, nullable = false)
	private LocalTime endTime;

	@Column(name = ScheduleTableConstants.COLUMN_NAME, nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = ScheduleTableConstants.COLUMN_SCHEDULE_COLOR, nullable = false)
	private ScheduleColor scheduleColor;

	@Column(name = ScheduleTableConstants.COLUMN_IS_RECURRING, nullable = false)
	private boolean isRecurring;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = ScheduleTableConstants.COLUMN_PARENT_ID, nullable = false)
	private Parent parent;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = ScheduleTableConstants.COLUMN_CHILD_ID, nullable = false)
	private Child child;

	public static Schedule create(
		Parent parent,
		Child child,
		String name,
		LocalTime startTime,
		LocalTime endTime,
		ScheduleColor scheduleColor,
		boolean isRecurring
	) {
		return Schedule.builder()
			.parent(parent)
			.child(child)
			.name(name)
			.startTime(startTime)
			.endTime(endTime)
			.scheduleColor(scheduleColor)
			.isRecurring(isRecurring)
			.build();
	}
}