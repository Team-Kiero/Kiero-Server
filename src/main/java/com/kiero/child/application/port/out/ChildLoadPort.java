package com.kiero.child.application.port.out;

import java.util.Optional;

import com.kiero.child.domain.Child;

public interface ChildLoadPort {
	Optional<Child> findById(Long childId);
	Optional<Child> findByIdWithLock(Long childId);
	Optional<Child> findByParentIdAndName(Long parentId, String lastName, String firstName);
}
