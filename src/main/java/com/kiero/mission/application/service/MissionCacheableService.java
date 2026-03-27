package com.kiero.mission.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.mission.application.dto.MissionResponse;
import com.kiero.mission.application.port.out.MissionPersistencePort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MissionCacheableService {

	private final MissionPersistencePort missionPort;
	private final Clock clock;

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = "missions", key = "#root.target.cacheKey(#childId)", condition = "#childId != null")
	public List<MissionResponse> getMissionsByChild(Long childId) {
		LocalDate today = LocalDate.now(clock);
		return missionPort.findAllByChildIdAndDueAtGreaterThanEqual(childId, today)
			.stream().map(MissionResponse::from).toList();
	}

	public String cacheKey(Long childId) {
		return childId + ":" + LocalDate.now(clock);
	}
}
