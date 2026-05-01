package com.domaindns.config;

import com.domaindns.auth.service.JwtService;
import com.domaindns.common.ClientIpUtils;
import com.domaindns.common.RateLimiter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final String API_CSP = "default-src 'self'; base-uri 'self'; frame-ancestors 'none'; "
            + "object-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' data: blob: https:; font-src 'self' data:; form-action 'self'";

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService, StringRedisTemplate redis,
            RateLimiter rateLimiter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(API_CSP))
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(contentTypeOptions -> {
                        })
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicy(permissions -> permissions.policy(
                                "geolocation=(), microphone=(), camera=(), payment=(), usb=(), browsing-topics=()")))
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                            FilterChain filterChain) throws ServletException, IOException {
                        if (request.getRequestURI().startsWith("/api/admin/")) {
                            String ip = ClientIpUtils.resolveClientIp(request);
                            boolean allowed = rateLimiter.tryConsume("rl:admin:api:ip:" + ip, 300, Duration.ofMinutes(1));
                            if (!allowed) {
                                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"code\":42900,\"message\":\"请求过于频繁，请稍后再试\"}");
                                return;
                            }
                        }
                        filterChain.doFilter(request, response);
                    }
                }, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                            FilterChain filterChain) throws ServletException, IOException {
                        String auth = request.getHeader("Authorization");
                        if (auth != null && auth.startsWith("Bearer ")) {
                            String token = auth.substring(7);
                            try {
                                Jws<Claims> jws = jwtService.parse(token);
                                String jti = jws.getBody().getId();
                                if (jti != null) {
                                    String key = "jwt:blacklist:" + jti;
                                    String v = redis.opsForValue().get(key);
                                    if (v != null) {
                                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                                        response.setContentType("application/json;charset=UTF-8");
                                        response.getWriter().write("{\"code\":40101,\"message\":\"Token 已失效\"}");
                                        return;
                                    }
                                }

                                String role = jws.getBody().get("role", String.class);
                                if (role != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                                    UsernamePasswordAuthenticationToken authentication =
                                            new UsernamePasswordAuthenticationToken(
                                                    jws.getBody().getSubject(),
                                                    null,
                                                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                                    SecurityContextHolder.getContext().setAuthentication(authentication);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                        filterChain.doFilter(request, response);
                    }
                }, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                            FilterChain filterChain) throws ServletException, IOException {
                        filterChain.doFilter(request, response);
                        if (request.getRequestURI().startsWith("/api/admin/")
                                || request.getRequestURI().startsWith("/api/auth/admin/")) {
                            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                            response.setHeader("Pragma", "no-cache");
                            response.setHeader("Expires", "0");
                        }
                    }
                }, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin/announcements/published").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/email-whitelist").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/domains/search", "/api/zones",
                                "/api/user/verification/reward-config").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/register/send-code",
                                "/api/auth/forgot", "/api/auth/reset", "/api/auth/logout",
                                "/api/auth/admin/login").permitAll()
                        .requestMatchers("/api/auth/admin/register").denyAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/user/**").hasRole("USER")
                        .anyRequest().permitAll());

        return http.build();
    }
}
