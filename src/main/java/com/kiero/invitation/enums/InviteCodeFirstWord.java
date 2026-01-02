package com.kiero.invitation.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InviteCodeFirstWord {
    APPLE("사과"),
    BANANA("바나나"),
    GRAPE("포도"),
    STRAWBERRY("딸기"),
    WATERMELON("수박"),
    PEACH("복숭아"),
    PLUM("자두"),
    MANGO("망고"),
    KIWI("키위"),
    CHERRY("체리");

    private final String value;
}