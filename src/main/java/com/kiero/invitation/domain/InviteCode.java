package com.kiero.invitation.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@RedisHash(value = "inviteCode", timeToLive = 60L * 10)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InviteCode {

    @Id
    private String code;

    @Indexed
    private Long parentId;

    private String childName;

    public static InviteCode of(String code, Long parentId, String childName) {
        return new InviteCode(code, parentId, childName);
    }
}
