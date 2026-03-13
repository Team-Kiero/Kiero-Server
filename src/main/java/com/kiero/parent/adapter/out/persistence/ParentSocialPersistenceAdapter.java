package com.kiero.parent.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kiero.global.auth.client.enums.Provider;
import com.kiero.parent.application.port.out.ParentLoadPort;
import com.kiero.parent.application.port.out.ParentSavePort;
import com.kiero.parent.domain.Parent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ParentSocialPersistenceAdapter implements ParentLoadPort, ParentSavePort {

	private final ParentRepository parentRepository;

	@Override
	public Optional<Parent> findById(Long parentId) {
		return parentRepository.findById(parentId);
	}

	@Override
	public Optional<Parent> findParentBySocialIdAndProvider(String socialId, Provider provider) {
		return parentRepository.findParentBySocialIdAndProvider(socialId, provider);
	}

	@Override
	public Parent save(Parent parent) {
		return parentRepository.save(parent);
	}

	@Override
	public List<Long> findAllIds() { return parentRepository.findAllIds(); }
}
