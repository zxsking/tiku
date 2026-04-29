package com.example.questionbank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.questionbank.common.PageResult;
import com.example.questionbank.common.exception.BusinessException;
import com.example.questionbank.dto.response.WrongQuestionVO;
import com.example.questionbank.entity.Bank;
import com.example.questionbank.entity.Question;
import com.example.questionbank.entity.ReviewRecord;
import com.example.questionbank.entity.WrongQuestion;
import com.example.questionbank.mapper.BankMapper;
import com.example.questionbank.mapper.QuestionMapper;
import com.example.questionbank.mapper.ReviewRecordMapper;
import com.example.questionbank.mapper.WrongQuestionMapper;
import com.example.questionbank.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    // 艾宾浩斯复习间隔（天），索引 = 已复习次数
    private static final int[] REVIEW_INTERVALS = {1, 2, 4, 7, 15, 30};

    @Autowired
    private WrongQuestionMapper wrongQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private BankMapper bankMapper;

    @Autowired
    private ReviewRecordMapper reviewRecordMapper;

    // ================================================================
    // 错题详情
    // ================================================================

    @Override
    @Transactional(readOnly = true)
    public WrongQuestionVO getWrongQuestionDetail(Long id, Integer userId) {
        WrongQuestion wq = wrongQuestionMapper.selectById(id);
        if (wq == null || !wq.getUserId().equals(userId)) {
            throw new BusinessException("错题记录不存在");
        }
        Question q = questionMapper.selectById(wq.getQuestionId());
        if (q == null) {
            throw new BusinessException("题目不存在");
        }
        return buildVO(wq, q, true);
    }

    // ================================================================
    // 掌握 / 收藏 / 错因
    // ================================================================

    @Override
    public void toggleMastered(Long id, Integer userId) {
        WrongQuestion wq = getOwnedRecord(id, userId);
        wq.setMastered(wq.getMastered() == null || !wq.getMastered());
        wrongQuestionMapper.updateById(wq);
    }

    @Override
    public void toggleStarred(Long id, Integer userId) {
        WrongQuestion wq = getOwnedRecord(id, userId);
        wq.setStarred(wq.getStarred() == null || !wq.getStarred());
        wrongQuestionMapper.updateById(wq);
    }

    @Override
    public void setErrorReason(Long id, Integer userId, String reason) {
        WrongQuestion wq = getOwnedRecord(id, userId);
        wq.setErrorReason(reason);
        wrongQuestionMapper.updateById(wq);
    }

    // ================================================================
    // 今日复习（艾宾浩斯）
    // ================================================================

    @Override
    @Transactional(readOnly = true)
    public List<WrongQuestionVO> getTodayReview(Integer userId) {
        // 查询 next_review_at <= NOW() 且未掌握的错题
        QueryWrapper<WrongQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .and(w -> w.isNull("next_review_at")
                          .or().le("next_review_at", LocalDateTime.now()))
               .eq("mastered", false)
               .orderByAsc("next_review_at")
               .last("LIMIT 20");

        List<WrongQuestion> list = wrongQuestionMapper.selectList(wrapper);
        if (list.isEmpty()) return Collections.emptyList();

        List<Integer> qIds = list.stream().map(WrongQuestion::getQuestionId).collect(Collectors.toList());
        Map<Integer, Question> qMap = questionMapper.selectBatchIds(qIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        return list.stream()
                .filter(wq -> qMap.containsKey(wq.getQuestionId()))
                .map(wq -> buildVO(wq, qMap.get(wq.getQuestionId()), true))
                .collect(Collectors.toList());
    }

    // ================================================================
    // 复习打卡
    // ================================================================

    @Override
    public void submitReview(Long id, Integer userId, Boolean result) {
        WrongQuestion wq = getOwnedRecord(id, userId);

        // 插入打卡记录
        ReviewRecord record = new ReviewRecord();
        record.setUserId(userId);
        record.setQuestionId(wq.getQuestionId());
        record.setReviewedAt(LocalDateTime.now());
        record.setResult(result);
        reviewRecordMapper.insert(record);

        // 更新复习次数和下次复习时间（艾宾浩斯）
        int reviewCount = wq.getReviewCount() == null ? 0 : wq.getReviewCount();
        reviewCount++;
        wq.setReviewCount(reviewCount);

        int intervalDays = REVIEW_INTERVALS[Math.min(reviewCount, REVIEW_INTERVALS.length - 1)];
        wq.setNextReviewAt(LocalDateTime.now().plusDays(intervalDays));

        // 若答对则标记已掌握（连续复习 3 次以上且答对）
        if (Boolean.TRUE.equals(result) && reviewCount >= 3) {
            wq.setMastered(true);
        }

        wrongQuestionMapper.updateById(wq);
    }

    // ================================================================
    // 热力图
    // ================================================================

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHeatmap(Integer userId, int days) {
        return reviewRecordMapper.countByDay(userId, days);
    }

    // ================================================================
    // 扩展错题列表查询
    // ================================================================

    @Override
    @Transactional(readOnly = true)
    public PageResult<WrongQuestionVO> getWrongQuestions(
            Integer userId, String type, String difficulty, Integer bankId,
            String keyword, String sort, Boolean mastered, Boolean starred,
            Integer page, Integer size) {

        // 先分页查错题本
        Page<WrongQuestion> pageParam = new Page<>(page, size);
        QueryWrapper<WrongQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);

        if (mastered != null) wrapper.eq("mastered", mastered);
        if (starred != null) wrapper.eq("starred", starred);

        // 排序
        if ("wrong_count".equals(sort)) {
            wrapper.orderByDesc("wrong_count");
        } else if ("next_review".equals(sort)) {
            wrapper.orderByAsc("next_review_at");
        } else {
            wrapper.orderByDesc("last_wrong_at");
        }

        Page<WrongQuestion> result = wrongQuestionMapper.selectPage(pageParam, wrapper);
        List<WrongQuestion> wrongList = result.getRecords();

        if (wrongList.isEmpty()) {
            return PageResult.of(0L, page, size, Collections.emptyList());
        }

        // 批量查题目详情
        List<Integer> qIds = wrongList.stream().map(WrongQuestion::getQuestionId).collect(Collectors.toList());

        QueryWrapper<Question> qWrapper = new QueryWrapper<>();
        qWrapper.in("id", qIds);
        if (type != null && !type.isEmpty()) qWrapper.eq("type", type);
        if (difficulty != null && !difficulty.isEmpty()) qWrapper.eq("difficulty", difficulty);
        if (bankId != null) qWrapper.eq("bank_id", bankId);
        if (keyword != null && !keyword.isEmpty()) qWrapper.like("content", keyword);

        List<Question> questions = questionMapper.selectList(qWrapper);
        Map<Integer, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 查题库名称
        Set<Integer> bankIds = questions.stream()
                .map(Question::getBankId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Integer, String> bankNameMap = new HashMap<>();
        if (!bankIds.isEmpty()) {
            bankMapper.selectBatchIds(bankIds).forEach(b -> bankNameMap.put(b.getId(), b.getName()));
        }

        List<WrongQuestionVO> vos = wrongList.stream()
                .filter(wq -> questionMap.containsKey(wq.getQuestionId()))
                .map(wq -> {
                    Question q = questionMap.get(wq.getQuestionId());
                    WrongQuestionVO vo = buildVO(wq, q, false);
                    vo.setBankName(bankNameMap.get(q.getBankId()));
                    return vo;
                })
                .collect(Collectors.toList());

        // 关键词过滤后总数需要重新计算（因为是内存过滤）
        long total = (keyword != null && !keyword.isEmpty()) ? vos.size() : result.getTotal();
        return PageResult.of(total, page, size, vos);
    }

    // ================================================================
    // 私有辅助
    // ================================================================

    private WrongQuestion getOwnedRecord(Long id, Integer userId) {
        WrongQuestion wq = wrongQuestionMapper.selectById(id);
        if (wq == null || !wq.getUserId().equals(userId)) {
            throw new BusinessException("错题记录不存在");
        }
        return wq;
    }

    private WrongQuestionVO buildVO(WrongQuestion wq, Question q, boolean includeAnswer) {
        WrongQuestionVO vo = new WrongQuestionVO();
        vo.setId(wq.getId());
        vo.setQuestionId(wq.getQuestionId());
        vo.setUserId(wq.getUserId());
        vo.setWrongCount(wq.getWrongCount());
        vo.setLastWrongAt(wq.getLastWrongAt());
        vo.setMastered(wq.getMastered());
        vo.setStarred(wq.getStarred());
        vo.setErrorReason(wq.getErrorReason());
        vo.setNextReviewAt(wq.getNextReviewAt());
        vo.setReviewCount(wq.getReviewCount());
        vo.setType(q.getType());
        vo.setContent(q.getContent());
        vo.setOptions(q.getOptions());
        vo.setDifficulty(q.getDifficulty());
        vo.setBankId(q.getBankId());
        if (includeAnswer) {
            vo.setCorrectAnswer(q.getAnswer());
            vo.setAnalysis(q.getAnalysis());
        }
        return vo;
    }
}
