package com.example.questionbank.controller;

import com.example.questionbank.common.Result;
import com.example.questionbank.entity.User;
import com.example.questionbank.security.JwtUtil;
import com.example.questionbank.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "关注模块")
@RestController
public class FollowController {

    @Autowired
    private FollowService followService;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(summary = "关注/取消关注用户")
    @PostMapping("/users/{id}/follow")
    public Result<Boolean> toggleFollow(@PathVariable Integer id,
                                        @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long followerId = jwtUtil.getUserIdFromToken(token);
        boolean followed = followService.toggleFollow(followerId.intValue(), id);
        return Result.success(followed);
    }

    @Operation(summary = "获取我的关注列表")
    @GetMapping("/user/following")
    public Result<List<User>> getFollowing(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long followerId = jwtUtil.getUserIdFromToken(token);
        List<User> users = followService.getFollowingUsers(followerId.intValue());
        return Result.success(users);
    }
}
