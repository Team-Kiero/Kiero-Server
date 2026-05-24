package com.kiero.parent.application.dto;

public record ParentMeResponse(
	String image,
	String name,
	boolean hasPendingChildSession,
	boolean pushNotificationEnabled
) {
}