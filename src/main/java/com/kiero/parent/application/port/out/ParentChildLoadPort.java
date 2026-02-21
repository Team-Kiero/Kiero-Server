package com.kiero.parent.application.port.out;

import java.util.List;

import com.kiero.parent.domain.Parent;
import com.kiero.parent.domain.ParentChild;

public interface ParentChildLoadPort {
	List<Long> findChildIdsByParentId(Long parentId);
	List<ParentChild> findAllByParentId(Long parentId);
	List<Parent> findParentsByChildId(Long childId);
}
