package com.kiero.invitation.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InviteCodeSecondWord {
    KNIGHT("기사"),
    WARRIOR("용사"),
    CAPTAIN("대장"),
    WIZARD("마법사"),
    EXPLORER("탐험가"),
    GUARDIAN("수호자"),
    FAIRY("요정"),
    HERO("히어로"),
    NINJA("닌자"),
    RANGER("레인저"),
    JEWEL("보석"),
    TIGER("호랑이"),
    EAGLE("독수리"),
    GOBLIN("도깨비"),
    ASTRONAUT("우주인");

    private final String value;
}
