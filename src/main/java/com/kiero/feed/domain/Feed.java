package com.kiero.feed.domain;

import java.time.LocalDate;

import com.kiero.child.domain.Child;
import com.kiero.feed.domain.enums.EventType;
import com.kiero.global.entity.BaseTimeEntity;
import com.kiero.parent.domain.Parent;

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
@Table(name = FeedTableConstants.TABLE_FEED)
public class Feed extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = FeedTableConstants.COLUMN_ID)
	private Long id;

	@Column(name = FeedTableConstants.COLUMN_DATE, nullable = false)
	private LocalDate date;

	@Enumerated(EnumType.STRING)
	@Column(name = FeedTableConstants.COLUMN_EVENT_TYPE, nullable = false)
	private EventType eventType;

	@Column(name = FeedTableConstants.COLUMN_METADATA, columnDefinition = "json", nullable = false)
	private String metadata;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = FeedTableConstants.COLUMN_PARENT_ID, nullable = false)
	private Parent parent;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = FeedTableConstants.COLUMN_CHILD_ID, nullable = false)
	private Child child;

	public static Feed create(
		Parent parent,
		Child child,
		LocalDate date,
		EventType eventType,
		String metadata
	) {
		return Feed.builder()
			.parent(parent)
			.child(child)
			.date(date)
			.eventType(eventType)
			.metadata(metadata)
			.build();
	}
}