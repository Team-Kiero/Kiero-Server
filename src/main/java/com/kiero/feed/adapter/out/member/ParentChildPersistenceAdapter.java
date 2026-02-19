package com.kiero.feed.adapter.out.member;

import java.util.List;

import org.springframework.stereotype.Component;

import com.kiero.feed.application.port.out.ParentChildAccessPort;
import com.kiero.feed.application.port.out.ParentChildQueryPort;
import com.kiero.parent.adapter.out.persistence.ParentChildRepository;
import com.kiero.parent.domain.Parent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ParentChildPersistenceAdapter implements ParentChildAccessPort, ParentChildQueryPort {

	private final ParentChildRepository parentChildRepository;

	@Override
	public boolean existsByParentIdAndChildId(Long parentId, Long childId) {
		return parentChildRepository.existsByParentIdAndChildId(parentId, childId);
	}

	@Override
	public List<Parent> findParentsByChildId(Long childId) {
		return parentChildRepository.findParentsByChildId(childId);
	}
}