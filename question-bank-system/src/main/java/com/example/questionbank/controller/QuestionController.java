package com.example.questionbank.controller;

import com.example.questionbank.common.Result;
import com.example.questionbank.common.PageResult;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.questionbank.security.JwtUtil;
import com.example.questionbank.service.QuestionService;
import com.example.questionbank.service.UserService;
import com.example.questionbank.entity.Question;
import com.example.questionbank.dto.request.CreateQuestionRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Tag(name = "题目模块")
@RestController
@RequestMapping("/questions")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Operation(summary = "分页查询题目")
    @GetMapping
    public Result<PageResult<Question>> getQuestions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer bankId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "latest") String sortBy,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 解析可选 token，允许题库作者查看自己非公开题库的题目
        Integer currentUserId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                currentUserId = jwtUtil.getUserIdFromToken(authHeader.replace("Bearer ", "")).intValue();
            } catch (Exception ignored) {}
        }

        IPage<Question> result = questionService.getQuestions(page, size, bankId, type, difficulty, keyword, currentUserId, sortBy);
        PageResult<Question> pageResult = PageResult.of(result.getTotal(), (int)result.getCurrent(), (int)result.getSize(), result.getRecords());
        return Result.success(pageResult);
    }

    @Operation(summary = "题目详情")
    @GetMapping("/{id}")
    public Result<Question> getQuestion(@PathVariable Integer id,
                                        @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 解析可选 token，获取 userId（未登录或 token 无效时为 null）
        Integer userId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.replace("Bearer ", "");
                userId = jwtUtil.getUserIdFromToken(token).intValue();
            } catch (Exception ignored) {
                // 忽略无效 token，按未登录处理
            }
        }

        Question question = questionService.getQuestionById(id, userId);
        questionService.updateViewCount(id);
        return Result.success(question);
    }

    @Operation(summary = "创建题目")
    @PostMapping
    public Result<Question> createQuestion(@RequestHeader("Authorization") String authHeader,
                                           @Valid @RequestBody CreateQuestionRequest request) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        Question question = questionService.createQuestion(request, userId.intValue());
        return Result.success(question);
    }

    @Operation(summary = "更新题目")
    @PutMapping("/{id}")
    public Result<Question> updateQuestion(@PathVariable Integer id,
                                           @RequestHeader("Authorization") String authHeader,
                                           @Valid @RequestBody CreateQuestionRequest request) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        Question question = questionService.updateQuestion(id, request, userId.intValue());
        return Result.success(question);
    }

    @Operation(summary = "删除题目")
    @DeleteMapping("/{id}")
    public Result<String> deleteQuestion(@PathVariable Integer id,
                                       @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        questionService.deleteQuestion(id, userId.intValue());
        return Result.success("题目删除成功");
    }

    @Operation(summary = "点赞/取消点赞题目")
    @PostMapping("/{id}/like")
    public Result<String> toggleLike(@PathVariable Integer id,
                                     @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        questionService.toggleLike(userId.intValue(), id, "question");
        return Result.success("操作成功");
    }

    @Operation(summary = "收藏/取消收藏题目")
    @PostMapping("/{id}/favorite")
    public Result<String> toggleFavorite(@PathVariable Integer id,
                                         @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        questionService.toggleFavorite(userId.intValue(), id, "question");
        return Result.success("操作成功");
    }
}