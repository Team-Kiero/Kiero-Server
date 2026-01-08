package com.kiero.schedule.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kiero.schedule.domain.enums.ScheduleStatus;
import com.kiero.schedule.domain.enums.StoneType;

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
@Table(name = ScheduleDetailTableConstants.TABLE_SCHEDULE_DETAIL)
public class ScheduleDetail {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = ScheduleDetailTableConstants.COLUMN_ID)
	private Long id;

	@Column(name = ScheduleDetailTableConstants.COLUMN_DATE, nullable = false)
	private LocalDate date;

	@Column(name = ScheduleDetailTableConstants.COLUMN_IMAGE_URL, nullable = true)
	private String imageUrl;

	@Column(name = ScheduleDetailTableConstants.COLUMN_STONE_USED_AT, nullable = true)
	private LocalDateTime stoneUsedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = ScheduleDetailTableConstants.COLUMN_SCHEDULE_STATUS, nullable = false)
	private ScheduleStatus scheduleStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = ScheduleDetailTableConstants.COLUMN_STONE_TYPE, nullable = true)
	private StoneType stoneType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = ScheduleDetailTableConstants.COLUMN_SCHEDULE_ID, nullable = false)
	private Schedule schedule;

	public static ScheduleDetail create(
		LocalDate date,
		String imageUrl,
		LocalDateTime stoneUsedAt,
		ScheduleStatus scheduleStatus,
		StoneType stoneType,
		Schedule schedule
	) {
		return ScheduleDetail.builder()
			.date(date)
			.imageUrl(imageUrl)
			.stoneUsedAt(stoneUsedAt)
			.scheduleStatus(scheduleStatus)
			.stoneType(stoneType)
			.schedule(schedule)
			.build();
	}

	public void changeScheduleStatus(ScheduleStatus scheduleStatus) {
		this.scheduleStatus = scheduleStatus;
	}

	public void changeStoneType(StoneType stoneType) {
		this.stoneType = stoneType;
	}

}
