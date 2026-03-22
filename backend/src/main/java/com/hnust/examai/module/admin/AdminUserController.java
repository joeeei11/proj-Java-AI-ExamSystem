package com.hnust.examai.module.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hnust.examai.common.result.R;
import com.hnust.examai.module.admin.dto.UpdateUserStatusRequest;
import com.hnust.examai.module.admin.dto.UserAdminVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员 - 用户管理接口
 * <p>所有接口需要 ADMIN 角色（SecurityConfig 已全局拦截 /api/admin/**）</p>
 */
@Tag(name = "管理员 - 用户管理", description = "用户列表查询与启用/禁用")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 分页查询用户列表
     *
     * @param page    页码，默认 1
     * @param size    每页数量，默认 10
     * @param keyword 搜索关键字（用户名/昵称模糊匹配），可选
     */
    @Operation(summary = "分页查询用户列表")
    @GetMapping
    public R<IPage<UserAdminVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return R.ok(adminUserService.listUsers(page, size, keyword));
    }

    /**
     * 启用或禁用用户
     *
     * @param id      用户 ID
     * @param request 状态请求体
     */
    @Operation(summary = "启用/禁用用户")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id,
                                @Valid @RequestBody UpdateUserStatusRequest request) {
        adminUserService.updateUserStatus(id, request);
        return R.ok();
    }
}
