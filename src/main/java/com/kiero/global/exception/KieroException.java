package com.kiero.global.exception;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;

@Getter
public class KieroException extends RuntimeException {
	private final BaseCode baseCode;

	public KieroException(BaseCode baseCode) {
		super(baseCode.getMessage());
		this.baseCode = baseCode;
	}
}
