package com.kiero.parent.presentation.dto;

import com.kiero.child.domain.Child;

public record ChildInfoResponse(
        Long childId,
        String childLastName,
        String childFirstName
) {
    public static ChildInfoResponse of(Child child) {
        return new ChildInfoResponse(child.getId(), child.getLastName(), child.getFirstName());
    }
}
