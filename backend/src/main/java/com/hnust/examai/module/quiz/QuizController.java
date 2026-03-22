package com.hnust.examai.module.quiz;

import com.hnust.examai.common.result.R;
import com.hnust.examai.module.quiz.dto.GenerateRequest;
import com.hnust.examai.module.quiz.dto.QuestionVO;
import com.hnust.examai.module.quiz.dto.SubmitRequest;
import com.hnust.examai.module.quiz.dto.SubmitResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 刷题接口
 */
@Tag(name = "刷题", description = "AI 出题、提交答案")
@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    /**
     * AI 出题
     */
    @Operation(summary = "AI 出题")
    @PostMapping("/generate")
    public R<List<QuestionVO>> generate(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GenerateRequest request) {

        Long userId = Long.parseLong(userDetails.getUsername());
        List<QuestionVO> questions = quizService.generate(userId, request);
        return R.ok(questions);
    }

    /**
     * 提交答案
     */
    @Operation(summary = "提交答案并获取判分结果")
    @PostMapping("/submit")
    public R<SubmitResult> submit(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SubmitRequest request) {

        Long userId = Long.parseLong(userDetails.getUsername());
        SubmitResult result = quizService.submit(userId, request);
        return R.ok(result);
    }
}
