package com.kiero.child.application.port.in;

import java.util.Optional;

import com.kiero.child.application.dto.ChildMeResponse;
import com.kiero.child.domain.Child;

public interface ChildQueryUseCase {
	Optional<Child> findById(Long childId);
	Optional<Child> findByIdWithLock(Long childId);
	ChildMeResponse getMyInfo(Long childId);
}
