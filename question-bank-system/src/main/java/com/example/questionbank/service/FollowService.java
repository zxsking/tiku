package com.example.questionbank.service;


import com.example.questionbank.entity.User;

import java.util.List;

public interface FollowService {
    boolean toggleFollow(Integer followerId, Integer followingId);
    boolean isFollowing(Integer followerId, Integer followingId);
    List<User> getFollowingUsers(Integer followerId);
}