package com.kiero.admin.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.admin.application.dto.AdminScheduleResponse;
import com.kiero.admin.application.exception.AdminErrorCode;
import com.kiero.admin.application.port.in.AdminScheduleUseCase;
import com.kiero.admin.application.port.out.AdminDeletePort;
import com.kiero.admin.application.port.out.AdminScheduleLoadPort;
import com.kiero.global.exception.KieroException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminScheduleService implements AdminScheduleUseCase {

	private final AdminScheduleLoadPort adminScheduleLoadPort;
	private final AdminDeletePort adminDeletePort;

	@Override
	@Transactional(readOnly = true)
	public List<AdminScheduleResponse> findAllByChildId(Long childId) {
		return adminScheduleLoadPort.findAllByChildId(childId)
			.stream()
			.map(AdminScheduleResponse::from)
			.toList();
	}

	@Override
	@Transactional
	public void deleteSchedule(Long scheduleId) {
		adminScheduleLoadPort.findById(scheduleId)
			.orElseThrow(() -> new KieroException(AdminErrorCode.SCHEDULE_NOT_FOUND));

		adminDeletePort.deleteScheduleById(scheduleId);
	}
}
