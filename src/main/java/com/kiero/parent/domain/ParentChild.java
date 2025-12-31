package com.kiero.parent.domain;

import com.kiero.child.domain.Child;
import com.kiero.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(
	name = ParentChildTableConstants.TABLE_PARENT_CHILD,
	uniqueConstraints = @UniqueConstraint(
		name = "uk_parent_child",
		columnNames = {ParentChildTableConstants.COLUMN_PARENT_ID, ParentChildTableConstants.COLUMN_CHILD_ID}
	)
)
public class ParentChild extends BaseTimeEntity {

	@Id
	@Column(name = ParentChildTableConstants.COLUMN_ID)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = ParentChildTableConstants.COLUMN_PARENT_ID, nullable = false)
	private Parent parent;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = ParentChildTableConstants.COLUMN_CHILD_ID, nullable = false)
	private Child child;

	public static ParentChild create(final Parent parent, final Child child) {
		return ParentChild.builder()
			.parent(parent)
			.child(child)
			.build();
	}
}
