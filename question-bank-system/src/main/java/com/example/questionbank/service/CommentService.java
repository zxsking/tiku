package com.example.questionbank.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.questionbank.entity.Comment;

public interface CommentService {
    IPage<Comment> getCommentsByQuestionId(Integer questionId, Integer page, Integer size);
    Comment addComment(Integer questionId, Integer userId, String content, Integer parentId);
    void deleteComment(Integer questionId, Integer commentId, Integer userId, boolean isAdmin);
}