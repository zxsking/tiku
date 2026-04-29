package com.example.questionbank.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.questionbank.entity.Question;
import com.example.questionbank.dto.request.CreateQuestionRequest;

import java.util.concurrent.CompletableFuture;

public interface QuestionService {
    IPage<Question> getQuestions(Integer page, Integer size, Integer bankId, String type, String difficulty, String keyword);
    IPage<Question> getQuestions(Integer page, Integer size, Integer bankId, String type, String difficulty, String keyword, Integer currentUserId);
    /**
     * @param sortBy latest=按最后更新时间（含编辑）；hot/popular=按浏览量热度。与题目收藏数无关。
     */
    IPage<Question> getQuestions(Integer page, Integer size, Integer bankId, String type, String difficulty, String keyword, Integer currentUserId, String sortBy);
    Question getQuestionById(Integer id);
    Question getQuestionById(Integer id, Integer userId);
    Question createQuestion(CreateQuestionRequest request, Integer userId);
    Question updateQuestion(Integer id, CreateQuestionRequest request, Integer userId);
    void deleteQuestion(Integer id, Integer userId);
    CompletableFuture<Void> updateViewCount(Integer questionId);
    void toggleLike(Integer userId, Integer targetId, String targetType);
    void toggleFavorite(Integer userId, Integer targetId, String targetType);
    IPage<Question> getQuestionsForReview(Integer page, Integer size, String status);
    Question reviewQuestion(Integer id, String status);

    // 新增：查询当前用户的题目列表
    IPage<Question> getMyQuestions(Integer userId, Integer page, Integer size, String status, Integer bankId, String type, String difficulty, String keyword);
}