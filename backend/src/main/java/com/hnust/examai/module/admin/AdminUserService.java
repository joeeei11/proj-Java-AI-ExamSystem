package com.hnust.examai.module.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hnust.examai.common.exception.BizException;
import com.hnust.examai.common.result.ResultCode;
import com.hnust.examai.entity.User;
import com.hnust.examai.module.admin.dto.UpdateUserStatusRequest;
import com.hnust.examai.module.admin.dto.UserAdminVO;
import com.hnust.examai.module.auth.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 管理员用户管理 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserMapper userMapper;

    /**
     * 分页查询用户列表（支持用户名/昵称模糊搜索）
     *
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页数量
     * @param keyword  搜索关键字（可选）
     * @return 分页结果
     */
    public IPage<UserAdminVO> listUsers(int pageNum, int pageSize, String keyword) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .select(User::getId, User::getUsername, User::getNickname, User::getEmail,
                        User::getRole, User::getIsActive, User::getStudyStreak,
                        User::getLastLoginAt, User::getCreatedAt)
                .orderByDesc(User::getCreatedAt);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword));
        }

        IPage<User> userPage = userMapper.selectPage(page, wrapper);
        return userPage.convert(this::toVO);
    }

    /**
     * 启用或禁用用户
     *
     * @param userId  用户 ID
     * @param request 状态请求
     */
    public void updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        int isActive = Boolean.TRUE.equals(request.getIsActive()) ? 1 : 0;
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getIsActive, isActive));
        log.info("管理员操作：用户 {} 状态更新为 {}", userId, isActive == 1 ? "启用" : "禁用");
    }

    private UserAdminVO toVO(User user) {
        UserAdminVO vo = new UserAdminVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setIsActive(user.getIsActive());
        vo.setStudyStreak(user.getStudyStreak());
        vo.setLastLoginAt(user.getLastLoginAt());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
