package com.kiero.invitation.adapter.out.persistence;

import com.kiero.invitation.domain.InviteCode;
import org.springframework.data.repository.CrudRepository;

public interface InviteCodeRepository extends CrudRepository<InviteCode, String> {

}
