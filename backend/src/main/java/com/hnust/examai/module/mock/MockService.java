package com.hnust.examai.module.mock;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnust.examai.common.exception.BizException;
import com.hnust.examai.common.result.ResultCode;
import com.hnust.examai.entity.MockExam;
import com.hnust.examai.entity.MockExamQuestion;
import com.hnust.examai.entity.Question;
import com.hnust.examai.module.mock.dto.*;
import com.hnust.examai.module.quiz.QuestionMapper;
import com.hnust.examai.module.quiz.dto.GenerateRequest;
import com.hnust.examai.module.quiz.dto.QuestionVO;
import com.hnust.examai.module.quiz.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模考 Service
 * <p>
 * 核心规则（TDR-006）：服务端超时检测为准，前端倒计时仅展示
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockService {

    private final MockExamMapper mockExamMapper;
    private final MockExamQuestionMapper mockExamQuestionMapper;
    private final QuestionMapper questionMapper;
    private final QuizService quizService;
    private final ObjectMapper objectMapper;

    /**
     * 开始模考：生成题目 → 创建模考记录 → 写题目明细
     */
    @Transactional
    public Long start(Long userId, MockStartRequest req) {
        // 不允许同时有两场进行中
        long inProgressCount = mockExamMapper.selectCount(
                new LambdaQueryWrapper<MockExam>()
                        .eq(MockExam::getUserId, userId)
                        .eq(MockExam::getStatus, 0));
        if (inProgressCount > 0) {
            throw new BizException(ResultCode.MOCK_EXAM_IN_PROGRESS);
        }

        // 复用 QuizService 缓存逻辑出题
        GenerateRequest genReq = new GenerateRequest();
        genReq.setSubjectId(req.getSubjectId());
        genReq.setCount(req.getCount());
        genReq.setDifficulty(req.getDifficulty() != null ? req.getDifficulty() : 2);
        List<QuestionVO> questionVOs = quizService.generate(userId, genReq);

        // 查询历史模考数量，生成标题
        long historyCount = mockExamMapper.selectCount(
                new LambdaQueryWrapper<MockExam>().eq(MockExam::getUserId, userId));
        String title = "模考练习 #" + (historyCount + 1);

        // 创建模考记录
        MockExam exam = new MockExam();
        exam.setUserId(userId);
        exam.setSubjectId(req.getSubjectId());
        exam.setTitle(title);
        exam.setTimeLimitMin(req.getTimeLimitMinutes());
        exam.setTotalQuestions(questionVOs.size());
        exam.setTotalCorrect(0);
        exam.setStatus(0);
        exam.setStartedAt(LocalDateTime.now());
        mockExamMapper.insert(exam);

        // 写题目明细
        for (int i = 0; i < questionVOs.size(); i++) {
            MockExamQuestion meq = new MockExamQuestion();
            meq.setExamId(exam.getId());
            meq.setQuestionId(questionVOs.get(i).getId());
            meq.setSortOrder(i + 1);
            meq.setIsFlagged(0);
            mockExamQuestionMapper.insert(meq);
        }

        log.info("用户 {} 开始模考，examId={}, 题数={}", userId, exam.getId(), questionVOs.size());
        return exam.getId();
    }

    /**
     * 获取进行中模考（服务端检测超时，返回题目列表但不含答案）
     */
    public MockExamVO getExam(Long userId, Long examId) {
        MockExam exam = getAndValidateExam(userId, examId);

        // 服务端超时检测
        if (exam.getStatus() == 0) {
            LocalDateTime deadline = exam.getStartedAt().plusMinutes(exam.getTimeLimitMin());
            if (LocalDateTime.now().isAfter(deadline)) {
                // 自动置超时
                mockExamMapper.update(null,
                        new LambdaUpdateWrapper<MockExam>()
                                .set(MockExam::getStatus, 2)
                                .eq(MockExam::getId, examId));
                exam.setStatus(2);
            }
        }

        List<MockExamQuestion> details = mockExamQuestionMapper.selectByExamId(examId);
        List<Long> questionIds = details.stream().map(MockExamQuestion::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> questionMap = questionIds.isEmpty() ? Collections.emptyMap()
                : questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 计算剩余秒数
        long remainingSeconds = 0;
        if (exam.getStatus() == 0) {
            LocalDateTime deadline = exam.getStartedAt().plusMinutes(exam.getTimeLimitMin());
            remainingSeconds = Math.max(0, ChronoUnit.SECONDS.between(LocalDateTime.now(), deadline));
        }

        MockExamVO vo = new MockExamVO();
        vo.setExamId(exam.getId());
        vo.setTitle(exam.getTitle());
        vo.setTotalQuestions(exam.getTotalQuestions());
        vo.setRemainingSeconds(remainingSeconds);
        vo.setStatus(exam.getStatus());

        List<MockExamVO.MockQuestionItem> items = details.stream().map(detail -> {
            Question q = questionMap.get(detail.getQuestionId());
            MockExamVO.MockQuestionItem item = new MockExamVO.MockQuestionItem();
            item.setQuestionId(detail.getQuestionId());
            item.setSortOrder(detail.getSortOrder());
            item.setFlagged(detail.getIsFlagged() == 1);
            item.setUserAnswer(detail.getUserAnswer());
            if (q != null) {
                item.setContent(q.getContent());
                item.setType(q.getType());
                item.setDifficulty(q.getDifficulty());
                item.setOptions(parseOptions(q.getOptions()));
            }
            return item;
        }).collect(Collectors.toList());

        vo.setQuestions(items);
        return vo;
    }

    /**
     * 切换题目标记状态
     */
    public void flag(Long userId, Long examId, FlagRequest req) {
        MockExam exam = getAndValidateExam(userId, examId);
        assertInProgress(exam);

        mockExamQuestionMapper.update(null,
                new LambdaUpdateWrapper<MockExamQuestion>()
                        .set(MockExamQuestion::getIsFlagged, req.getFlagged() ? 1 : 0)
                        .eq(MockExamQuestion::getExamId, examId)
                        .eq(MockExamQuestion::getQuestionId, req.getQuestionId()));
    }

    /**
     * 交卷：判分 + 写结果 + 更新模考记录
     */
    @Transactional
    public MockReportVO submit(Long userId, Long examId, MockSubmitRequest req) {
        MockExam exam = getAndValidateExam(userId, examId);
        assertInProgress(exam);

        List<MockExamQuestion> details = mockExamQuestionMapper.selectByExamId(examId);
        List<Long> questionIds = details.stream().map(MockExamQuestion::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> questionMap = questionIds.isEmpty() ? Collections.emptyMap()
                : questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 构建用户答案映射
        Map<Long, String> answerMap = req.getAnswers().stream()
                .filter(a -> a.getQuestionId() != null)
                .collect(Collectors.toMap(MockSubmitRequest.AnswerItem::getQuestionId,
                        a -> a.getAnswer() != null ? a.getAnswer() : "",
                        (a, b) -> a));

        int correctCount = 0;

        for (MockExamQuestion detail : details) {
            Question q = questionMap.get(detail.getQuestionId());
            String userAns = answerMap.getOrDefault(detail.getQuestionId(), "");
            boolean correct = q != null && judge(q, userAns);
            if (correct) correctCount++;

            // 写回答案和判分结果
            mockExamQuestionMapper.update(null,
                    new LambdaUpdateWrapper<MockExamQuestion>()
                            .set(MockExamQuestion::getUserAnswer, userAns.isEmpty() ? null : userAns)
                            .set(MockExamQuestion::getIsCorrect, correct ? 1 : 0)
                            .eq(MockExamQuestion::getId, detail.getId()));
        }

        // 更新模考记录
        LocalDateTime now = LocalDateTime.now();
        mockExamMapper.update(null,
                new LambdaUpdateWrapper<MockExam>()
                        .set(MockExam::getStatus, 1)
                        .set(MockExam::getTotalCorrect, correctCount)
                        .set(MockExam::getSubmittedAt, now)
                        .eq(MockExam::getId, examId));

        exam.setStatus(1);
        exam.setTotalCorrect(correctCount);
        exam.setSubmittedAt(now);

        log.info("用户 {} 完成模考 examId={}，正确 {}/{}", userId, examId, correctCount, details.size());
        return buildReport(exam, details, questionMap);
    }

    /**
     * 获取模考报告（含完整答案和解析）
     */
    public MockReportVO getReport(Long userId, Long examId) {
        MockExam exam = getAndValidateExam(userId, examId);
        if (exam.getStatus() == 0) {
            throw new BizException(ResultCode.MOCK_EXAM_IN_PROGRESS, "模考未结束，无法查看报告");
        }

        List<MockExamQuestion> details = mockExamQuestionMapper.selectByExamId(examId);
        List<Long> questionIds = details.stream().map(MockExamQuestion::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> questionMap = questionIds.isEmpty() ? Collections.emptyMap()
                : questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        return buildReport(exam, details, questionMap);
    }

    /**
     * 查询历史模考列表（已完成/超时）
     */
    public List<MockHistoryVO> listHistory(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        List<MockExam> list = mockExamMapper.selectHistoryByUserId(userId, offset, size);
        return list.stream().map(this::toHistoryVO).collect(Collectors.toList());
    }

    // ===== 定时任务 =====

    /**
     * 每 5 分钟扫描一次超时模考，批量置为 status=2
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void scanTimeoutExams() {
        int count = mockExamMapper.batchMarkTimeout(LocalDateTime.now());
        if (count > 0) {
            log.info("超时模考批量处理：{} 条记录置为超时", count);
        }
    }

    // ===== 私有方法 =====

    private MockExam getAndValidateExam(Long userId, Long examId) {
        MockExam exam = mockExamMapper.selectById(examId);
        if (exam == null) {
            throw new BizException(ResultCode.MOCK_EXAM_NOT_FOUND);
        }
        if (!exam.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return exam;
    }

    private void assertInProgress(MockExam exam) {
        if (exam.getStatus() == 1) {
            throw new BizException(ResultCode.MOCK_EXAM_SUBMITTED);
        }
        if (exam.getStatus() == 2) {
            throw new BizException(ResultCode.MOCK_EXAM_TIMEOUT);
        }
    }

    private boolean judge(Question q, String userAnswer) {
        if (userAnswer == null || userAnswer.isBlank() || q.getAnswer() == null) return false;
        return q.getAnswer().trim().equalsIgnoreCase(userAnswer.trim());
    }

    private List<MockExamVO.OptionItem> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) return Collections.emptyList();
        try {
            List<Map<String, String>> raw = objectMapper.readValue(optionsJson, new TypeReference<>() {});
            return raw.stream().map(m -> {
                MockExamVO.OptionItem item = new MockExamVO.OptionItem();
                item.setKey(m.get("key"));
                item.setValue(m.get("value"));
                return item;
            }).collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private List<MockReportVO.OptionItem> parseReportOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) return Collections.emptyList();
        try {
            List<Map<String, String>> raw = objectMapper.readValue(optionsJson, new TypeReference<>() {});
            return raw.stream().map(m -> {
                MockReportVO.OptionItem item = new MockReportVO.OptionItem();
                item.setKey(m.get("key"));
                item.setValue(m.get("value"));
                return item;
            }).collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private MockReportVO buildReport(MockExam exam, List<MockExamQuestion> details,
                                     Map<Long, Question> questionMap) {
        MockReportVO report = new MockReportVO();
        report.setExamId(exam.getId());
        report.setTitle(exam.getTitle());
        report.setTotalQuestions(exam.getTotalQuestions());
        report.setTotalCorrect(exam.getTotalCorrect());
        report.setAccuracy(exam.getTotalQuestions() > 0
                ? (double) exam.getTotalCorrect() / exam.getTotalQuestions() : 0);
        report.setStatus(exam.getStatus());

        if (exam.getSubmittedAt() != null) {
            report.setDurationSeconds(ChronoUnit.SECONDS.between(exam.getStartedAt(), exam.getSubmittedAt()));
        } else {
            report.setDurationSeconds((long) exam.getTimeLimitMin() * 60);
        }

        List<MockReportVO.QuestionResultItem> items = details.stream().map(detail -> {
            Question q = questionMap.get(detail.getQuestionId());
            MockReportVO.QuestionResultItem item = new MockReportVO.QuestionResultItem();
            item.setQuestionId(detail.getQuestionId());
            item.setSortOrder(detail.getSortOrder());
            item.setUserAnswer(detail.getUserAnswer());
            item.setCorrect(detail.getIsCorrect() != null && detail.getIsCorrect() == 1);
            if (q != null) {
                item.setContent(q.getContent());
                item.setType(q.getType());
                item.setOptions(parseReportOptions(q.getOptions()));
                item.setCorrectAnswer(q.getAnswer());
                item.setExplanation(q.getExplanation());
            }
            return item;
        }).collect(Collectors.toList());

        report.setQuestions(items);
        return report;
    }

    private MockHistoryVO toHistoryVO(MockExam exam) {
        MockHistoryVO vo = new MockHistoryVO();
        vo.setExamId(exam.getId());
        vo.setTitle(exam.getTitle());
        vo.setTotalQuestions(exam.getTotalQuestions());
        vo.setTotalCorrect(exam.getTotalCorrect());
        vo.setAccuracy(exam.getTotalQuestions() > 0
                ? (double) exam.getTotalCorrect() / exam.getTotalQuestions() : 0);
        vo.setTimeLimitMin(exam.getTimeLimitMin());
        vo.setStatus(exam.getStatus());
        vo.setStartedAt(exam.getStartedAt());
        vo.setSubmittedAt(exam.getSubmittedAt());
        return vo;
    }
}
