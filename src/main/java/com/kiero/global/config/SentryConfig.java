package com.kiero.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.kiero.global.exception.KieroException;

import io.sentry.SentryOptions;
import io.sentry.spring.jakarta.SentryTaskDecorator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class SentryConfig {

	@Bean
	public SentryOptions.BeforeSendCallback beforeSendCallback() {
		return (event, hint) -> {
			Throwable throwable = event.getThrowable();

			if (throwable instanceof KieroException kieroException) {
				if (!kieroException.getBaseCode().getHttpStatus().is5xxServerError()) {
					log.debug("Sentry 필터링: {} ({})",
						kieroException.getBaseCode().getMessage(),
						kieroException.getBaseCode().getHttpStatus().value());
					return null;
				}
			}

			return event;
		};
	}

	@Bean
	public SentryTaskDecorator sentryTaskDecorator() {
		return new SentryTaskDecorator();
	}
}
