package com.kiero.child.presentation.dto;

import com.kiero.child.domain.Child;

public record ChildMeResponse(
        String lastName,
        String firstName,
        int coinAmount
) {
    public static ChildMeResponse from(Child child) {
        return new ChildMeResponse(
                child.getLastName(),
                child.getFirstName(),
                child.getCoinAmount()
        );
    }
}
