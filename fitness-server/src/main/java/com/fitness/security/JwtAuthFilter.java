package com.fitness.security;

import com.fitness.common.ErrorCode;
import com.fitness.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final TokenBlacklistService blacklistService;

    private static final List<String> WHITELIST = Arrays.asList(
            "/api/user/register", "/api/user/login", "/api/health"
    );

    public JwtAuthFilter(JwtUtil jwtUtil, ObjectMapper objectMapper,
                         TokenBlacklistService blacklistService) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.blacklistService = blacklistService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return WHITELIST.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
                writeUnauthorized(response, "缺少 Authorization 头");
                return;
            }

            String token = authHeader.substring(7);
            if (jwtUtil.isTokenExpired(token)) {
                writeUnauthorized(response, "Token 已过期");
                return;
            }

            // 检查黑名单（Redis 或内存）
            String tokenId = jwtUtil.getTokenId(token);
            if (blacklistService.isBlacklisted(tokenId)) {
                writeUnauthorized(response, "Token 已被注销");
                return;
            }

            Long userId = jwtUtil.getUserId(token);
            request.setAttribute("userId", userId);

            // 设置 SecurityContext（仅供参考，SecurityConfig 已改为 permitAll）
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(
                    new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList()));
            SecurityContextHolder.setContext(context);

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.warn("jwt auth failed: {}", e.getMessage());
            writeUnauthorized(response, "认证失败");
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        response.getWriter().write(
                objectMapper.writeValueAsString(Result.error(ErrorCode.UNAUTHORIZED.getCode(), msg))
        );
    }
}
