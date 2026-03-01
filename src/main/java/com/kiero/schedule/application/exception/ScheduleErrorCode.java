package com.kiero.schedule.application.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleErrorCode implements BaseCode {
	/*
	400 BAD REQUEST
	 */
	INVALID_DAY_OF_WEEK(HttpStatus.BAD_REQUEST, "형식에 맞지 않는 요일 입력입니다."),
	FIRE_LIT_ALREADY_COMPLETE(HttpStatus.BAD_REQUEST, "오늘의 불 피우기가 이미 완료되었습니다."),
	SCHEDULE_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "해당 일정은 이미 완료되었습니다."),
	SCHEDULE_COULD_NOT_BE_SKIPPED(HttpStatus.BAD_REQUEST, "해당 일정은 다음으로 넘어갈 수 있는 상태가 아닙니다."),
	DAY_OF_WEEK_XOR_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "반복 요일이나 일정 중 한 가지만 입력되어야 합니다."),
	DAY_OF_WEEK_NOT_NULLABLE_WHEN_IS_RECURRING_IS_TRUE(HttpStatus.BAD_REQUEST, "반복 일정일 때, 반복요일이 입력되어야 합니다."),
	DATE_NOT_NULLABLE_WHEN_IS_RECURRING_IS_FALSE(HttpStatus.BAD_REQUEST, "반복 일정이 아닐 때, 일정 일자가 입력되어야 합니다."),
	INVALID_DATE_DURATION(HttpStatus.BAD_REQUEST, "시작 일자가 종료 일자의 이전 시점이어야 합니다."),
	SCHEDULE_DUPLICATED(HttpStatus.BAD_REQUEST, "기존의 일정과 시간이 중복되는 일정은 추가할 수 없습니다."),
	INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "입력된 일자가 형식에 맞지 않습니다."),
	PAST_SCHEDULE_CANNOT_BE_MODIFIED(HttpStatus.BAD_REQUEST, "과거의 일정은 수정될 수 없습니다."),
	DAY_OF_WEEK_CANNOT_BE_MODIFIED(HttpStatus.BAD_REQUEST, "'이후 일정 포함', '이번 일정만' 조건 선택 시 요일은 수정될 수 없습니다."),
	IS_INCLUDE_FOLLOWING_IS_REQUIRED(HttpStatus.BAD_REQUEST, "반복일정을 삭제할 경우, body에 '이후 일정 포함 여부'를 포함해야 합니다."),
	SCHEDULE_CANNOT_BE_DELETED(HttpStatus.BAD_REQUEST, "아이가 이미 넘어가기, 인증 등의 행위를 수행하였거나 일정 시각이 지난 일정은 삭제할 수 없습니다."),
	/*
	403 FORBIDDEN
	 */
	SCHEDULE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 일정에 접근할 권한이 없습니다."),
	NOT_ALLOWED_TO_CHILD(HttpStatus.FORBIDDEN, "자신의 아이에만 접근할 수 있습니다."),

	/*
	404 NOT FOUND
	 */
	SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 일정 데이터를 찾을 수 없습니다."),
	CHILD_NOT_FOUND(HttpStatus.NOT_FOUND, "아이 정보를 찾을 수 없습니다."),
	PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부모 정보를 찾을 수 없습니다."),

	/*
	500 INTERNAL SERVER ERROR
	 */
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "예기치 않은 서버 에러가 발생했습니다."),
	SCHEDULE_UPDATE_CASE_CANNOT_RESOLVED(HttpStatus.INTERNAL_SERVER_ERROR, "일정 업데이트 유형 판단에 실패했습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
