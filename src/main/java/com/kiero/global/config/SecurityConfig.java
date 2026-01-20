package com.kiero.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.kiero.global.auth.jwt.service.JwtAuthenticationFilter;

import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.formLogin(form -> form.disable())
			.httpBasic(basic -> basic.disable())
			.sessionManagement(sm ->
				sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			.authorizeHttpRequests(auth -> auth
				.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers(
					"/swagger-ui/**",
					"/v3/api-docs/**",
					"/health-check",
					"/actuator/health",
					"/api/v1/parents/login",
					"/api/v1/parents/login/access-token",
					"/api/v1/children/signup",
					"/api/v1/tokens/reissue/*",
					"/api/v1/tokens/subscribe-token"
				).permitAll()

				.requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()

				.anyRequest().authenticated()
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
