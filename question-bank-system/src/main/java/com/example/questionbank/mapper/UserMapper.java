package com.example.questionbank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.questionbank.entity.User;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMapper extends BaseMapper<User> {
    
    @Select("SELECT * FROM users WHERE username = #{username}")
    User selectByUsername(String username);
    
    @Select("SELECT * FROM users WHERE email = #{email}")
    User selectByEmail(String email);
}