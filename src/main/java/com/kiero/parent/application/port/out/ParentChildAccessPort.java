package com.kiero.parent.application.port.out;

public interface ParentChildAccessPort {
	boolean existsByParentIdAndChildId(Long parentId, Long childId);
}
