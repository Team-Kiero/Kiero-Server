package com.kiero.feed.presentation.dto;

import java.time.LocalDateTime;

import com.kiero.feed.exception.FeedErrorCode;
import com.kiero.global.exception.KieroException;

public record FeedCursor(
	LocalDateTime occurredAt,
	Long id
) {
	public static FeedCursor parse(String cursor) {
		if (cursor == null || cursor.isBlank()) return null;

		try {
			String[] parts = cursor.split("\\|");
			if (parts.length != 2)
				throw new KieroException(FeedErrorCode.CURSOR_NOT_VALID);

			return new FeedCursor(
				LocalDateTime.parse(parts[0]),
				Long.parseLong(parts[1])
			);
		} catch (Exception e) {
			throw new KieroException(FeedErrorCode.CURSOR_NOT_VALID);
		}
	}

	public String toCursorString() {
		return occurredAt.toString() + "|" + id;
	}
}
