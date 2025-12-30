package com.kiero.global.response.dto;

import com.kiero.global.response.base.BaseCode;

public record SuccessResponse<T>(
	int status,
	String message,
	T data
) {

	public static <T> SuccessResponse<T> of(BaseCode basecode) { //반환 데이터 없음
		return new SuccessResponse<>(basecode.getHttpStatus().value(), basecode.getMessage(), null);
	}

	public static <T> SuccessResponse<T> of(BaseCode basecode, String message) { //반환 데이터 없음, 메시지 커스텀
		return new SuccessResponse<>(basecode.getHttpStatus().value(), message, null);
	}

	public static <T> SuccessResponse<T> of(BaseCode basecode, T data) { //반환 데이터 있음
		return new SuccessResponse<>(basecode.getHttpStatus().value(), basecode.getMessage(), data);
	}

	public static <T> SuccessResponse<T> of(BaseCode basecode, String message,
		T data) { //반환 데이터 있음, 메시지 커스텀
		return new SuccessResponse<>(basecode.getHttpStatus().value(), message, data);
	}
}
