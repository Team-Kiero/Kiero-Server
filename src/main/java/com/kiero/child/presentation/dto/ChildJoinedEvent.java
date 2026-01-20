package com.kiero.child.presentation.dto;

public record ChildJoinedEvent(
	Long parentId,
	Long childId
) {
}
