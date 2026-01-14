package com.kiero.global.config;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import com.kiero.global.util.ApiQueryCounter;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QueryInspector implements StatementInspector {

	private final ApiQueryCounter apiQueryCounter;

	@Override
	public String inspect(String sql) {
		if (RequestContextHolder.getRequestAttributes() != null) {
			apiQueryCounter.increaseCount();
		}
		return sql;
	}
}
