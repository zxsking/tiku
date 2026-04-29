package com.example.questionbank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.questionbank.entity.ReviewRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface ReviewRecordMapper extends BaseMapper<ReviewRecord> {

    /**
     * 统计近 N 天每天的复习次数（用于热力图）
     */
    @Select("SELECT DATE(reviewed_at) as date, COUNT(*) as count " +
            "FROM review_records " +
            "WHERE user_id = #{userId} " +
            "  AND reviewed_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(reviewed_at) " +
            "ORDER BY date ASC")
    List<Map<String, Object>> countByDay(@Param("userId") Integer userId, @Param("days") int days);

    /**
     * 统计今日已复习题目数
     */
    @Select("SELECT COUNT(DISTINCT question_id) FROM review_records " +
            "WHERE user_id = #{userId} AND DATE(reviewed_at) = CURDATE()")
    int countTodayReviewed(@Param("userId") Integer userId);
}
