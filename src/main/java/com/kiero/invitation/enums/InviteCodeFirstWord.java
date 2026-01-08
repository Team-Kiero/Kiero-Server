package com.kiero.invitation.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InviteCodeFirstWord {
    STARLIGHT("별빛"),
    GOLDEN("황금"),
    BRAVE("용감한"),
    THUNDER("천둥"),
    SECRET("비밀"),
    INVINCIBLE("무적의"),
    BLUE("푸른"),
    SWEET("달콤한"),
    DAWN("새벽"),
    WIND("바람"),
    SUN("태양"),
    MYSTERIOUS("신비한"),
    STEEL("강철"),
    HAPPY("행복한"),
    RAINBOW("무지개");

    private final String value;
}