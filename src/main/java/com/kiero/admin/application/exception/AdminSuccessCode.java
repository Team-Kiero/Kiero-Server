package com.kiero.admin.application.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminSuccessCode implements BaseCode {

	/*
	200 OK
	 */
	LOGIN_SUCCESS(HttpStatus.OK, "관리자 로그인에 성공하였습니다."),
	GET_PARENTS_SUCCESS(HttpStatus.OK, "부모 목록 조회에 성공하였습니다."),
	GET_PARENT_SUCCESS(HttpStatus.OK, "부모 상세 조회에 성공하였습니다."),
	DELETE_PARENT_SUCCESS(HttpStatus.OK, "부모 삭제에 성공하였습니다."),
	GET_CHILDREN_SUCCESS(HttpStatus.OK, "아이 목록 조회에 성공하였습니다."),
	GET_CHILD_SUCCESS(HttpStatus.OK, "아이 상세 조회에 성공하였습니다."),
	DELETE_CHILD_SUCCESS(HttpStatus.OK, "아이 삭제에 성공하였습니다."),
	GET_MISSIONS_SUCCESS(HttpStatus.OK, "미션 목록 조회에 성공하였습니다."),
	UPDATE_MISSION_SUCCESS(HttpStatus.OK, "미션 수정에 성공하였습니다."),
	DELETE_MISSION_SUCCESS(HttpStatus.OK, "미션 삭제에 성공하였습니다."),
	GET_SCHEDULES_SUCCESS(HttpStatus.OK, "일정 목록 조회에 성공하였습니다."),
	DELETE_SCHEDULE_SUCCESS(HttpStatus.OK, "일정 삭제에 성공하였습니다."),
	GET_COUPONS_SUCCESS(HttpStatus.OK, "쿠폰 목록 조회에 성공하였습니다."),
	UPDATE_COUPON_SUCCESS(HttpStatus.OK, "쿠폰 수정에 성공하였습니다."),
	DELETE_COUPON_SUCCESS(HttpStatus.OK, "쿠폰 삭제에 성공하였습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
