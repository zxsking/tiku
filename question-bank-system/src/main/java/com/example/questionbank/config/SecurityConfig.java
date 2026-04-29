package com.example.questionbank.config;

import com.example.questionbank.security.JwtAuthFilter;
import com.example.questionbank.security.TokenQueryParamFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private TokenQueryParamFilter tokenQueryParamFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/auth/login", "/auth/register").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/categories").permitAll()
                        .requestMatchers("/banks").permitAll()
                        .requestMatchers("/banks/**").permitAll()
                        .requestMatchers("/questions").permitAll()
                        .requestMatchers("/questions/**").permitAll()
                        .requestMatchers("/stats/**").permitAll()
                        .requestMatchers("/search/**").permitAll()
                        .requestMatchers("/user/*/profile", "/user/*/banks", "/user/*/questions").permitAll()
                        // 明确声明 /user/** 需要登录（避免被 anyRequest 兜底时顺序问题影响）
                        .requestMatchers("/user/**").authenticated()
                        // 练习/考试模块需要登录
                        .requestMatchers("/practice/**").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        //api文档
                        .requestMatchers("/doc.html").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/webjars/**").permitAll()
                        .anyRequest().authenticated()
                );

        // tokenQueryParamFilter 先执行（Order=1），将 ?token= 转换为 Authorization header
        // jwtAuthFilter 再执行，读取 Authorization header 完成认证
        http.addFilterBefore(tokenQueryParamFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}