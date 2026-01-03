package com.kiero.schedule.domain;

import java.time.LocalDate;

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
@Table(name = ScheduleDetailTableConstants.TABLE_SCHEDULE_DETAIL)
public class ScheduleDetail {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = ScheduleDetailTableConstants.COLUMN_ID)
	private Long id;

	@Column(name = ScheduleDetailTableConstants.COLUMN_DATE, nullable = false)
	private LocalDate date;

	@Column(name = ScheduleDetailTableConstants.COLUMN_IS_VERIFIED, nullable = false)
	private boolean isVerified;

	@Column(name = ScheduleDetailTableConstants.COLUMN_IMAGE_URL, nullable = true)
	private String imageUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = ScheduleDetailTableConstants.COLUMN_STONE_TYPE, nullable = true)
	private StoneType stoneType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = ScheduleDetailTableConstants.COLUMN_SCHEDULE_ID, nullable = false)
	private Schedule schedule;

	public static ScheduleDetail create(
		LocalDate date,
		boolean isVerified,
		String imageUrl,
		StoneType stoneType,
		Schedule schedule
	) {
		return ScheduleDetail.builder()
			.date(date)
			.isVerified(isVerified)
			.imageUrl(imageUrl)
			.stoneType(stoneType)
			.schedule(schedule)
			.build();
	}

}
