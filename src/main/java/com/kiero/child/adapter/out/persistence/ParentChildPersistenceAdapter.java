package com.kiero.child.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.kiero.child.application.port.out.ParentChildPersistencePort;
import com.kiero.parent.domain.ParentChild;
import com.kiero.parent.repository.ParentChildRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ParentChildPersistenceAdapter implements ParentChildPersistencePort {

	private final ParentChildRepository parentChildRepository;

	@Override
	public ParentChild save(ParentChild parentChild) {
		return parentChildRepository.save(parentChild);
	}
}
