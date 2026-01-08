package com.kiero.schedule.presentation.dto;

import java.util.List;

import com.kiero.schedule.domain.enums.StoneType;

public record FireLitResponse(
	List<StoneType> gotStones,
	int earnedCoinAmount
) {
	public static FireLitResponse of(
		List<StoneType> gotStones,
		int earnedCoinAmount
	) {
		return new FireLitResponse(gotStones, earnedCoinAmount);
	}
}
