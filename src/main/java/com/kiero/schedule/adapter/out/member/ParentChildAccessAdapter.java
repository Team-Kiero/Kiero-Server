package com.kiero.schedule.adapter.out.member;

import org.springframework.stereotype.Component;

import com.kiero.parent.adapter.out.persistence.ParentChildRepository;
import com.kiero.schedule.application.port.out.ParentChildAccessPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ParentChildAccessAdapter implements ParentChildAccessPort {

	private final ParentChildRepository parentChildRepository;

	@Override
	public boolean existsByParentIdAndChildId(Long parentId, Long childId) {
		return parentChildRepository.existsByParentIdAndChildId(parentId, childId);
	}
}