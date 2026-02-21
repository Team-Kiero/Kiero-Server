package com.kiero.child.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kiero.child.domain.Child;

import jakarta.persistence.LockModeType;

public interface ChildRepository extends JpaRepository<Child, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT c FROM Child c WHERE c.id = :childId")
	Optional<Child> findByIdWithLock(@Param("childId") Long childId);

}