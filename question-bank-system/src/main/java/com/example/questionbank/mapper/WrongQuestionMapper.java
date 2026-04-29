package com.example.questionbank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.questionbank.entity.WrongQuestion;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface WrongQuestionMapper extends BaseMapper<WrongQuestion> {

    /**
     * 按题型统计错题数量
     */
    @Select("SELECT q.type, COUNT(*) as count " +
            "FROM wrong_questions wq " +
            "JOIN questions q ON wq.question_id = q.id " +
            "WHERE wq.user_id = #{userId} " +
            "GROUP BY q.type")
    List<Map<String, Object>> countByType(@Param("userId") Integer userId);

    /**
     * 按难度统计错题数量
     */
    @Select("SELECT q.difficulty, COUNT(*) as count " +
            "FROM wrong_questions wq " +
            "JOIN questions q ON wq.question_id = q.id " +
            "WHERE wq.user_id = #{userId} " +
            "GROUP BY q.difficulty")
    List<Map<String, Object>> countByDifficulty(@Param("userId") Integer userId);

    /**
     * 按题库统计错题数量
     */
    @Select("SELECT b.id as bankId, b.name as bankName, COUNT(*) as count " +
            "FROM wrong_questions wq " +
            "JOIN questions q ON wq.question_id = q.id " +
            "JOIN banks b ON q.bank_id = b.id " +
            "WHERE wq.user_id = #{userId} " +
            "GROUP BY b.id, b.name " +
            "ORDER BY count DESC " +
            "LIMIT 10")
    List<Map<String, Object>> countByBank(@Param("userId") Integer userId);
}
