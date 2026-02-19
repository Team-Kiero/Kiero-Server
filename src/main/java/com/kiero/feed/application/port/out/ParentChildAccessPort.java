package com.kiero.feed.application.port.out;

public interface ParentChildAccessPort {

	boolean existsByParentIdAndChildId(Long parentId, Long childId);

}
