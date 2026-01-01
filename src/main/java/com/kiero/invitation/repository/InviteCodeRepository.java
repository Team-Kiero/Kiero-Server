package com.kiero.invitation.repository;

import com.kiero.invitation.domain.InviteCode;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface InviteCodeRepository extends CrudRepository<InviteCode, String> {

    Optional<InviteCode> findByParentId(Long parentId);
}
