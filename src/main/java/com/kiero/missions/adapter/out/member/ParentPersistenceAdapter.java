package com.kiero.missions.adapter.out.member;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kiero.missions.application.port.out.ParentLoadPort;
import com.kiero.parent.adapter.out.persistence.ParentRepository;
import com.kiero.parent.domain.Parent;

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
