package com.example.questionbank.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.questionbank.common.Result;
import com.example.questionbank.common.PageResult;
import com.example.questionbank.entity.Comment;
import com.example.questionbank.entity.User;
import com.example.questionbank.security.JwtUtil;
import com.example.questionbank.service.CommentService;
import com.example.questionbank.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "评论模块")
@RestController
@RequestMapping("/questions/{questionId}/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(summary = "获取题目评论列表")
    @GetMapping
    public Result<PageResult<Map<String, Object>>> getComments(
            @PathVariable Integer questionId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        IPage<Comment> result = commentService.getCommentsByQuestionId(questionId, page, size);
        List<Map<String, Object>> records = result.getRecords().stream().map(comment -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", comment.getId());
            map.put("content", comment.getContent());
            map.put("parentId", comment.getParentId());
            map.put("userId", comment.getUserId());
            map.put("createdAt", comment.getCreatedAt());
            try {
                User user = userService.getUserById(comment.getUserId());
                map.put("author", user != null ? user.getUsername() : "用户");
            } catch (Exception e) {
                map.put("author", "用户");
            }
            return map;
        }).collect(Collectors.toList());

        PageResult<Map<String, Object>> pageResult = PageResult.of(
                result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), records);
        return Result.success(pageResult);
    }

    @Operation(summary = "发表评论")
    @PostMapping
    public Result<Map<String, Object>> addComment(
            @PathVariable Integer questionId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {

        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);

        String content = body.get("content") == null ? "" : String.valueOf(body.get("content")).trim();
        if (content.isEmpty()) {
            return Result.error("评论内容不能为空");
        }
        if (content.length() > 200) {
            return Result.error("评论内容不能超过200字");
        }
        Integer parentId = body.get("parentId") != null ? (Integer) body.get("parentId") : null;

        Comment comment = commentService.addComment(questionId, userId.intValue(), content, parentId);

        Map<String, Object> result = new HashMap<>();
        result.put("id", comment.getId());
        result.put("content", comment.getContent());
        result.put("parentId", comment.getParentId());
        result.put("userId", comment.getUserId());
        result.put("createdAt", comment.getCreatedAt());
        try {
            User user = userService.getUserById(userId.intValue());
            result.put("author", user != null ? user.getUsername() : "用户");
        } catch (Exception e) {
            result.put("author", "用户");
        }

        return Result.success(result);
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{commentId}")
    public Result<String> deleteComment(
            @PathVariable Integer questionId,
            @PathVariable Integer commentId,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        User currentUser = userService.getUserById(userId.intValue());
        boolean isAdmin = currentUser != null && "admin".equalsIgnoreCase(currentUser.getRole());
        commentService.deleteComment(questionId, commentId, userId.intValue(), isAdmin);
        return Result.success("删除成功");
    }
}
