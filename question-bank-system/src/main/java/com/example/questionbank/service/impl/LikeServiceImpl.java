package com.example.questionbank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.questionbank.service.LikeService;
import com.example.questionbank.entity.Like;
import com.example.questionbank.mapper.LikeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LikeServiceImpl implements LikeService {

    @Autowired
    private LikeMapper likeMapper;

    @Override
    public boolean toggleLike(Integer userId, Integer targetId, String targetType) {
        QueryWrapper<Like> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("target_type", targetType)
                .eq("target_id", targetId);

        Like existingLike = likeMapper.selectOne(wrapper);
        if (existingLike == null) {
            // 添加点赞
            Like like = new Like();
            like.setUserId(userId);
            like.setTargetType(targetType);
            like.setTargetId(targetId);
            likeMapper.insert(like);
            return true;
        } else {
            // 取消点赞
            likeMapper.delete(wrapper);
            return false;
        }
    }

    @Override
    public boolean isLiked(Integer userId, Integer targetId, String targetType) {
        QueryWrapper<Like> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("target_type", targetType)
                .eq("target_id", targetId);

        return likeMapper.selectCount(wrapper) > 0;
    }
}