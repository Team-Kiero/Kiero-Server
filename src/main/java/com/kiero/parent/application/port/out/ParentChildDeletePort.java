package com.kiero.parent.application.port.out;

public interface ParentChildDeletePort {
	void deleteAllParentChildRelationsByParentId(Long parentId);
}
