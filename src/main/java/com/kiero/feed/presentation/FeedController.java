package com.kiero.feed.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kiero.feed.exception.FeedSuccessCode;
import com.kiero.feed.presentation.dto.FeedGetResponse;
import com.kiero.feed.service.FeedService;
import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.response.dto.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/feeds")
public class FeedController {

	private final FeedService feedService;

    @PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
	@GetMapping("/{childId}")
	public ResponseEntity<SuccessResponse<FeedGetResponse>> getFeed(
		@PathVariable("childId") Long childId,
		@RequestParam(defaultValue = "20") Integer size,
		@RequestParam(required = false) String cursor,
		@CurrentMember CurrentAuth currentAuth
	) {
		FeedGetResponse response = feedService.getFeed(currentAuth.memberId(), childId, size, cursor);
		return ResponseEntity.ok()
			.body(SuccessResponse.of(FeedSuccessCode.FEED_GET_SUCCESS, response));
	}
}
