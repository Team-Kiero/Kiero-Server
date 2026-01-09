package com.kiero.child.repository;

import com.kiero.child.domain.Child;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChildRepository extends JpaRepository<Child, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Child c WHERE c.id = :childId")
    Optional<Child> findByIdWithLock(@Param("childId") Long childId);
}
