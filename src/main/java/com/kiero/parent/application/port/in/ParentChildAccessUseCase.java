package com.kiero.parent.application.port.in;

public interface ParentChildAccessUseCase {
	boolean existsByParentIdAndChildId(Long parentId, Long childId);
}
