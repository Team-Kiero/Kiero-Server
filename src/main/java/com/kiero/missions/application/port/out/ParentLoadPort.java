package com.kiero.missions.application.port.out;

import java.util.Optional;

import com.kiero.parent.domain.Parent;

public interface ParentLoadPort {
	Optional<Parent> findById(Long parentId);
}
