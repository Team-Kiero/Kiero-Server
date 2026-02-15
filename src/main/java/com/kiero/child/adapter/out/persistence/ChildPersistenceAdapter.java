package com.kiero.child.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kiero.child.application.port.out.ChildPersistencePort;
import com.kiero.child.domain.Child;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChildPersistenceAdapter implements ChildPersistencePort {

	private final ChildRepository childRepository;

	@Override
	public Child save(Child child) {
		return childRepository.save(child);
	}

	@Override
	public Optional<Child> findById(Long childId) {
		return childRepository.findById(childId);
	}

	@Override
	public Optional<Child> findByIdWithLock(Long childId) {
		return childRepository.findByIdWithLock(childId);
	}
}