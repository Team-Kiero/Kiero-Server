package com.kiero.admin.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.kiero.admin.application.port.out.AdminChildLoadPort;
import com.kiero.child.adapter.out.persistence.ChildRepository;
import com.kiero.child.domain.Child;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminChildPersistenceAdapter implements AdminChildLoadPort {

	private final ChildRepository childRepository;

	@Override
	public Page<Child> findAll(Pageable pageable) {
		return childRepository.findAll(pageable);
	}

	@Override
	public Optional<Child> findById(Long childId) {
		return childRepository.findById(childId);
	}
}
