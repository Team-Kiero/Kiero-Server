package com.kiero.parent.application.port.out;

import java.util.List;

import com.kiero.child.domain.Child;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.domain.ParentChild;

public interface ParentChildQueryPort {
	List<Long> findChildIdsByParentId(Long parentId);
	List<ParentChild> findAllByParentId(Long parentId);
	boolean existsByParentIdAndChildId(Long parentId, Long childId);
	List<Parent> findParentsByChildId(Long childId);
	boolean existsByParentAndChild(Parent parent, Child child);
}
