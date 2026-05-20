package com.kiero.terms.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kiero.terms.domain.TermsAgreement;

public interface TermsAgreementRepository extends JpaRepository<TermsAgreement, Long> {

	@Query("""
		SELECT COUNT(ta) > 0
		FROM TermsAgreement ta
		WHERE ta.parent.id = :parentId
			AND ta.terms.id = :termsId
			AND ta.withdrawnAt IS NULL
		""")
	boolean existsByParentIdAndTermsIdAndWithdrawnAtIsNull(@Param("parentId") Long parentId,
		@Param("termsId") Long termsId);

	@Query("""
		SELECT ta.terms.id
		FROM TermsAgreement ta
		WHERE ta.parent.id = :parentId
			AND ta.withdrawnAt IS NULL
		""")
	List<Long> findActiveAgreedTermsIdsByParentId(@Param("parentId") Long parentId);
}