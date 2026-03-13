package com.kiero.parent.application.port.out;

import java.util.Optional;

import com.kiero.global.auth.client.enums.Provider;
import com.kiero.parent.domain.Parent;

public interface ParentLoadPort {
	Optional<Parent> findById(Long parentId);
	Optional<Parent> findParentBySocialIdAndProvider(String socialId, Provider provider);
}
