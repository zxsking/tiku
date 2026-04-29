package com.example.questionbank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.questionbank.service.UserService;
import com.example.questionbank.entity.Bank;
import com.example.questionbank.entity.Favorite;
import com.example.questionbank.entity.Follow;
import com.example.questionbank.entity.Question;
import com.example.questionbank.entity.User;
import com.example.questionbank.mapper.BankMapper;
import com.example.questionbank.mapper.FavoriteMapper;
import com.example.questionbank.mapper.FollowMapper;
import com.example.questionbank.mapper.QuestionMapper;
import com.example.questionbank.mapper.UserMapper;
import com.example.questionbank.dto.request.UpdateUserRequest;
import com.example.questionbank.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BankMapper bankMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User getCurrentUserInfo(Long userId) {
        User user = userMapper.selectById(userId.intValue());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        QueryWrapper<Bank> bankWrapper = new QueryWrapper<>();
        bankWrapper.eq("author_id", user.getId());
        user.setBankCount(bankMapper.selectCount(bankWrapper));

        QueryWrapper<Question> questionWrapper = new QueryWrapper<>();
        questionWrapper.eq("author_id", user.getId());
        user.setQuestionCount(questionMapper.selectCount(questionWrapper));

        QueryWrapper<Favorite> favoriteWrapper = new QueryWrapper<>();
        favoriteWrapper.eq("user_id", user.getId());
        user.setFavoriteCount(favoriteMapper.selectCount(favoriteWrapper));

        QueryWrapper<Follow> followWrapper = new QueryWrapper<>();
        followWrapper.eq("follower_id", user.getId());
        user.setFollowingCount(followMapper.selectCount(followWrapper));

        user.setPassword(null);
        return user;
    }

    @Override
    public User updateUserInfo(Long userId, UpdateUserRequest request) {
        User user = userMapper.selectById(userId.intValue());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            User existing = userMapper.selectByUsername(request.getUsername().trim());
            if (existing != null && !existing.getId().equals(user.getId())) {
                throw new BusinessException("用户名已存在");
            }
            user.setUsername(request.getUsername().trim());
        }
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            User existing = userMapper.selectByEmail(request.getEmail().trim());
            if (existing != null && !existing.getId().equals(user.getId())) {
                throw new BusinessException("邮箱已存在");
            }
            user.setEmail(request.getEmail().trim());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public User updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId.intValue());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public User getUserById(Integer userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public IPage<User> getUsersForAdmin(Integer page, Integer size, String role, String status, String keyword) {
        Page<User> pager = new Page<>(page, size);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        if (role != null && !role.trim().isEmpty()) {
            queryWrapper.eq("role", role);
        }

        if (status != null && !status.trim().isEmpty()) {
            queryWrapper.eq("status", status);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.like("username", keyword)
                    .or()
                    .like("email", keyword);
        }

        IPage<User> result = userMapper.selectPage(pager, queryWrapper);
        for (User user : result.getRecords()) {
            QueryWrapper<Bank> bw = new QueryWrapper<>();
            bw.eq("author_id", user.getId());
            user.setBankCount(bankMapper.selectCount(bw));

            QueryWrapper<Question> qw = new QueryWrapper<>();
            qw.eq("author_id", user.getId());
            user.setQuestionCount(questionMapper.selectCount(qw));
        }
        return result;
    }

    @Override
    public User updateUserStatus(Integer userId, String status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        user.setStatus(status);
        userMapper.updateById(user);
        return user;
    }

    @Override
    public User updateUserRole(Integer userId, String role) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setRole(role);
        userMapper.updateById(user);
        return user;
    }
}