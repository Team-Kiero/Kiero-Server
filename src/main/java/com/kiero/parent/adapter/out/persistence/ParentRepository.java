package com.kiero.parent.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.kiero.global.auth.client.enums.Provider;
import com.kiero.parent.domain.Parent;

public interface ParentRepository extends JpaRepository<Parent, Long> {

	Optional<Parent> findParentBySocialIdAndProvider(String socialId, Provider provider);

	@Query(
		"""
		select p.id
		from Parent p
		"""
	)
	List<Long> findAllIds();
}