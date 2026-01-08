package com.kiero.child.presentation.dto;

import com.kiero.child.domain.Child;

public record ChildMeResponse(
        String name,
        int coinAmount
) {
    public static ChildMeResponse from(Child child) {
        return new ChildMeResponse(
                child.getName(),
                child.getCoinAmount()
        );
    }
}
