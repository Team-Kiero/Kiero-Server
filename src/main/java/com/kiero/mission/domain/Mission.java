package com.kiero.mission.domain;

import java.time.LocalDate;

import com.kiero.child.domain.Child;
import com.kiero.global.entity.BaseTimeEntity;
import com.kiero.parent.domain.Parent;

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
@Table(name = MissionTableConstants.TABLE_MISSION)
public class Mission extends BaseTimeEntity {

	@Id
	@Column(name = MissionTableConstants.COLUMN_ID)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = MissionTableConstants.COLUMN_NAME, nullable = false)
	private String name;

	@Column(name = MissionTableConstants.COLUMN_REWARD, nullable = false)
	private int reward;

	@Column(name = MissionTableConstants.COLUMN_DUE_AT, nullable = false)
	private LocalDate dueAt;

	@Column(name = MissionTableConstants.COLUMN_IS_COMPLETED, nullable = false)
	private boolean isCompleted;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = MissionTableConstants.COLUMN_PARENT_ID, nullable = false)
	private Parent parent;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = MissionTableConstants.COLUMN_CHILD_ID, nullable = false)
	private Child child;

	public static Mission create(Parent parent, Child child, String name, int reward, LocalDate dueAt) {
		return Mission.builder()
			.parent(parent)
			.child(child)
			.name(name)
			.reward(reward)
			.dueAt(dueAt)
			.isCompleted(false)
			.build();
	}
}