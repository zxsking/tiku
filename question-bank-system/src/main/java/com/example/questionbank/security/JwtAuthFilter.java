package com.example.questionbank.security;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        String header = request.getHeader("Authorization");
        String bearer = "Bearer ";
        String token = null;
        
        if (header != null && header.startsWith(bearer)) {
            token = header.substring(bearer.length());
        }
        // SSE(EventSource) 无法自定义请求头，允许通过 query 参数 ?token= 传递
        if (!StringUtils.hasText(token)) {
            String tokenParam = request.getParameter("token");
            if (StringUtils.hasText(tokenParam)) {
                token = tokenParam;
            }
        }

        if (token != null) {
            try {
                String username = jwtUtil.getUsernameFromToken(token);
                
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    if (jwtUtil.validateToken(token) && !jwtUtil.isTokenExpired(token)) {
                        UsernamePasswordAuthenticationToken authToken = 
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (ExpiredJwtException e) {
                // token过期，这里可以记录日志但不能打断请求流程
            }
        }
        
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        // SseEmitter 会触发 ASYNC dispatch；默认 OncePerRequestFilter 会跳过异步调度，
        // 导致后续调度时鉴权信息丢失而被 AuthorizationFilter 拒绝。
        return false;
    }
}