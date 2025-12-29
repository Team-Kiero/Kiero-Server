package com.kiero.global.response.dto;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

public record ErrorResponse(
	int status,
	String message
) {

	public static ErrorResponse of(BaseCode baseCode) {
		return new ErrorResponse(baseCode.getHttpStatus().value(), baseCode.getMessage());
	}

	public static ErrorResponse of(HttpStatus httpStatus, String message) { //메시지 추가 커스텀
		return new ErrorResponse(httpStatus.value(), message);
	}

	public static ErrorResponse of(BaseCode baseCode, Object detail) { //디테일 추가 커스텀
		return new ErrorResponse(baseCode.getHttpStatus().value(),
			baseCode.getMessage() + (detail != null ? ": " + detail : "")
		);
	}
}