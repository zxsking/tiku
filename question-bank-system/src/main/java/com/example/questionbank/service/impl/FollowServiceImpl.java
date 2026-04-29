package com.example.questionbank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.questionbank.service.FollowService;
import com.example.questionbank.entity.Bank;
import com.example.questionbank.entity.Follow;
import com.example.questionbank.entity.Question;
import com.example.questionbank.entity.User;
import com.example.questionbank.mapper.BankMapper;
import com.example.questionbank.mapper.FollowMapper;
import com.example.questionbank.mapper.QuestionMapper;
import com.example.questionbank.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class FollowServiceImpl implements FollowService {

    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BankMapper bankMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Override
    public boolean toggleFollow(Integer followerId, Integer followingId) {
        QueryWrapper<Follow> wrapper = new QueryWrapper<>();
        wrapper.eq("follower_id", followerId)
                .eq("following_id", followingId);

        Follow existingFollow = followMapper.selectOne(wrapper);
        if (existingFollow == null) {
            // 添加关注
            Follow follow = new Follow();
            follow.setFollowerId(followerId);
            follow.setFollowingId(followingId);
            followMapper.insert(follow);
            return true;
        } else {
            // 取消关注
            followMapper.delete(wrapper);
            return false;
        }
    }

    @Override
    public boolean isFollowing(Integer followerId, Integer followingId) {
        QueryWrapper<Follow> wrapper = new QueryWrapper<>();
        wrapper.eq("follower_id", followerId)
                .eq("following_id", followingId);

        return followMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<User> getFollowingUsers(Integer followerId) {
        QueryWrapper<Follow> wrapper = new QueryWrapper<>();
        wrapper.eq("follower_id", followerId).orderByDesc("created_at");
        List<Follow> follows = followMapper.selectList(wrapper);
        if (follows.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> userIds = follows.stream().map(Follow::getFollowingId).collect(Collectors.toList());
        List<User> users = userMapper.selectBatchIds(userIds);
        users.forEach(user -> {
            user.setPassword(null);
            QueryWrapper<Bank> bankWrapper = new QueryWrapper<>();
            bankWrapper.eq("author_id", user.getId());
            user.setBankCount(bankMapper.selectCount(bankWrapper));

            QueryWrapper<Question> questionWrapper = new QueryWrapper<>();
            questionWrapper.eq("author_id", user.getId());
            user.setQuestionCount(questionMapper.selectCount(questionWrapper));
        });
        return users;
    }
}