package com.kiero.global.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.auth.enums.Role;
import com.kiero.global.auth.jwt.exception.TokenErrorCode;
import com.kiero.global.exception.KieroException;

@Component
public class CurrentMemberArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.getParameterAnnotation(CurrentMember.class) != null
			&& parameter.getParameterType().equals(CurrentAuth.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}
		Long memberId = Long.valueOf(authentication.getPrincipal().toString());
		Role role = authentication.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.map(a-> a.replace("ROLE_", ""))
			.map(Role::valueOf)
			.findFirst()
			.orElseThrow(()-> new KieroException(TokenErrorCode.INVALID_REFRESH_TOKEN_ERROR));
		return new CurrentAuth(memberId, role);
	}

}