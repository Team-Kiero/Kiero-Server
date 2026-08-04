package com.kiero.parent.application.dto;

public record ParentMeResponse(
	Long id,
	String image,
	String name,
	boolean hasPendingChildSession,
	boolean pushNotificationEnabled
) {
}