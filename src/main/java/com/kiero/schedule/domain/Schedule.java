package com.kiero.schedule.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kiero.child.domain.Child;
import com.kiero.global.entity.BaseTimeEntity;
import com.kiero.parent.domain.Parent;
import com.kiero.schedule.enums.StoneType;

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
	private LocalDateTime startAt;

	@Column(name = ScheduleTableConstants.COLUMN_END_AT, nullable = false)
	private LocalDateTime endAt;

	@Column(name = ScheduleTableConstants.COLUMN_DATE, nullable = false)
	private LocalDate date;

	@Column(name = ScheduleTableConstants.COLUMN_NAME, nullable = false)
	private String name;

	@Column(name = ScheduleTableConstants.COLUMN_COLOR_CODE, nullable = false)
	private String colorCode;

	@Column(name = ScheduleTableConstants.COLUMN_IS_VERIFIED, nullable = false)
	private Boolean isVerified;

	@Column(name = ScheduleTableConstants.COLUMN_IMAGE_URL, nullable = true)
	private String imageUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = ScheduleTableConstants.COLUMN_STONE_TYPE, nullable = false)
	private StoneType stoneType;

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
		LocalDate date,
		LocalDateTime startAt,
		LocalDateTime endAt,
		String colorCode,
		StoneType stoneType
	) {
		return Schedule.builder()
			.parent(parent)
			.child(child)
			.name(name)
			.date(date)
			.startAt(startAt)
			.endAt(endAt)
			.colorCode(colorCode)
			.stoneType(stoneType)
			.isVerified(false)
			.build();
	}
}