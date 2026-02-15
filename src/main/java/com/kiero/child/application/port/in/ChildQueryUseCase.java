package com.kiero.child.application.port.in;

import java.util.Optional;

import com.kiero.child.domain.Child;

public interface ChildQueryUseCase {
	Optional<Child> findByIdWithLock(Long childId);
}
