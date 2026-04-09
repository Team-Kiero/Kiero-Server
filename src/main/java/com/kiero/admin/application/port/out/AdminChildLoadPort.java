package com.kiero.admin.application.port.out;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.kiero.child.domain.Child;

public interface AdminChildLoadPort {
	Page<Child> findAll(Pageable pageable);
	Optional<Child> findById(Long childId);
}
