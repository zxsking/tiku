package com.example.questionbank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.questionbank.service.CommentService;
import com.example.questionbank.service.UserService;
import com.example.questionbank.entity.Comment;
import com.example.questionbank.entity.User;
import com.example.questionbank.mapper.CommentMapper;
import com.example.questionbank.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private UserService userService;

    @Override
    public IPage<Comment> getCommentsByQuestionId(Integer questionId, Integer page, Integer size) {
        Page<Comment> pager = new Page<>(page, size);
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("question_id", questionId)
                .eq("status", "active")
                .orderByDesc("created_at");

        IPage<Comment> result = commentMapper.selectPage(pager, queryWrapper);
        // 填充评论者用户名和头像
        result.getRecords().forEach(c -> {
            try {
                User u = userService.getUserById(c.getUserId());
                if (u != null) {
                    c.setAuthor(u.getUsername());
                    c.setAvatar(u.getAvatar());
                }
            } catch (Exception ignored) {}
        });
        return result;
    }

    @Override
    public Comment addComment(Integer questionId, Integer userId, String content, Integer parentId) {
        Comment comment = new Comment();
        comment.setQuestionId(questionId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setStatus("active");

        commentMapper.insert(comment);
        // 填充用户名用于前端立即显示
        try {
            User u = userService.getUserById(userId);
            if (u != null) {
                comment.setAuthor(u.getUsername());
                comment.setAvatar(u.getAvatar());
            }
        } catch (Exception ignored) {}
        return comment;
    }

    @Override
    public void deleteComment(Integer questionId, Integer commentId, Integer userId, boolean isAdmin) {
        Comment target = commentMapper.selectById(commentId);
        if (target == null || !"active".equals(target.getStatus()) || !questionId.equals(target.getQuestionId())) {
            throw new BusinessException("评论不存在");
        }
        if (!isAdmin && !userId.equals(target.getUserId())) {
            throw new BusinessException("无权删除该评论");
        }

        target.setStatus("deleted");
        commentMapper.updateById(target);

        // 递归级联隐藏所有子孙回复，避免楼中楼出现孤立评论
        Set<Integer> allDescendantIds = new HashSet<>();
        List<Integer> currentLevel = new ArrayList<>();
        currentLevel.add(commentId);
        while (!currentLevel.isEmpty()) {
            QueryWrapper<Comment> childQuery = new QueryWrapper<>();
            childQuery.eq("question_id", questionId).in("parent_id", currentLevel).select("id");
            List<Object> childIdObjs = commentMapper.selectObjs(childQuery);
            if (childIdObjs == null || childIdObjs.isEmpty()) break;
            List<Integer> nextLevel = new ArrayList<>();
            for (Object idObj : childIdObjs) {
                Integer id = Integer.valueOf(String.valueOf(idObj));
                if (allDescendantIds.add(id)) nextLevel.add(id);
            }
            currentLevel = nextLevel;
        }

        if (!allDescendantIds.isEmpty()) {
            UpdateWrapper<Comment> replyUpdate = new UpdateWrapper<>();
            replyUpdate.eq("question_id", questionId)
                    .in("id", allDescendantIds)
                    .set("status", "deleted");
            commentMapper.update(null, replyUpdate);
        }
    }
}