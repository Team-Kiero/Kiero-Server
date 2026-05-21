package com.kiero.parent.adapter.out.redis;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import lombok.AllArgsConstructor;
import lombok.Getter;

@RedisHash("parentWithdraw")
@Getter
@AllArgsConstructor(staticName = "of")
public class ParentWithdrawal {

	@Id
	private String id;

	private Long childId;

	@TimeToLive
	private Long ttl;
}
