package com.hnust.examai.common.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一 API 响应体
 *
 * @param <T> 数据类型
 */
@Data
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务状态码：200 成功，其余为失败 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 时间戳（毫秒） */
    private long timestamp;

    private R() {
        this.timestamp = System.currentTimeMillis();
    }

    // ===== 成功响应 =====

    public static <T> R<T> ok() {
        R<T> r = new R<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.message = ResultCode.SUCCESS.getMessage();
        return r;
    }

    public static <T> R<T> ok(T data) {
        R<T> r = ok();
        r.data = data;
        return r;
    }

    public static <T> R<T> ok(String message, T data) {
        R<T> r = ok(data);
        r.message = message;
        return r;
    }

    // ===== 失败响应 =====

    public static <T> R<T> fail() {
        R<T> r = new R<>();
        r.code = ResultCode.INTERNAL_ERROR.getCode();
        r.message = ResultCode.INTERNAL_ERROR.getMessage();
        return r;
    }

    public static <T> R<T> fail(String message) {
        R<T> r = new R<>();
        r.code = ResultCode.INTERNAL_ERROR.getCode();
        r.message = message;
        return r;
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        R<T> r = new R<>();
        r.code = resultCode.getCode();
        r.message = resultCode.getMessage();
        return r;
    }

    public static <T> R<T> fail(ResultCode resultCode, String message) {
        R<T> r = new R<>();
        r.code = resultCode.getCode();
        r.message = message;
        return r;
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        return r;
    }

    // ===== 工具方法 =====

    /** 判断是否成功 */
    public boolean isOk() {
        return this.code == ResultCode.SUCCESS.getCode();
    }
}
