package com.kiero.global.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Health-Check", description = "health check API")
public interface HealthCheckApi {

	@Operation(summary = "health check GET API", description = "서버 현재 상태를 확인하기 위한 GET API입니다. 정상적으로 동작할 경우 'OK' 문자열을 반환합니다.")
	@ApiResponses(
		value = {
			@ApiResponse(responseCode = "200", description = "서버가 정상적으로 동작 중입니다.")
		}
	)
	String healthCheck();
}
