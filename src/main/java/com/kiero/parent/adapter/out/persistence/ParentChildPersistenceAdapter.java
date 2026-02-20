package com.kiero.parent.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.kiero.child.domain.Child;
import com.kiero.parent.application.port.out.ParentChildAccessPort;
import com.kiero.parent.application.port.out.ParentChildLoadPort;
import com.kiero.parent.application.port.out.ParentChildSavePort;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.domain.ParentChild;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ParentChildPersistenceAdapter implements ParentChildLoadPort, ParentChildAccessPort, ParentChildSavePort {

	private final ParentChildRepository parentChildRepository;

	@Override
	public List<Long> findChildIdsByParentId(Long parentId) {
		return parentChildRepository.findChildIdsByParentId(parentId);
	}

	@Override
	public List<Parent> findParentsByChildId(Long childId) {
		return parentChildRepository.findParentsByChildId(childId);
	}

	@Override
	public List<ParentChild> findAllByParentId(Long parentId) {
		return parentChildRepository.findAllByParentId(parentId);
	}

	@Override
	public boolean existsByParentIdAndChildId(Long parentId, Long childId) {
		return parentChildRepository.existsByParentIdAndChildId(parentId, childId);
	}

	@Override
	public ParentChild save(ParentChild parentChild) {
		return parentChildRepository.save(parentChild);
	}
}
