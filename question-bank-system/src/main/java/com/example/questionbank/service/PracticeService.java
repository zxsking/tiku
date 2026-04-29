package com.example.questionbank.service;

import com.example.questionbank.common.PageResult;
import com.example.questionbank.dto.request.CreateSessionRequest;
import com.example.questionbank.dto.request.SubmitSessionRequest;
import com.example.questionbank.dto.response.SessionDetailVO;
import com.example.questionbank.dto.response.WrongQuestionVO;

import java.util.List;
import java.util.Map;

public interface PracticeService {

    /** 创建答题会话，返回会话详情（含题目列表，不含答案） */
    SessionDetailVO createSession(CreateSessionRequest request, Integer userId);

    /** 获取当前用户历史答题记录（分页） */
    PageResult<SessionDetailVO> getSessions(Integer userId, Integer page, Integer size);

    /** 获取会话详情（已提交则含答案和解析） */
    SessionDetailVO getSession(Long sessionId, Integer userId);

    /** 提交答案，判分，写入错题本，返回完整结果 */
    SessionDetailVO submitSession(Long sessionId, SubmitSessionRequest request, Integer userId);

    // ---- 错题本 ----

    /** 获取错题列表（分页+筛选） */
    PageResult<WrongQuestionVO> getWrongQuestions(Integer userId, String type, String difficulty,
                                                   Integer bankId, Integer page, Integer size);

    /** 从错题本移除 */
    void removeWrongQuestion(Integer userId, Integer questionId);

    /** 错题统计：按题型、难度、题库分组 */
    Map<String, Object> getWrongQuestionStats(Integer userId);
}
