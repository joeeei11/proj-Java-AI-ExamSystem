package com.hnust.examai.module.admin;

import com.hnust.examai.common.result.R;
import com.hnust.examai.entity.KnowledgePoint;
import com.hnust.examai.entity.Subject;
import com.hnust.examai.module.admin.dto.KnowledgePointRequest;
import com.hnust.examai.module.admin.dto.SubjectRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员 - 科目与知识点管理接口
 * 需要 ADMIN 角色（SecurityConfig 已拦截 /api/admin/**）
 */
@Tag(name = "管理员-科目管理", description = "科目与知识点 CRUD")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer")
public class SubjectAdminController {

    private final SubjectService subjectService;

    // ===== 科目接口 =====

    @Operation(summary = "科目列表（含禁用）")
    @GetMapping("/subjects")
    public R<List<Subject>> listSubjects() {
        return R.ok(subjectService.listSubjects());
    }

    @Operation(summary = "创建科目")
    @PostMapping("/subjects")
    public R<Subject> createSubject(@Valid @RequestBody SubjectRequest request) {
        return R.ok(subjectService.createSubject(request));
    }

    @Operation(summary = "更新科目")
    @PutMapping("/subjects/{id}")
    public R<Subject> updateSubject(@PathVariable Long id,
                                    @Valid @RequestBody SubjectRequest request) {
        return R.ok(subjectService.updateSubject(id, request));
    }

    @Operation(summary = "删除科目（级联删除知识点）")
    @DeleteMapping("/subjects/{id}")
    public R<Void> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return R.ok();
    }

    // ===== 知识点接口 =====

    @Operation(summary = "科目下的知识点列表")
    @GetMapping("/subjects/{id}/knowledge-points")
    public R<List<KnowledgePoint>> listKnowledgePoints(@PathVariable Long id) {
        return R.ok(subjectService.listKnowledgePoints(id));
    }

    @Operation(summary = "创建知识点")
    @PostMapping("/knowledge-points")
    public R<KnowledgePoint> createKnowledgePoint(@Valid @RequestBody KnowledgePointRequest request) {
        return R.ok(subjectService.createKnowledgePoint(request));
    }

    @Operation(summary = "更新知识点")
    @PutMapping("/knowledge-points/{id}")
    public R<KnowledgePoint> updateKnowledgePoint(@PathVariable Long id,
                                                   @Valid @RequestBody KnowledgePointRequest request) {
        return R.ok(subjectService.updateKnowledgePoint(id, request));
    }

    @Operation(summary = "删除知识点")
    @DeleteMapping("/knowledge-points/{id}")
    public R<Void> deleteKnowledgePoint(@PathVariable Long id) {
        subjectService.deleteKnowledgePoint(id);
        return R.ok();
    }
}
