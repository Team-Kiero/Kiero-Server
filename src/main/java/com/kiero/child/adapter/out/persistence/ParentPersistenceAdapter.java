package com.kiero.child.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kiero.child.application.port.out.ParentLoadPort;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.repository.ParentRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ParentPersistenceAdapter implements ParentLoadPort {

	private final ParentRepository parentRepository;

	@Override
	public Optional<Parent> findById(Long parentId) {
		return parentRepository.findById(parentId);
	}
}