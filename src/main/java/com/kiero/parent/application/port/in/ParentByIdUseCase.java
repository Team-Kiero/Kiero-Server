package com.kiero.parent.application.port.in;

import java.util.Optional;

import com.kiero.parent.domain.Parent;

public interface ParentByIdUseCase {
	Optional<Parent> findById(Long parentId);
}
