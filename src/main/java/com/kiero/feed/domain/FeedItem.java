package com.kiero.feed.domain;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.kiero.child.domain.Child;
import com.kiero.feed.domain.enums.EventType;
import com.kiero.feed.infrastructure.converter.JsonNodeConverter;
import com.kiero.global.entity.BaseTimeEntity;
import com.kiero.parent.domain.Parent;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
@Table(name = FeedItemTableConstants.TABLE_FEED_ITEM)
public class FeedItem extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = FeedItemTableConstants.COLUMN_ID)
	private Long id;

	@Column(name = FeedItemTableConstants.COLUMN_OCCURRED_AT, nullable = false)
	private LocalDateTime occurredAt;

	@Enumerated(EnumType.STRING)
	@Column(name = FeedItemTableConstants.COLUMN_EVENT_TYPE, nullable = false)
	private EventType eventType;

	@Convert(converter = JsonNodeConverter.class)
	@Column(name = FeedItemTableConstants.COLUMN_METADATA, columnDefinition = "json", nullable = false)
	private JsonNode metadata;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = FeedItemTableConstants.COLUMN_PARENT_ID, nullable = false)
	private Parent parent;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = FeedItemTableConstants.COLUMN_CHILD_ID, nullable = false)
	private Child child;

	public static FeedItem create(
		Parent parent,
		Child child,
		LocalDateTime occurredAt,
		EventType eventType,
		JsonNode metadata
	) {
		return FeedItem.builder()
			.parent(parent)
			.child(child)
			.occurredAt(occurredAt)
			.eventType(eventType)
			.metadata(metadata)
			.build();
	}
}