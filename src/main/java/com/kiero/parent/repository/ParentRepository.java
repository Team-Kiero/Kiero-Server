package com.kiero.parent.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kiero.global.auth.client.enums.Provider;
import com.kiero.parent.domain.Parent;

public interface ParentRepository extends JpaRepository<Parent, Long> {

	Optional<Parent> findParentBySocialIdAndProvider(String socialId, Provider provider);
}
