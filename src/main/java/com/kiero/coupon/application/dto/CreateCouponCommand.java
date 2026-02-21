package com.kiero.coupon.application.dto;

public record CreateCouponCommand(
    Long parentId,
    Long childId,
    String name,
    int price
) {
}
