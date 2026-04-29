package com.example.questionbank.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 支持通过 query 参数 ?token=xxx 传递 JWT。
 * 主要用于 SSE 端点（浏览器 EventSource 不支持自定义 Header）。
 * 若请求已有 Authorization header，则不做任何处理。
 * Order(1) 确保在 JwtAuthFilter 之前执行。
 */
@Component
@Order(1)
public class TokenQueryParamFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String tokenParam = request.getParameter("token");

        // 已有 Authorization header，或没有 token 参数，直接放行
        if (authHeader != null || tokenParam == null || tokenParam.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        // 用 wrapper 注入 Authorization header，让后续的 JwtAuthFilter 正常处理
        final String bearerToken = "Bearer " + tokenParam;
        HttpServletRequestWrapper wrapped = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if ("Authorization".equalsIgnoreCase(name)) return bearerToken;
                return super.getHeader(name);
            }
        };

        chain.doFilter(wrapped, response);
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        // 让 token 注入逻辑在 ASYNC dispatch 也生效（SSE 场景）
        return false;
    }
}
