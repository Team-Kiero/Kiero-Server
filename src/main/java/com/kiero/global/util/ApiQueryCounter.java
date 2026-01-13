package com.kiero.global.util;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import lombok.Getter;

@Component
@RequestScope
@Getter
public class ApiQueryCounter {

	private int count;

	public void increaseCount() {
		count ++;
	}
}
