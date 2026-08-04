package com.kiero.parent.application.dto;

import com.kiero.child.domain.Child;
import com.kiero.parent.domain.ParentChild;

public record ChildInfoResponse(
        Long id,
        Long childId,
        String childLastName,
        String childFirstName
) {
    public static ChildInfoResponse of(ParentChild parentChild) {
        Child child = parentChild.getChild();
        return new ChildInfoResponse(parentChild.getId(), child.getId(), child.getLastName(), child.getFirstName());
    }
}
