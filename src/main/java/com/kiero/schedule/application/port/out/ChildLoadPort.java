package com.kiero.schedule.application.port.out;

import java.util.Optional;

import com.kiero.child.domain.Child;

public interface ChildLoadPort {
	Optional<Child> findById(Long childId);
}
