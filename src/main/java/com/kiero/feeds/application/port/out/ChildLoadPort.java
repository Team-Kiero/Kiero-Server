package com.kiero.feeds.application.port.out;

import java.util.Optional;

import com.kiero.child.domain.Child;

public interface ChildLoadPort {

	Optional<Child> findById(Long childId);

}
