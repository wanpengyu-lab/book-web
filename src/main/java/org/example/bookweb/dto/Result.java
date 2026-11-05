package org.example.bookweb.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

/**
 * 统一响应结构（PRD 3.1）。
 * 字段顺序固定 code, message, data（@JsonPropertyOrder 锁定 JSON 输出顺序）。
 */
@Data
@JsonPropertyOrder({"code", "message", "data"})
public class Result<T> {

    /** 0 = 成功；业务错误码见 PRD 3.1 错误码表 */
    private int code;

    private String message;

    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.message = "ok";
        r.data = data;
        return r;
    }

    /**
     * v0.7 外部数据源约定（PRD v0.7 强制规范）：外部调用未命中/未配置/失败不抛 5xx，
     * 一律 HTTP 200 + code 0 + data:null + 明确 message。
     */
    public static <T> Result<T> notice(String message) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.message = message;
        r.data = null;
        return r;
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        r.data = null;
        return r;
    }
}
