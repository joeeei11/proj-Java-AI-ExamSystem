package com.hnust.examai.module.errorbook;

import com.hnust.examai.common.exception.BizException;
import com.hnust.examai.common.result.ResultCode;
import com.hnust.examai.entity.ErrorBook;
import com.hnust.examai.module.common.SrsService;
import com.hnust.examai.module.errorbook.dto.ErrorBookVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 错题本 Service
 * <p>
 * 提供错题列表查询、错因标签设置、SRS 间隔复习功能
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorBookService {

    private final ErrorBookMapper errorBookMapper;
    private final SrsService srsService;

    // ===== 公开方法 =====

    /**
     * 错题列表（支持科目/掌握状态过滤）
     *
     * @param userId     当前用户 ID
     * @param subjectId  按科目过滤（null 表示全部）
     * @param isMastered 按掌握状态过滤（null 表示全部，0 未掌握，1 已掌握）
     */
    public List<ErrorBookVO> list(Long userId, Long subjectId, Integer isMastered) {
        return errorBookMapper.selectByUserId(userId, subjectId, isMastered);
    }

    /**
     * 设置错因标签
     *
     * @param userId   当前用户 ID
     * @param errorId  错题本记录 ID
     * @param reason   错因标签（1-6）
     */
    public void setReason(Long userId, Long errorId, Integer reason) {
        ErrorBook errorBook = requireErrorBook(errorId, userId);
        errorBook.setErrorReason(reason);
        errorBookMapper.updateById(errorBook);
        log.info("设置错因标签，userId={}, errorId={}, reason={}", userId, errorId, reason);
    }

    /**
     * 今日待复做列表（SRS 到期，且未掌握）
     *
     * @param userId 当前用户 ID
     */
    public List<ErrorBookVO> todayReview(Long userId) {
        return errorBookMapper.selectTodayReview(userId);
    }

    /**
     * 提交错题复做结果，更新 SRS 调度字段
     *
     * @param userId  当前用户 ID
     * @param errorId 错题本记录 ID
     * @param known   是否已掌握
     */
    public void submitReview(Long userId, Long errorId, boolean known) {
        ErrorBook errorBook = requireErrorBook(errorId, userId);

        // 计算新的间隔天数和下次复习时间
        int currentInterval = errorBook.getReviewIntervalDays() != null
                ? errorBook.getReviewIntervalDays() : 1;
        int nextInterval = srsService.calcNextInterval(known, currentInterval);
        LocalDateTime nextReviewAt = srsService.calcNextReviewAt(nextInterval);

        // 更新 SRS 字段
        errorBook.setReviewIntervalDays(nextInterval);
        errorBook.setNextReviewAt(nextReviewAt);
        errorBook.setReviewCount(
                errorBook.getReviewCount() != null ? errorBook.getReviewCount() + 1 : 1);

        if (known) {
            errorBook.setConsecutiveCorrect(
                    errorBook.getConsecutiveCorrect() != null
                            ? errorBook.getConsecutiveCorrect() + 1 : 1);
            // 连续正确 3 次，标记为已掌握
            if (errorBook.getConsecutiveCorrect() >= 3) {
                errorBook.setIsMastered(1);
                log.info("错题已掌握，userId={}, errorId={}", userId, errorId);
            }
        } else {
            errorBook.setConsecutiveCorrect(0);
            errorBook.setIsMastered(0);
        }

        errorBookMapper.updateById(errorBook);
        log.info("提交复做结果，userId={}, errorId={}, known={}, nextInterval={}天",
                userId, errorId, known, nextInterval);
    }

    // ===== 私有方法 =====

    /**
     * 查询错题本记录并校验归属，不存在或无权限时抛出业务异常
     */
    private ErrorBook requireErrorBook(Long errorId, Long userId) {
        ErrorBook errorBook = errorBookMapper.selectById(errorId);
        if (errorBook == null) {
            throw new BizException(ResultCode.ERROR_BOOK_NOT_FOUND);
        }
        if (!errorBook.getUserId().equals(userId)) {
            throw new BizException(ResultCode.ERROR_BOOK_ACCESS_DENIED);
        }
        return errorBook;
    }
}
