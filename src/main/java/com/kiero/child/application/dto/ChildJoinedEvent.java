package com.kiero.child.application.dto;

public record ChildJoinedEvent(
	Long parentId,
	Long childId
) {
}
