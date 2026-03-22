package com.hnust.examai.module.feedback;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hnust.examai.common.exception.BizException;
import com.hnust.examai.common.result.ResultCode;
import com.hnust.examai.entity.Question;
import com.hnust.examai.entity.QuestionFeedback;
import com.hnust.examai.module.common.NoticeService;
import com.hnust.examai.module.feedback.dto.*;
import com.hnust.examai.module.quiz.QuestionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 题目纠错反馈 Service
 * <p>
 * 用户端：提交、查看自己的反馈列表/详情<br>
 * 管理员端：查看全部反馈、处理并发 Redis 通知
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private static final String[] TYPE_LABELS = {"", "答案错误", "题干歧义", "解析不清", "排版问题"};
    private static final String[] STATUS_LABELS = {"待处理", "已采纳", "已驳回", "已修复"};

    private final FeedbackMapper feedbackMapper;
    private final QuestionMapper questionMapper;
    private final NoticeService noticeService;

    // ===== 用户端方法 =====

    /**
     * 提交题目纠错反馈
     *
     * @param userId  提交用户 ID
     * @param request 反馈内容
     */
    public void submit(Long userId, SubmitFeedbackRequest request) {
        // 校验题目是否存在
        Question question = questionMapper.selectById(request.getQuestionId());
        if (question == null) {
            throw new BizException(ResultCode.QUESTION_NOT_FOUND);
        }

        QuestionFeedback feedback = new QuestionFeedback();
        feedback.setUserId(userId);
        feedback.setQuestionId(request.getQuestionId());
        feedback.setType(request.getType());
        feedback.setDescription(request.getDescription());
        feedback.setScreenshotUrl(request.getScreenshotUrl());
        feedback.setStatus(0); // 待处理
        feedbackMapper.insert(feedback);

        log.info("用户提交纠错反馈，userId={}, questionId={}, type={}",
                userId, request.getQuestionId(), request.getType());
    }

    /**
     * 查询用户自己的反馈列表（分页）
     *
     * @param userId 用户 ID
     * @param page   页码（从 1 开始）
     * @param size   每页数量
     * @return 分页结果
     */
    public IPage<FeedbackVO> listMy(Long userId, int page, int size) {
        return feedbackMapper.selectByUserId(new Page<>(page, size), userId);
    }

    /**
     * 查询用户自己的反馈详情
     *
     * @param userId     用户 ID
     * @param feedbackId 反馈 ID
     * @return 详情 VO
     */
    public FeedbackDetailVO getMy(Long userId, Long feedbackId) {
        QuestionFeedback feedback = requireFeedback(feedbackId);
        if (!feedback.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }

        Question question = questionMapper.selectById(feedback.getQuestionId());

        FeedbackDetailVO vo = buildDetailVO(feedback, question);
        return vo;
    }

    // ===== 管理员端方法 =====

    /**
     * 管理员分页查询全部反馈
     *
     * @param page   页码（从 1 开始）
     * @param size   每页数量
     * @param status 状态过滤（null 查全部）
     * @return 分页结果
     */
    public IPage<FeedbackVO> listAll(int page, int size, Integer status) {
        return feedbackMapper.selectAllForAdmin(new Page<>(page, size), status);
    }

    /**
     * 管理员处理反馈（修改状态 + 写回复 + 发 Redis 通知）
     *
     * @param adminId    管理员 ID
     * @param feedbackId 反馈 ID
     * @param request    处理请求
     */
    @Transactional
    public void handle(Long adminId, Long feedbackId, HandleFeedbackRequest request) {
        QuestionFeedback feedback = requireFeedback(feedbackId);

        feedback.setStatus(request.getStatus());
        feedback.setAdminId(adminId);
        feedback.setAdminReply(request.getAdminReply());
        feedback.setRepliedAt(LocalDateTime.now());
        feedbackMapper.updateById(feedback);

        // 发送 Redis 站内通知给反馈者
        String statusLabel = STATUS_LABELS[request.getStatus()];
        String noticeMsg = buildNoticeMessage(feedback, statusLabel, request.getAdminReply());
        noticeService.push(feedback.getUserId(), noticeMsg);

        log.info("管理员处理纠错反馈，adminId={}, feedbackId={}, status={}",
                adminId, feedbackId, request.getStatus());
    }

    // ===== 私有方法 =====

    private QuestionFeedback requireFeedback(Long feedbackId) {
        QuestionFeedback feedback = feedbackMapper.selectById(feedbackId);
        if (feedback == null) {
            throw new BizException(ResultCode.FEEDBACK_NOT_FOUND);
        }
        return feedback;
    }

    private FeedbackDetailVO buildDetailVO(QuestionFeedback feedback, Question question) {
        FeedbackDetailVO vo = new FeedbackDetailVO();
        vo.setId(feedback.getId());
        vo.setQuestionId(feedback.getQuestionId());
        if (question != null) {
            vo.setQuestionContent(question.getContent());
            vo.setQuestionAnswer(question.getAnswer());
        }
        vo.setType(feedback.getType());
        vo.setTypeLabel(typeLabel(feedback.getType()));
        vo.setStatus(feedback.getStatus());
        vo.setStatusLabel(statusLabel(feedback.getStatus()));
        vo.setDescription(feedback.getDescription());
        vo.setScreenshotUrl(feedback.getScreenshotUrl());
        vo.setAdminReply(feedback.getAdminReply());
        vo.setCreatedAt(feedback.getCreatedAt());
        vo.setRepliedAt(feedback.getRepliedAt());
        return vo;
    }

    private String buildNoticeMessage(QuestionFeedback feedback, String statusLabel, String adminReply) {
        StringBuilder sb = new StringBuilder();
        sb.append("您的题目纠错反馈（").append(typeLabel(feedback.getType())).append("）");
        sb.append("已被处理，状态更新为：").append(statusLabel);
        if (adminReply != null && !adminReply.isBlank()) {
            sb.append("。管理员回复：").append(adminReply);
        }
        return sb.toString();
    }

    private String typeLabel(Integer type) {
        if (type == null || type < 1 || type >= TYPE_LABELS.length) return "其他";
        return TYPE_LABELS[type];
    }

    private String statusLabel(Integer status) {
        if (status == null || status < 0 || status >= STATUS_LABELS.length) return "未知";
        return STATUS_LABELS[status];
    }
}
