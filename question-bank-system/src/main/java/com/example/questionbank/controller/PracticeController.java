package com.example.questionbank.controller;

import com.example.questionbank.common.PageResult;
import com.example.questionbank.common.Result;
import com.example.questionbank.dto.request.CreateSessionRequest;
import com.example.questionbank.dto.request.SubmitSessionRequest;
import com.example.questionbank.dto.response.SessionDetailVO;
import com.example.questionbank.security.JwtUtil;
import com.example.questionbank.service.PracticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "练习/考试模块")
@RestController
@RequestMapping("/practice/sessions")
public class PracticeController {

    @Autowired
    private PracticeService practiceService;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(summary = "创建答题会话")
    @PostMapping
    public Result<SessionDetailVO> createSession(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateSessionRequest request) {
        Integer userId = extractUserId(authHeader);
        SessionDetailVO vo = practiceService.createSession(request, userId);
        return Result.success(vo);
    }

    @Operation(summary = "获取历史答题记录（分页）")
    @GetMapping
    public Result<PageResult<SessionDetailVO>> getSessions(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "15") Integer size) {
        Integer userId = extractUserId(authHeader);
        PageResult<SessionDetailVO> result = practiceService.getSessions(userId, page, size);
        return Result.success(result);
    }

    @Operation(summary = "获取会话详情")
    @GetMapping("/{id}")
    public Result<SessionDetailVO> getSession(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        Integer userId = extractUserId(authHeader);
        SessionDetailVO vo = practiceService.getSession(id, userId);
        return Result.success(vo);
    }

    @Operation(summary = "提交答案并判分")
    @PostMapping("/{id}/submit")
    public Result<SessionDetailVO> submitSession(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SubmitSessionRequest request) {
        Integer userId = extractUserId(authHeader);
        SessionDetailVO vo = practiceService.submitSession(id, request, userId);
        return Result.success(vo);
    }

    private Integer extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.getUserIdFromToken(token).intValue();
    }
}
