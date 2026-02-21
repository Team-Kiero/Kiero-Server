package com.kiero.global.auth.jwt.application.port.out;

import java.util.List;

public interface ParentChildRelationQueryPort {
	List<Long> findChildIdsByParentId(Long parentId);
}