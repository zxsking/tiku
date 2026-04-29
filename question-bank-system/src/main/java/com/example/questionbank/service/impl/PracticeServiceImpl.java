package com.example.questionbank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.questionbank.common.PageResult;
import com.example.questionbank.common.exception.BusinessException;
import com.example.questionbank.dto.request.CreateSessionRequest;
import com.example.questionbank.dto.request.SubmitSessionRequest;
import com.example.questionbank.dto.response.SessionDetailVO;
import com.example.questionbank.dto.response.WrongQuestionVO;
import com.example.questionbank.entity.PracticeSession;
import com.example.questionbank.entity.Question;
import com.example.questionbank.entity.WrongQuestion;
import com.example.questionbank.mapper.BankMapper;
import com.example.questionbank.mapper.PracticeSessionMapper;
import com.example.questionbank.mapper.QuestionMapper;
import com.example.questionbank.mapper.WrongQuestionMapper;
import com.example.questionbank.service.PracticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PracticeServiceImpl implements PracticeService {

    @Autowired
    private PracticeSessionMapper sessionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private WrongQuestionMapper wrongQuestionMapper;

    @Autowired
    private BankMapper bankMapper;

    // ================================================================
    // 创建会话
    // ================================================================

    @Override
    public SessionDetailVO createSession(CreateSessionRequest request, Integer userId) {
        List<Integer> questionIds;

        if (request.getQuestionIds() != null && !request.getQuestionIds().isEmpty()) {
            // 直接指定题目（错题练习）
            questionIds = new ArrayList<>(request.getQuestionIds());
        } else {
            // 按条件查询题目
            QueryWrapper<Question> wrapper = new QueryWrapper<>();
            wrapper.eq("status", "published");

            if (request.getBankId() != null) {
                // 验证题库访问权限：公开已发布 或 作者本人
                com.example.questionbank.entity.Bank bank = bankMapper.selectById(request.getBankId());
                if (bank == null) {
                    throw new BusinessException("题库不存在");
                }
                boolean isPublished = "public".equals(bank.getVisibility()) && "published".equals(bank.getStatus());
                boolean isOwner = userId != null && userId.equals(bank.getAuthorId());
                if (!isPublished && !isOwner) {
                    throw new BusinessException("无权访问该题库");
                }
                wrapper.eq("bank_id", request.getBankId());
            }
            if (request.getTypes() != null && !request.getTypes().isEmpty()) {
                wrapper.in("type", request.getTypes());
            }
            if (request.getDifficulties() != null && !request.getDifficulties().isEmpty()) {
                wrapper.in("difficulty", request.getDifficulties());
            }

            List<Question> questions = questionMapper.selectList(wrapper);
            if (questions.isEmpty()) {
                throw new BusinessException("没有找到符合条件的题目，请调整筛选条件");
            }

            questionIds = questions.stream()
                    .map(Question::getId)
                    .collect(Collectors.toList());
        }

        // 随机打乱
        if (Boolean.TRUE.equals(request.getShuffle())) {
            Collections.shuffle(questionIds);
        }

        // 截取数量
        int count = request.getCount() != null ? request.getCount() : 20;
        if (questionIds.size() > count) {
            questionIds = questionIds.subList(0, count);
        }

        // 保存会话
        PracticeSession session = new PracticeSession();
        session.setUserId(userId);
        session.setBankId(request.getBankId());
        session.setQuestionIds(questionIds);
        session.setTimeLimit(request.getTimeLimit() != null ? request.getTimeLimit() : 0);
        session.setTotalScore(questionIds.size());
        session.setScore(0);
        session.setTimeUsed(0);
        session.setStatus(0);
        session.setCreatedAt(LocalDateTime.now());
        sessionMapper.insert(session);

        return buildSessionVO(session, false);
    }

    // ================================================================
    // 历史记录
    // ================================================================

    @Override
    @Transactional(readOnly = true)
    public PageResult<SessionDetailVO> getSessions(Integer userId, Integer page, Integer size) {
        Page<PracticeSession> pageParam = new Page<>(page, size);
        QueryWrapper<PracticeSession> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .orderByDesc("created_at");

        Page<PracticeSession> result = sessionMapper.selectPage(pageParam, wrapper);

        List<SessionDetailVO> vos = result.getRecords().stream()
                .map(s -> buildSessionVO(s, false))
                .collect(Collectors.toList());

        return PageResult.of(result.getTotal(), page, size, vos);
    }

    // ================================================================
    // 获取会话详情
    // ================================================================

    @Override
    @Transactional(readOnly = true)
    public SessionDetailVO getSession(Long sessionId, Integer userId) {
        PracticeSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("答题会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该答题会话");
        }
        return buildSessionVO(session, session.getStatus() == 1);
    }

    // ================================================================
    // 提交答案
    // ================================================================

    @Override
    public SessionDetailVO submitSession(Long sessionId, SubmitSessionRequest request, Integer userId) {
        PracticeSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("答题会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该答题会话");
        }
        if (session.getStatus() == 1) {
            throw new BusinessException("该会话已提交，请勿重复提交");
        }

        Map<String, Object> userAnswers = request.getUserAnswers() != null
                ? request.getUserAnswers()
                : new HashMap<>();

        // 查询所有题目
        List<Integer> questionIds = session.getQuestionIds();
        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        Map<Integer, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 判分
        int correctCount = 0;
        int wrongCount = 0;
        int skipCount = 0;
        List<Integer> wrongQuestionIds = new ArrayList<>();

        for (Integer qId : questionIds) {
            Question q = questionMap.get(qId);
            if (q == null) continue;

            Object userAnswer = userAnswers.get(String.valueOf(qId));
            boolean skipped = isSkipped(userAnswer);

            // 简答题不自动判分，有作答则算"已答"，不计入对错和错题本
            if ("essay".equals(q.getType())) {
                if (skipped) skipCount++;
                continue;
            }

            if (skipped) {
                skipCount++;
            } else {
                boolean correct = judgeAnswer(q, userAnswer);
                if (correct) {
                    correctCount++;
                } else {
                    wrongCount++;
                    wrongQuestionIds.add(qId);
                }
            }
        }

        // 写入错题本
        for (Integer qId : wrongQuestionIds) {
            upsertWrongQuestion(userId, qId);
        }

        // 更新会话
        session.setUserAnswers(userAnswers);
        session.setScore(correctCount);
        session.setTotalScore(questionIds.size());
        session.setTimeUsed(request.getTimeUsed() != null ? request.getTimeUsed() : 0);
        session.setStatus(1);
        session.setSubmittedAt(LocalDateTime.now());
        sessionMapper.updateById(session);

        return buildSessionVO(session, true);
    }

    // ================================================================
    // 错题本
    // ================================================================

    @Override
    @Transactional(readOnly = true)
    public PageResult<WrongQuestionVO> getWrongQuestions(Integer userId, String type, String difficulty,
                                                          Integer bankId, Integer page, Integer size) {
        // 先分页查错题本
        Page<WrongQuestion> pageParam = new Page<>(page, size);
        QueryWrapper<WrongQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .orderByDesc("last_wrong_at");

        Page<WrongQuestion> result = wrongQuestionMapper.selectPage(pageParam, wrapper);
        List<WrongQuestion> wrongList = result.getRecords();

        if (wrongList.isEmpty()) {
            return PageResult.of(0L, page, size, Collections.emptyList());
        }

        // 批量查题目详情
        List<Integer> qIds = wrongList.stream()
                .map(WrongQuestion::getQuestionId)
                .collect(Collectors.toList());

        QueryWrapper<Question> qWrapper = new QueryWrapper<>();
        qWrapper.in("id", qIds);
        if (type != null && !type.isEmpty()) {
            qWrapper.eq("type", type);
        }
        if (difficulty != null && !difficulty.isEmpty()) {
            qWrapper.eq("difficulty", difficulty);
        }
        if (bankId != null) {
            qWrapper.eq("bank_id", bankId);
        }

        List<Question> questions = questionMapper.selectList(qWrapper);
        Map<Integer, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 组装 VO
        List<WrongQuestionVO> vos = wrongList.stream()
                .filter(wq -> questionMap.containsKey(wq.getQuestionId()))
                .map(wq -> {
                    Question q = questionMap.get(wq.getQuestionId());
                    WrongQuestionVO vo = new WrongQuestionVO();
                    vo.setId(wq.getId());
                    vo.setQuestionId(wq.getQuestionId());
                    vo.setUserId(wq.getUserId());
                    vo.setWrongCount(wq.getWrongCount());
                    vo.setLastWrongAt(wq.getLastWrongAt());
                    vo.setType(q.getType());
                    vo.setContent(q.getContent());
                    vo.setOptions(q.getOptions());
                    vo.setDifficulty(q.getDifficulty());
                    vo.setBankId(q.getBankId());
                    return vo;
                })
                .collect(Collectors.toList());

        return PageResult.of(result.getTotal(), page, size, vos);
    }

    @Override
    public void removeWrongQuestion(Integer userId, Integer questionId) {
        QueryWrapper<WrongQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("question_id", questionId);
        int rows = wrongQuestionMapper.delete(wrapper);
        if (rows == 0) {
            throw new BusinessException("错题记录不存在");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getWrongQuestionStats(Integer userId) {
        long total = wrongQuestionMapper.selectCount(
                new QueryWrapper<WrongQuestion>().eq("user_id", userId));

        List<Map<String, Object>> byType = wrongQuestionMapper.countByType(userId);
        List<Map<String, Object>> byDifficulty = wrongQuestionMapper.countByDifficulty(userId);
        List<Map<String, Object>> byBank = wrongQuestionMapper.countByBank(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("byType", byType);
        stats.put("byDifficulty", byDifficulty);
        stats.put("byBank", byBank);
        return stats;
    }

    // ================================================================
    // 私有辅助方法
    // ================================================================

    /**
     * 构建会话 VO
     * @param includeAnswers 是否包含正确答案和解析（已提交时为 true）
     */
    private SessionDetailVO buildSessionVO(PracticeSession session, boolean includeAnswers) {
        SessionDetailVO vo = new SessionDetailVO();
        vo.setId(session.getId());
        vo.setUserId(session.getUserId());
        vo.setBankId(session.getBankId());
        vo.setTimeLimit(session.getTimeLimit());
        vo.setTimeUsed(session.getTimeUsed());
        vo.setStatus(session.getStatus());
        vo.setCreatedAt(session.getCreatedAt());
        vo.setSubmittedAt(session.getSubmittedAt());

        if (session.getStatus() == 1) {
            vo.setScore(session.getScore());
            vo.setTotalScore(session.getTotalScore());
            int total = session.getTotalScore();
            vo.setAccuracy(total > 0 ? (double) session.getScore() / total : 0.0);
        }

        // 查题目详情
        List<Integer> questionIds = session.getQuestionIds();
        if (questionIds == null || questionIds.isEmpty()) {
            vo.setQuestions(Collections.emptyList());
            return vo;
        }

        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        Map<Integer, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        Map<String, Object> userAnswers = session.getUserAnswers() != null
                ? session.getUserAnswers()
                : new HashMap<>();

        int correctCount = 0, wrongCount = 0, skipCount = 0;

        List<SessionDetailVO.QuestionResultItem> items = new ArrayList<>();
        for (Integer qId : questionIds) {
            Question q = questionMap.get(qId);
            if (q == null) continue;

            SessionDetailVO.QuestionResultItem item = new SessionDetailVO.QuestionResultItem();
            item.setId(q.getId());
            item.setType(q.getType());
            item.setContent(q.getContent());
            item.setOptions(q.getOptions());
            item.setDifficulty(q.getDifficulty());

            if (includeAnswers) {
                item.setCorrectAnswer(q.getAnswer());
                item.setAnalysis(q.getAnalysis());

                Object userAnswer = userAnswers.get(String.valueOf(qId));
                item.setUserAnswer(userAnswer);

                boolean skipped = isSkipped(userAnswer);
                if ("essay".equals(q.getType())) {
                    // 简答题不自动判分，isCorrect 设为 null 表示"待人工批改"
                    item.setIsCorrect(null);
                    if (skipped) skipCount++;
                } else if (skipped) {
                    item.setIsCorrect(false);
                    skipCount++;
                } else {
                    boolean correct = judgeAnswer(q, userAnswer);
                    item.setIsCorrect(correct);
                    if (correct) correctCount++;
                    else wrongCount++;
                }
            }

            items.add(item);
        }

        vo.setQuestions(items);

        if (includeAnswers) {
            vo.setCorrectCount(correctCount);
            vo.setWrongCount(wrongCount);
            vo.setSkipCount(skipCount);
        }

        return vo;
    }

    /**
     * 判断是否未作答
     */
    private boolean isSkipped(Object userAnswer) {
        if (userAnswer == null) return true;
        if (userAnswer instanceof String s) return s.trim().isEmpty();
        if (userAnswer instanceof List<?> list) return list.isEmpty();
        return false;
    }

    /**
     * 判断答案是否正确
     */
    @SuppressWarnings("unchecked")
    private boolean judgeAnswer(Question question, Object userAnswer) {
        if (isSkipped(userAnswer)) return false;

        String type = question.getType();

        // 简答题不自动判分
        if ("essay".equals(type)) return false;

        Object correctAnswer = question.getAnswer();

        if ("multiple".equals(type)) {
            // 多选题：Set 比对，顺序无关
            Set<String> userSet = toStringSet(userAnswer);
            Set<String> correctSet = toStringSet(correctAnswer);
            return userSet.equals(correctSet);
        } else if ("fill".equals(type)) {
            // 填空题：去除首尾空格后比对
            String userStr = toAnswerString(userAnswer).trim();
            String correctStr = toAnswerString(correctAnswer).trim();
            return userStr.equalsIgnoreCase(correctStr);
        } else {
            // 单选/判断：字符串比对（忽略大小写和空格）
            String userStr = toAnswerString(userAnswer);
            String correctStr = toAnswerString(correctAnswer);
            return userStr.equalsIgnoreCase(correctStr);
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> toStringSet(Object answer) {
        if (answer == null) return Collections.emptySet();
        if (answer instanceof List) {
            return ((List<?>) answer).stream()
                    .map(Object::toString)
                    .map(String::trim)
                    .collect(Collectors.toSet());
        }
        // 逗号分隔字符串
        return Arrays.stream(answer.toString().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private String toAnswerString(Object answer) {
        if (answer == null) return "";
        if (answer instanceof List) {
            return ((List<?>) answer).stream()
                    .map(Object::toString)
                    .map(String::trim)
                    .collect(Collectors.joining(","));
        }
        return answer.toString().trim();
    }

    /**
     * 错题本 upsert：存在则 wrong_count+1，不存在则插入
     */
    private void upsertWrongQuestion(Integer userId, Integer questionId) {
        QueryWrapper<WrongQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("question_id", questionId);
        WrongQuestion existing = wrongQuestionMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setWrongCount(existing.getWrongCount() + 1);
            existing.setLastWrongAt(LocalDateTime.now());
            wrongQuestionMapper.updateById(existing);
        } else {
            WrongQuestion wq = new WrongQuestion();
            wq.setUserId(userId);
            wq.setQuestionId(questionId);
            wq.setWrongCount(1);
            wq.setLastWrongAt(LocalDateTime.now());
            wrongQuestionMapper.insert(wq);
        }
    }
}
