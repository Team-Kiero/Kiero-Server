package com.kiero.global.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.kiero.global.util.ApiQueryCounter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class QueryLoggingAspect {

	private final ApiQueryCounter apiQueryCounter;

	@Around("within(@org.springframework.web.bind.annotation.RestController *)")
	public Object logQueries(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

		long startTime = System.currentTimeMillis();

		try {
			return proceedingJoinPoint.proceed();
		} finally {
			long processingTime = System.currentTimeMillis() - startTime;

			log.info(
				"Occurred Method : {}, Query Count : {}, Processing Time : {}ms",
				proceedingJoinPoint.getSignature().getDeclaringTypeName()
					+ " - " + proceedingJoinPoint.getSignature().getName(),
				apiQueryCounter.getCount(),
				processingTime
			);
		}
	}
}