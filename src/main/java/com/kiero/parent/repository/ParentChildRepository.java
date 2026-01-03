package com.kiero.parent.repository;

import com.kiero.parent.domain.ParentChild;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParentChildRepository extends JpaRepository<ParentChild, Long> {

    @Query("SELECT pc.child.id FROM ParentChild pc WHERE pc.parent.id = :parentId")
    List<Long> findChildIdsByParentId(@Param("parentId") Long parentId);

    @EntityGraph(attributePaths = {"child"})
    List<ParentChild> findAllByParent_Id(Long parentId);
}
