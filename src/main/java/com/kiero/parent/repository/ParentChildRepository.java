package com.kiero.parent.repository;

import com.kiero.parent.domain.ParentChild;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParentChildRepository extends JpaRepository<ParentChild, Long> {

    List<ParentChild> findAllByParent_Id(Long parentId);
}
