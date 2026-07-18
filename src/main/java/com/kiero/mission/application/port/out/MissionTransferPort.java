package com.kiero.mission.application.port.out;

public interface MissionTransferPort {
	void transferOwnership(Long fromParentId, Long toParentId, Long childId);
}
