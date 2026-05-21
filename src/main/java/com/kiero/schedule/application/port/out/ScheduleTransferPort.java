package com.kiero.schedule.application.port.out;

public interface ScheduleTransferPort {
	void transferOwnership(Long fromParentId, Long toParentId, Long childId);
}
