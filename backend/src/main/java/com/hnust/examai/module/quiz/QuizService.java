package com.hnust.examai.module.quiz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnust.examai.common.ai.DeepSeekClient;
import com.hnust.examai.common.exception.BizException;
import com.hnust.examai.common.result.ResultCode;
import com.hnust.examai.entity.Question;
import com.hnust.examai.entity.UserAnswer;
import com.hnust.examai.module.errorbook.ErrorBookMapper;
import com.hnust.examai.module.quiz.dto.GenerateRequest;
import com.hnust.examai.module.quiz.dto.QuestionVO;
import com.hnust.examai.module.quiz.dto.SubmitRequest;
import com.hnust.examai.module.quiz.dto.SubmitResult;
import com.hnust.examai.module.user.StudyStatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 刷题 Service
 * <p>
 * 核心逻辑：优先命中 Redis 缓存，miss 则调用 DeepSeek AI 出题，结果入库并缓存
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private static final String CACHE_KEY_PREFIX = "quiz:cache:";
    private static final long CACHE_TTL_HOURS = 24;

    private final DeepSeekClient deepSeekClient;
    private final QuestionMapper questionMapper;
    private final UserAnswerMapper userAnswerMapper;
    private final ErrorBookMapper errorBookMapper;
    private final StudyStatMapper studyStatMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * AI 出题（先查缓存，miss 则调用 DeepSeek）
     *
     * @return 题目视图列表（不含答案/解析，提交后才返回）
     */
    public List<QuestionVO> generate(Long userId, GenerateRequest req) {
        String cacheKey = buildCacheKey(req);

        // 1. 尝试命中缓存
        List<Long> cachedIds = getCachedQuestionIds(cacheKey);
        if (!CollectionUtils.isEmpty(cachedIds)) {
            log.debug("Quiz 缓存命中，key={}", cacheKey);
            List<Question> questions = questionMapper.selectBatchIds(cachedIds);
            return toVOList(questions, false);
        }

        // 2. 调用 DeepSeek AI 出题
        log.info("Quiz 缓存未命中，调用 AI 出题，userId={}, subjectId={}", userId, req.getSubjectId());
        List<Question> questions = generateByAI(req, cacheKey);

        // 3. 缓存题目 ID 列表
        List<Long> questionIds = questions.stream().map(Question::getId).collect(Collectors.toList());
        cacheQuestionIds(cacheKey, questionIds);

        return toVOList(questions, false);
    }

    /**
     * 提交答案，判分，写答题记录，错题写入错题本
     */
    public SubmitResult submit(Long userId, SubmitRequest req) {
        List<SubmitRequest.AnswerItem> answers = req.getAnswers();
        List<Long> questionIds = answers.stream()
                .map(SubmitRequest.AnswerItem::getQuestionId)
                .collect(Collectors.toList());

        // 查询题目（含答案）
        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        int correctCount = 0;
        List<SubmitResult.QuestionResult> results = new ArrayList<>();
        List<UserAnswer> toInsert = new ArrayList<>();

        for (SubmitRequest.AnswerItem item : answers) {
            Question q = questionMap.get(item.getQuestionId());
            if (q == null) {
                throw new BizException(ResultCode.QUESTION_NOT_FOUND);
            }

            boolean correct = judge(q, item.getAnswer());
            if (correct) correctCount++;

            // 构建答题记录
            UserAnswer record = new UserAnswer();
            record.setUserId(userId);
            record.setQuestionId(q.getId());
            record.setSessionId(req.getSessionId());
            record.setUserAnswer(item.getAnswer());
            record.setIsCorrect(correct ? 1 : 0);
            record.setTimeSpentS(item.getTimeSpentS());
            toInsert.add(record);

            // 错题录入（答错则写入错题本）
            if (!correct) {
                errorBookMapper.upsertError(userId, q.getId());
            }

            // 构建结果 VO
            SubmitResult.QuestionResult qr = new SubmitResult.QuestionResult();
            qr.setQuestionId(q.getId());
            qr.setUserAnswer(item.getAnswer());
            qr.setCorrectAnswer(q.getAnswer());
            qr.setCorrect(correct);
            qr.setExplanation(q.getExplanation());
            qr.setType(q.getType());
            qr.setContent(q.getContent());
            qr.setOptions(parseOptions(q.getOptions()));
            results.add(qr);
        }

        // 批量写入答题记录
        toInsert.forEach(userAnswerMapper::insert);

        // 异步更新学习统计
        updateStudyStat(userId, answers.size(), correctCount);

        SubmitResult result = new SubmitResult();
        result.setTotalCount(answers.size());
        result.setCorrectCount(correctCount);
        result.setAccuracy(answers.isEmpty() ? 0 : (double) correctCount / answers.size());
        result.setResults(results);

        log.info("用户 {} 提交答题，共 {} 题，正确 {} 题", userId, answers.size(), correctCount);
        return result;
    }

    // ===== 私有方法 =====

    /**
     * 构建 Redis 缓存 Key（基于参数 MD5）
     */
    private String buildCacheKey(GenerateRequest req) {
        String raw = req.getSubjectId() + ":" + req.getDifficulty() + ":" + req.getCount() + ":"
                + (req.getKnowledgePointIds() == null ? "" :
                req.getKnowledgePointIds().stream().sorted().map(String::valueOf).collect(Collectors.joining(",")));
        return CACHE_KEY_PREFIX + DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 从 Redis 获取缓存的题目 ID 列表
     */
    private List<Long> getCachedQuestionIds(String cacheKey) {
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached == null) return Collections.emptyList();
        try {
            return objectMapper.readValue(cached, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Quiz 缓存解析失败，key={}", cacheKey);
            return Collections.emptyList();
        }
    }

    /**
     * 将题目 ID 列表写入 Redis 缓存
     */
    private void cacheQuestionIds(String cacheKey, List<Long> ids) {
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(ids),
                    CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.warn("Quiz 缓存写入失败", e);
        }
    }

    /**
     * 调用 DeepSeek AI 生成题目并入库
     */
    private List<Question> generateByAI(GenerateRequest req, String cacheKey) {
        String promptTemplate = loadPromptTemplate("quiz_generate.txt");
        String userPrompt = buildUserPrompt(req);

        // AI 调用（DeepSeekClient 内部已含重试逻辑）
        String aiResponse = deepSeekClient.chat(promptTemplate, userPrompt);

        // 解析 AI 响应
        List<Question> questions = parseAIResponse(aiResponse, req, cacheKey);

        // 批量入库
        questions.forEach(q -> questionMapper.insert(q));

        log.info("AI 出题成功，共 {} 道题", questions.size());
        return questions;
    }

    /**
     * 读取 Prompt 模板文件
     */
    private String loadPromptTemplate(String filename) {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/" + filename);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException(ResultCode.AI_GENERATE_FAILED, "Prompt 模板加载失败: " + filename);
        }
    }

    /**
     * 构建用户提示词（注入科目/难度/数量/知识点参数）
     */
    private String buildUserPrompt(GenerateRequest req) {
        String difficultyName = switch (req.getDifficulty()) {
            case 1 -> "简单";
            case 3 -> "困难";
            default -> "中等";
        };

        StringBuilder sb = new StringBuilder();
        sb.append("请生成 ").append(req.getCount()).append(" 道");
        sb.append("难度为【").append(difficultyName).append("】");
        sb.append("的行政职业能力测验（行测）题目。");

        if (!CollectionUtils.isEmpty(req.getKnowledgePointIds())) {
            sb.append("\n请覆盖以下知识点 ID：").append(req.getKnowledgePointIds());
        }

        sb.append("\n请严格按照 system prompt 中的 JSON 格式输出，不要输出任何额外说明。");
        return sb.toString();
    }

    /**
     * 解析 AI 响应 JSON，校验 Schema，转换为 Question 实体列表
     */
    private List<Question> parseAIResponse(String aiResponse, GenerateRequest req, String cacheKey) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            JsonNode questionsNode = root.path("questions");

            if (!questionsNode.isArray() || questionsNode.isEmpty()) {
                throw new BizException(ResultCode.AI_PARSE_FAILED, "AI 响应缺少 questions 数组");
            }

            List<Question> result = new ArrayList<>();
            for (JsonNode node : questionsNode) {
                Question q = new Question();
                q.setSubjectId(req.getSubjectId());

                // 知识点（可选）
                if (!CollectionUtils.isEmpty(req.getKnowledgePointIds())) {
                    q.setKnowledgePointId(req.getKnowledgePointIds().get(0));
                }

                q.setType(node.path("type").asInt(1));
                q.setDifficulty(req.getDifficulty());
                q.setContent(requireText(node, "content"));
                q.setAnswer(requireText(node, "answer"));
                q.setExplanation(node.path("explanation").asText(""));
                q.setSource(1); // AI 生成

                // options 转为 JSON 字符串存储
                JsonNode optionsNode = node.path("options");
                if (!optionsNode.isMissingNode() && !optionsNode.isNull()) {
                    q.setOptions(objectMapper.writeValueAsString(optionsNode));
                }

                q.setAiCacheKey(cacheKey);
                result.add(q);
            }

            return result;

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 响应解析失败: {}", aiResponse, e);
            throw new BizException(ResultCode.AI_PARSE_FAILED);
        }
    }

    /**
     * 从 JsonNode 中读取必填文本字段
     */
    private String requireText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            throw new BizException(ResultCode.AI_PARSE_FAILED, "AI 响应缺少字段: " + field);
        }
        return value.asText();
    }

    /**
     * 判题：标准化答案后比较（大写、去空格）
     */
    private boolean judge(Question q, String userAnswer) {
        if (userAnswer == null || q.getAnswer() == null) return false;
        String std = q.getAnswer().trim().toUpperCase();
        String usr = userAnswer.trim().toUpperCase();
        return std.equals(usr);
    }

    /**
     * 解析 options JSON 字符串为列表
     */
    private List<QuestionVO.Option> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    /**
     * 将 Question 实体列表转换为 QuestionVO 列表
     *
     * @param includeAnswer 是否包含答案（答题前不返回）
     */
    private List<QuestionVO> toVOList(List<Question> questions, boolean includeAnswer) {
        return questions.stream().map(q -> {
            QuestionVO vo = new QuestionVO();
            vo.setId(q.getId());
            vo.setSubjectId(q.getSubjectId());
            vo.setKnowledgePointId(q.getKnowledgePointId());
            vo.setType(q.getType());
            vo.setDifficulty(q.getDifficulty());
            vo.setContent(q.getContent());
            vo.setOptions(parseOptions(q.getOptions()));
            if (includeAnswer) {
                vo.setAnswer(q.getAnswer());
                vo.setExplanation(q.getExplanation());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 异步更新学习统计（不阻塞提交响应）
     */
    @Async
    public void updateStudyStat(Long userId, int answerCount, int correctCount) {
        try {
            studyStatMapper.upsertAnswerStat(userId, LocalDate.now(), answerCount, correctCount);
        } catch (Exception e) {
            log.warn("异步更新学习统计失败，userId={}", userId, e);
        }
    }
}
