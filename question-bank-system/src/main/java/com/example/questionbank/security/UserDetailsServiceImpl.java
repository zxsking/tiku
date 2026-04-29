package com.example.questionbank.security;

import com.example.questionbank.entity.User;
import com.example.questionbank.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户名不存在: " + username);
        }

        boolean isActive = "active".equals(user.getStatus());

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                // roles() 自动加 ROLE_ 前缀，统一转大写避免大小写问题
                .roles(user.getRole().toUpperCase())
                // 账号未激活才是 expired/disabled，active 用户三项都应为 false
                .accountExpired(!isActive)
                .credentialsExpired(false)   // 凭证过期单独管理，不与 status 混用
                .disabled(!isActive)
                .build();
    }
}