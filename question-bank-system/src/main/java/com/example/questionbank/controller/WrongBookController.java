package com.example.questionbank.controller;

import com.example.questionbank.common.PageResult;
import com.example.questionbank.common.Result;
import com.example.questionbank.dto.response.WrongQuestionVO;
import com.example.questionbank.security.JwtUtil;
import com.example.questionbank.service.PracticeService;
import com.example.questionbank.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "错题本模块")
@RestController
@RequestMapping("/user/wrong-questions")
public class WrongBookController {

    @Autowired
    private PracticeService practiceService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private JwtUtil jwtUtil;

    // ----------------------------------------------------------------
    // 列表（扩展：keyword / sort / mastered / starred）
    // ----------------------------------------------------------------

    @Operation(summary = "获取错题列表（分页+筛选+搜索+排序）")
    @GetMapping
    public Result<PageResult<WrongQuestionVO>> getWrongQuestions(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) Integer bankId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Boolean mastered,
            @RequestParam(required = false) Boolean starred,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "15") Integer size) {
        Integer userId = extractUserId(authHeader);
        PageResult<WrongQuestionVO> result = reviewService.getWrongQuestions(
                userId, type, difficulty, bankId, keyword, sort, mastered, starred, page, size);
        return Result.success(result);
    }

    // ----------------------------------------------------------------
    // 单条详情
    // ----------------------------------------------------------------

    @Operation(summary = "获取单条错题详情（含完整题目信息）")
    @GetMapping("/{id}")
    public Result<WrongQuestionVO> getDetail(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        Integer userId = extractUserId(authHeader);
        return Result.success(reviewService.getWrongQuestionDetail(id, userId));
    }

    // ----------------------------------------------------------------
    // 从错题本移除
    // ----------------------------------------------------------------

    @Operation(summary = "从错题本移除题目")
    @DeleteMapping("/remove/{questionId}")
    public Result<String> removeWrongQuestion(
            @PathVariable Integer questionId,
            @RequestHeader("Authorization") String authHeader) {
        Integer userId = extractUserId(authHeader);
        practiceService.removeWrongQuestion(userId, questionId);
        return Result.success("已从错题本移除");
    }

    // ----------------------------------------------------------------
    // 掌握 / 收藏 / 错因
    // ----------------------------------------------------------------

    @Operation(summary = "标记/取消掌握")
    @PatchMapping("/{id}/mastered")
    public Result<String> toggleMastered(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        Integer userId = extractUserId(authHeader);
        reviewService.toggleMastered(id, userId);
        return Result.success("操作成功");
    }

    @Operation(summary = "收藏/取消收藏")
    @PatchMapping("/{id}/starred")
    public Result<String> toggleStarred(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        Integer userId = extractUserId(authHeader);
        reviewService.toggleStarred(id, userId);
        return Result.success("操作成功");
    }

    @Operation(summary = "设置错因")
    @PatchMapping("/{id}/reason")
    public Result<String> setReason(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String reason) {
        Integer userId = extractUserId(authHeader);
        reviewService.setErrorReason(id, userId, reason);
        return Result.success("操作成功");
    }

    // ----------------------------------------------------------------
    // 今日复习（艾宾浩斯）
    // ----------------------------------------------------------------

    @Operation(summary = "获取今日待复习题目")
    @GetMapping("/today-review")
    public Result<List<WrongQuestionVO>> getTodayReview(
            @RequestHeader("Authorization") String authHeader) {
        Integer userId = extractUserId(authHeader);
        return Result.success(reviewService.getTodayReview(userId));
    }

    @Operation(summary = "提交复习结果（打卡）")
    @PostMapping("/{id}/review")
    public Result<String> submitReview(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Boolean result) {
        Integer userId = extractUserId(authHeader);
        reviewService.submitReview(id, userId, result);
        return Result.success("复习打卡成功");
    }

    // ----------------------------------------------------------------
    // 热力图
    // ----------------------------------------------------------------

    @Operation(summary = "获取复习热力图数据（近 N 天）")
    @GetMapping("/heatmap")
    public Result<List<Map<String, Object>>> getHeatmap(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "90") int days) {
        Integer userId = extractUserId(authHeader);
        return Result.success(reviewService.getHeatmap(userId, days));
    }

    // ----------------------------------------------------------------
    // 统计
    // ----------------------------------------------------------------

    @Operation(summary = "错题统计分析")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(
            @RequestHeader("Authorization") String authHeader) {
        Integer userId = extractUserId(authHeader);
        Map<String, Object> stats = practiceService.getWrongQuestionStats(userId);
        return Result.success(stats);
    }

    // ----------------------------------------------------------------
    // 私有辅助
    // ----------------------------------------------------------------

    private Integer extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.getUserIdFromToken(token).intValue();
    }
}
