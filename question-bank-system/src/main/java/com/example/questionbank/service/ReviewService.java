package com.example.questionbank.service;

import com.example.questionbank.dto.response.WrongQuestionVO;

import java.util.List;
import java.util.Map;

public interface ReviewService {

    /** 获取单条错题详情（含完整题目信息） */
    WrongQuestionVO getWrongQuestionDetail(Long id, Integer userId);

    /** 标记/取消掌握 */
    void toggleMastered(Long id, Integer userId);

    /** 收藏/取消收藏 */
    void toggleStarred(Long id, Integer userId);

    /** 设置错因 */
    void setErrorReason(Long id, Integer userId, String reason);

    /** 获取今日待复习题目（艾宾浩斯推送） */
    List<WrongQuestionVO> getTodayReview(Integer userId);

    /** 提交复习结果（打卡），更新艾宾浩斯下次复习时间 */
    void submitReview(Long id, Integer userId, Boolean result);

    /** 获取近 days 天复习热力图数据 */
    List<Map<String, Object>> getHeatmap(Integer userId, int days);

    /** 扩展错题列表查询（支持 keyword / sort / mastered / starred） */
    com.example.questionbank.common.PageResult<WrongQuestionVO> getWrongQuestions(
            Integer userId, String type, String difficulty, Integer bankId,
            String keyword, String sort, Boolean mastered, Boolean starred,
            Integer page, Integer size);
}
