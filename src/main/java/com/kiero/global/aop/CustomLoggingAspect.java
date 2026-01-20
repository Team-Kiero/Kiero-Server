package com.kiero.global.aop;

import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.util.ApiQueryCounter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CustomLoggingAspect {

	private final ApiQueryCounter apiQueryCounter;

	@Around("within(@org.springframework.web.bind.annotation.RestController *)")
	public Object logQueries(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

		long startTime = System.currentTimeMillis();

		try {
			return proceedingJoinPoint.proceed();
		} finally {
			long processingTime = System.currentTimeMillis() - startTime;
			long queryCount = apiQueryCounter.getCount();

			String userInfo = extractUserInfo(proceedingJoinPoint);
			String requiredRole = extractRequiredRole(proceedingJoinPoint);
			String clientIp = extractClientIp();

			String httpMethod = extractHttpMethod();
			String fullPath = extractFullPath();

			log.info(
				"[API] {} {} | Occurred Method : {} | IP: {} | User: {} | Required: {} | Queries : {} | Time : {}ms",
				httpMethod,
				fullPath,
				proceedingJoinPoint.getSignature().getDeclaringTypeName()
					+ " - " + proceedingJoinPoint.getSignature().getName(),
				clientIp,
				userInfo,
				requiredRole,
				queryCount,
				processingTime
			);
		}
	}

	private String extractClientIp() {
		try {
			ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
			if (attributes == null)
				return "Unknown";

			HttpServletRequest request = attributes.getRequest();

			String ip = request.getHeader("X-Forwarded-For");

			if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getHeader("Proxy-Client-IP");
			}
			if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getHeader("WL-Proxy-Client-IP");
			}
			if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getHeader("HTTP_CLIENT_IP");
			}
			if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getHeader("HTTP_X_FORWARDED_FOR");
			}
			if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getRemoteAddr();
			}

			// X-Forwarded-For에 여러 IP가 찍히는 경우(예: client, proxy1, proxy2) 첫 번째 IP가 실제 클라이언트
			if (ip != null && ip.contains(",")) {
				return ip.split(",")[0].trim();
			}

			return ip;
		} catch (Exception e) {
			return "Unknown";
		}
	}

	private String extractUserInfo(ProceedingJoinPoint joinPoint) {
		String id = "Anonymous";
		String role = "None";

		for (Object arg : joinPoint.getArgs()) {
			if (arg instanceof CurrentAuth currentAuth) {
				id = String.valueOf(currentAuth.memberId());
				role = String.valueOf(currentAuth.role());
				break;
			}
		}

		return String.format("ID:%s, Role:%s", id, role);
	}

	private String extractRequiredRole(ProceedingJoinPoint joinPoint) {
		MethodSignature signature = (MethodSignature)joinPoint.getSignature();
		Method method = signature.getMethod();

		PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
		if (preAuth != null) {
			return preAuth.value();
		}

		return "Public";
	}

	private String extractHttpMethod() {
		try {
			ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
			if (attributes == null)
				return "UNKNOWN";
			return attributes.getRequest().getMethod();
		} catch (Exception e) {
			return "UNKNOWN";
		}
	}

	private String extractFullPath() {
		try {
			ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
			if (attributes == null)
				return "Unknown";

			HttpServletRequest request = attributes.getRequest();

			String uri = request.getRequestURI();
			String query = request.getQueryString();

			return (query == null || query.isBlank() ? uri : uri + "?" + query);
		} catch (Exception e) {
			return "Unknown";
		}
	}
}