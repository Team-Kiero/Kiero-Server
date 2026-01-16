package com.kiero.parent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kiero.child.domain.Child;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.domain.ParentChild;

public interface ParentChildRepository extends JpaRepository<ParentChild, Long> {

	@Query("SELECT pc.child.id FROM ParentChild pc WHERE pc.parent.id = :parentId")
	List<Long> findChildIdsByParentId(@Param("parentId") Long parentId);

	boolean existsByParentAndChild(Parent parent, Child child);

	@EntityGraph(attributePaths = {"child"})
	List<ParentChild> findAllByParentId(Long parentId);

	boolean existsByParentIdAndChildId(Long parentId, Long childId);

	@Query("""
		select pc.parent
		from ParentChild pc
		where pc.child.id = :childId
		""")
	List<Parent> findParentsByChildId(@Param("childId") Long childId);

	/*
	데모데이용 임시 메서드
	 */
	void deleteByChildIdIn(List<Long> childIds);
	/*
	 */
}
