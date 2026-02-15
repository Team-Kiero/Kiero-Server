package com.kiero.child.application.port.out;

import java.util.Optional;

import com.kiero.child.domain.Child;

public interface ChildPersistencePort {
	Child save(Child child);
	Optional<Child> findById(Long childId);
	Optional<Child> findByIdWithLock(Long childId);
}
