package com.hnust.examai.common.result;

import lombok.Getter;

/**
 * 业务状态码枚举
 */
@Getter
public enum ResultCode {

    // ===== 通用 =====
    SUCCESS(200, "操作成功"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或 Token 已过期"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),

    // ===== 用户模块 =====
    USER_NOT_FOUND(1001, "用户不存在"),
    USERNAME_EXISTS(1002, "用户名已存在"),
    PASSWORD_WRONG(1003, "密码错误"),
    USER_DISABLED(1004, "账号已被禁用"),
    TOKEN_EXPIRED(1005, "Token 已过期，请重新登录"),
    TOKEN_INVALID(1006, "Token 无效"),
    CHECKIN_DUPLICATE(409, "今日已打卡"),

    // ===== 题目模块 =====
    QUESTION_NOT_FOUND(2001, "题目不存在"),
    AI_GENERATE_FAILED(2002, "AI 出题失败，请稍后重试"),
    AI_PARSE_FAILED(2003, "AI 响应解析失败"),
    AI_TIMEOUT(2004, "AI 服务超时，请稍后重试"),

    // ===== 笔记模块 =====
    NOTE_NOT_FOUND(3001, "笔记不存在"),
    OCR_FAILED(3002, "OCR 识别失败"),
    NOTE_ACCESS_DENIED(3003, "无权操作此笔记"),

    // ===== 文件模块 =====
    FILE_UPLOAD_FAILED(4001, "文件上传失败"),
    FILE_TYPE_NOT_ALLOWED(4002, "不支持的文件类型"),
    FILE_SIZE_EXCEEDED(4003, "文件大小超出限制"),

    // ===== 模考模块 =====
    MOCK_EXAM_NOT_FOUND(5001, "模考不存在"),
    MOCK_EXAM_TIMEOUT(5002, "考试已超时"),
    MOCK_EXAM_SUBMITTED(5003, "考试已提交"),
    MOCK_EXAM_IN_PROGRESS(5004, "有进行中的模考，请先完成"),

    // ===== 抽认卡 =====
    FLASHCARD_DECK_NOT_FOUND(6001, "卡组不存在"),
    FLASHCARD_NOT_FOUND(6002, "卡片不存在"),

    // ===== 反馈 =====
    FEEDBACK_NOT_FOUND(7001, "反馈记录不存在"),

    // ===== 错题本 =====
    ERROR_BOOK_NOT_FOUND(8001, "错题记录不存在"),
    ERROR_BOOK_ACCESS_DENIED(8002, "无权操作此错题"),

    // ===== 复习提纲 =====
    REVIEW_OUTLINE_NOT_FOUND(8101, "复习提纲不存在"),
    REVIEW_OUTLINE_ACCESS_DENIED(8102, "无权操作此复习提纲"),
    REVIEW_NOTE_EMPTY(8103, "所选笔记内容为空，无法生成提纲");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
