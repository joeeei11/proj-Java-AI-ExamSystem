package com.hnust.examai.module.admin;

import com.hnust.examai.module.admin.dto.SystemStatsVO;
import com.hnust.examai.module.auth.UserMapper;
import com.hnust.examai.module.note.NoteMapper;
import com.hnust.examai.module.quiz.QuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 管理员系统统计 Service
 */
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final UserMapper userMapper;
    private final QuestionMapper questionMapper;
    private final NoteMapper noteMapper;
    private final AdminStatsMapper adminStatsMapper;

    /**
     * 获取系统级统计总览
     *
     * @return 系统统计数据
     */
    public SystemStatsVO getSystemStats() {
        SystemStatsVO vo = new SystemStatsVO();
        vo.setTotalUsers(userMapper.selectCount(null));
        vo.setTotalQuestions(questionMapper.selectCount(null));
        vo.setTotalNotes(noteMapper.selectCount(null));
        vo.setTotalAnswers(adminStatsMapper.countTotalAnswers());
        vo.setTodayActiveUsers(adminStatsMapper.countTodayActiveUsers());
        return vo;
    }
}
