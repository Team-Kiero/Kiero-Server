package com.kiero.invitation.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InviteCodeSecondWord {
    DOG("강아지"),
    CAT("고양이"),
    RABBIT("토끼"),
    HAMSTER("햄스터"),
    PARROT("앵무새"),
    TURTLE("거북이"),
    GOLDFISH("금붕어"),
    SQUIRREL("다람쥐"),
    GUINEA_PIG("기니피그"),
    CAPYBARA("카피바라");

    private final String value;
}
