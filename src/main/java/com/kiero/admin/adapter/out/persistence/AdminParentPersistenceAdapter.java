package com.kiero.admin.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.kiero.admin.application.port.out.AdminParentLoadPort;
import com.kiero.parent.adapter.out.persistence.ParentRepository;
import com.kiero.parent.domain.Parent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminParentPersistenceAdapter implements AdminParentLoadPort {

	private final ParentRepository parentRepository;

	@Override
	public Page<Parent> findAll(Pageable pageable) {
		return parentRepository.findAll(pageable);
	}

	@Override
	public Optional<Parent> findById(Long parentId) {
		return parentRepository.findById(parentId);
	}
}
