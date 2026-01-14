package com.kiero.global.aop;

import com.kiero.child.repository.ChildRepository;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.auth.enums.Role;
import com.kiero.global.util.ApiQueryCounter;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.repository.ParentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class QueryLoggingAspect {

	private final ApiQueryCounter apiQueryCounter;
    private final ParentRepository parentRepository;
    private final ChildRepository childRepository;

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

			log.info(
				"[API] Occurred Method : {} | User: {} | Required: {} | Queries : {} | Time : {}ms",
                proceedingJoinPoint.getSignature().getDeclaringTypeName()
                + " - " + proceedingJoinPoint.getSignature().getName(),
                userInfo,
                requiredRole,
                queryCount,
				processingTime
			);
		}
	}

    private String extractUserInfo(ProceedingJoinPoint joinPoint) {
        String id = "Anonymous";
        String role = "None";
        String name = "Unknown";

        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof CurrentAuth currentAuth) {
                id = String.valueOf(currentAuth.memberId());
                role = String.valueOf(currentAuth.role());
                name = findNameByRole(currentAuth.memberId(), currentAuth.role());
                break;
            }
        }

        return String.format("ID:%s, Name:%s, Role:%s", id, name, role);
    }

    private String findNameByRole(long id, Role role) {
        try {
            if (Role.PARENT.equals(role)) {
                return parentRepository.findById(id)
                        .map(Parent::getName)
                        .orElse("Unknown Parent");
            } else if (Role.CHILD.equals(role)) {
                return childRepository.findById(id)
                        .map(child -> child.getLastName()+child.getFirstName())
                        .orElse("Unknown Child");
            } else if (Role.ADMIN.equals(role)) {
                return "Admin";
            }
        } catch (Exception e) {
            log.warn("Failed to fetch user name in AOP: {}", e.getMessage());
            return "Unknown";
        }

        return "Unknown";
    }

    private String extractRequiredRole(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        if (preAuth != null) {
            return preAuth.value();
        }

        return "Public";
    }
}