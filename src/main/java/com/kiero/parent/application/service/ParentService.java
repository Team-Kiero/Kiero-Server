package com.kiero.parent.application.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.kiero.parent.application.port.in.ParentByIdUseCase;
import com.kiero.parent.application.port.out.ParentLoadPort;
import com.kiero.parent.domain.Parent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParentService implements ParentByIdUseCase {

	private final ParentLoadPort parentLoadPort;

	@Override
	public Optional<Parent> findById(Long parentId) {
		return parentLoadPort.findById(parentId);
	};

}
