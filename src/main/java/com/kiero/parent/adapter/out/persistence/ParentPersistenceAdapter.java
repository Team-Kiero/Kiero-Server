package com.kiero.parent.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.kiero.parent.application.port.out.ParentDeletePort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ParentPersistenceAdapter implements ParentDeletePort {

	private final ParentRepository parentRepository;

	@Override
	public void deleteParent(Long parentId) {
		parentRepository.deleteById(parentId);
	}
}
