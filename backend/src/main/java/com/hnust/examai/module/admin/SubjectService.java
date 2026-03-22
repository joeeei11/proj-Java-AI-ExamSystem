package com.hnust.examai.module.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hnust.examai.common.exception.BizException;
import com.hnust.examai.common.result.ResultCode;
import com.hnust.examai.entity.KnowledgePoint;
import com.hnust.examai.entity.Subject;
import com.hnust.examai.module.admin.dto.KnowledgePointRequest;
import com.hnust.examai.module.admin.dto.SubjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 科目与知识点 Service（管理员）
 */
@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectMapper subjectMapper;
    private final KnowledgePointMapper knowledgePointMapper;

    // ===== 科目 CRUD =====

    public List<Subject> listSubjects() {
        return subjectMapper.selectList(
                new LambdaQueryWrapper<Subject>().orderByAsc(Subject::getSortOrder));
    }

    public List<Subject> listActiveSubjects() {
        return subjectMapper.selectList(
                new LambdaQueryWrapper<Subject>()
                        .eq(Subject::getIsActive, 1)
                        .orderByAsc(Subject::getSortOrder));
    }

    public Subject createSubject(SubjectRequest request) {
        Subject subject = new Subject();
        subject.setName(request.getName());
        subject.setDescription(request.getDescription());
        subject.setSortOrder(request.getSortOrder());
        subject.setIsActive(request.getIsActive());
        subjectMapper.insert(subject);
        return subject;
    }

    public Subject updateSubject(Long id, SubjectRequest request) {
        Subject subject = getSubjectOrThrow(id);
        subject.setName(request.getName());
        subject.setDescription(request.getDescription());
        subject.setSortOrder(request.getSortOrder());
        subject.setIsActive(request.getIsActive());
        subjectMapper.updateById(subject);
        return subject;
    }

    @Transactional
    public void deleteSubject(Long id) {
        getSubjectOrThrow(id);
        // 级联删除知识点
        knowledgePointMapper.delete(
                new LambdaQueryWrapper<KnowledgePoint>().eq(KnowledgePoint::getSubjectId, id));
        subjectMapper.deleteById(id);
    }

    // ===== 知识点 CRUD =====

    public List<KnowledgePoint> listKnowledgePoints(Long subjectId) {
        return knowledgePointMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(KnowledgePoint::getSubjectId, subjectId)
                        .orderByAsc(KnowledgePoint::getSortOrder));
    }

    public KnowledgePoint createKnowledgePoint(KnowledgePointRequest request) {
        // 验证科目存在
        getSubjectOrThrow(request.getSubjectId());
        KnowledgePoint kp = new KnowledgePoint();
        kp.setSubjectId(request.getSubjectId());
        kp.setName(request.getName());
        kp.setDescription(request.getDescription());
        kp.setSortOrder(request.getSortOrder());
        knowledgePointMapper.insert(kp);
        return kp;
    }

    public KnowledgePoint updateKnowledgePoint(Long id, KnowledgePointRequest request) {
        KnowledgePoint kp = knowledgePointMapper.selectById(id);
        if (kp == null) {
            throw new BizException(ResultCode.NOT_FOUND, "知识点不存在");
        }
        kp.setName(request.getName());
        kp.setDescription(request.getDescription());
        kp.setSortOrder(request.getSortOrder());
        knowledgePointMapper.updateById(kp);
        return kp;
    }

    public void deleteKnowledgePoint(Long id) {
        knowledgePointMapper.deleteById(id);
    }

    // ===== 私有 =====

    private Subject getSubjectOrThrow(Long id) {
        Subject subject = subjectMapper.selectById(id);
        if (subject == null) {
            throw new BizException(ResultCode.NOT_FOUND, "科目不存在");
        }
        return subject;
    }
}
