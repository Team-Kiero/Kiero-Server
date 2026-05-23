package com.kiero.global.notification.application.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationSuccessCode implements BaseCode {
	/*
	200 OK
	*/
	FCM_TOKEN_REGISTERED(HttpStatus.OK, "FCM 토큰이 등록되었습니다."),
	NOTIFICATION_SETTINGS_FETCHED(HttpStatus.OK, "알림 설정을 조회했습니다."),
	NOTIFICATION_SETTINGS_UPDATED(HttpStatus.OK, "알림 설정이 변경되었습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
