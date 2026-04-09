package com.kiero.admin.application.port.out;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.kiero.parent.domain.Parent;

public interface AdminParentLoadPort {
	Page<Parent> findAll(Pageable pageable);
	Optional<Parent> findById(Long parentId);
}
