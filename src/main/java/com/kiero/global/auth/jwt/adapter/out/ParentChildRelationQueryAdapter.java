package com.kiero.global.auth.jwt.adapter.out;

import java.util.List;

import org.springframework.stereotype.Component;

import com.kiero.global.auth.jwt.application.port.out.ParentChildRelationQueryPort;
import com.kiero.parent.application.port.in.ParentChildQueryUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ParentChildRelationQueryAdapter implements ParentChildRelationQueryPort {

	private final ParentChildQueryUseCase parentChildQueryUseCase;

	@Override
	public List<Long> findChildIdsByParentId(Long parentId) {
		return parentChildQueryUseCase.getMyChildIds(parentId);
	}
}