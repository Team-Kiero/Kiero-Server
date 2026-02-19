package com.kiero.missions.adapter.out.member;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kiero.child.adapter.out.persistence.ChildRepository;
import com.kiero.child.domain.Child;
import com.kiero.missions.application.port.out.ChildLoadPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChildPersistenceAdapter implements ChildLoadPort {
	private final ChildRepository childRepository;

	@Override
	public Optional<Child> findById(Long childId) {
		return childRepository.findById(childId);
	}

	@Override
	public Optional<Child> findByIdWithLock(Long childId) {
		return childRepository.findByIdWithLock(childId);
	}
}
