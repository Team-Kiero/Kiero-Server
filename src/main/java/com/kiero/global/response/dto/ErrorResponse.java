package com.kiero.global.response.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
	int status,
	String message,
    String detail
) {

	public static ErrorResponse of(BaseCode baseCode) {
		return new ErrorResponse(baseCode.getHttpStatus().value(), baseCode.getMessage(), null);
	}

	public static ErrorResponse of(HttpStatus httpStatus, String message) { //메시지 추가 커스텀
		return new ErrorResponse(httpStatus.value(), message, null);
	}

	public static ErrorResponse of(BaseCode baseCode, Object detail) { //디테일 추가 커스텀
		return new ErrorResponse(
                baseCode.getHttpStatus().value(),
                baseCode.getMessage(),
                detail != null ? detail.toString() : null
		);
	}
}