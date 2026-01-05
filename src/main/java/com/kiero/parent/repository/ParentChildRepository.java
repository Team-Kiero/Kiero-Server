package com.kiero.parent.repository;

import com.kiero.child.domain.Child;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.domain.ParentChild;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParentChildRepository extends JpaRepository<ParentChild, Long> {

    @Query("SELECT pc.child.id FROM ParentChild pc WHERE pc.parent.id = :parentId")
    List<Long> findChildIdsByParentId(@Param("parentId") Long parentId);

    boolean existsByParentAndChild(Parent parent, Child child);
}
