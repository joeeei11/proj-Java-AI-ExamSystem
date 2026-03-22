package com.hnust.examai.module.subject;

import com.hnust.examai.common.result.R;
import com.hnust.examai.entity.KnowledgePoint;
import com.hnust.examai.entity.Subject;
import com.hnust.examai.module.admin.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公开科目接口（无需鉴权）
 * 用于前端出题选择等场景
 */
@Tag(name = "科目（公开）", description = "获取科目和知识点列表")
@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    /**
     * 获取所有启用科目列表
     */
    @Operation(summary = "科目列表（仅启用）")
    @GetMapping
    public R<List<Subject>> listSubjects() {
        return R.ok(subjectService.listActiveSubjects());
    }

    /**
     * 获取科目下的知识点列表
     */
    @Operation(summary = "科目知识点列表")
    @GetMapping("/{id}/knowledge-points")
    public R<List<KnowledgePoint>> listKnowledgePoints(@PathVariable Long id) {
        return R.ok(subjectService.listKnowledgePoints(id));
    }
}
