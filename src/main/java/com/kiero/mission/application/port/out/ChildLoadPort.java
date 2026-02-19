package com.kiero.mission.application.port.out;

import java.util.Optional;

import com.kiero.child.domain.Child;

public interface ChildLoadPort {
	Optional<Child> findById(Long childId);

	Optional<Child> findByIdWithLock(Long childId);
}
