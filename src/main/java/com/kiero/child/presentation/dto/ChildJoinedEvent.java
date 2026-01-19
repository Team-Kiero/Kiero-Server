package com.kiero.child.presentation.dto;

import java.time.LocalDateTime;

public record ChildJoinedEvent(
	Long parentId,
	Long childId,
	String childName,
	LocalDateTime occurredAt
) {
}
