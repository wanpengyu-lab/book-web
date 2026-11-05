package org.example.bookweb;

import org.example.bookweb.dto.Result;
import org.example.bookweb.service.AuthService;
import org.example.bookweb.service.BorrowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理（PRD 3.1 错误码表）：
 * MethodArgumentNotValidException -> 400 + 40001
 * DuplicateKeyException           -> 409 + 40901
 * 兜底 Exception                  -> 500 + 50001（只回概要，不回堆栈）
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** @Valid 校验失败 -> 40001 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .orElse("参数校验失败");
        return ResponseEntity.badRequest().body(Result.error(40001, message));
    }

    /** 请求体缺失 / JSON 反序列化失败（类型错误等）-> 40001 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Result.error(40001, "请求体缺失或格式错误"));
    }

    /** 路径 / 查询参数类型错误 -> 40001 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(Result.error(40001, "参数类型错误: " + e.getName()));
    }

    /** v0.3：请求方法与接口不匹配（如 POST /api/stats）-> 400 + 40001，不再落入 50001 兜底 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.badRequest().body(Result.error(40001, "请求方法不支持"));
    }

    /** 唯一键冲突（重复编号等业务冲突）-> 40901 */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKey(DuplicateKeyException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.error(40901, "该编号已存在，添加失败"));
    }

    /** v0.3：注册用户名已存在 -> 409 + 40901 */
    @ExceptionHandler(AuthService.UsernameExistsException.class)
    public ResponseEntity<Result<Void>> handleUsernameExists(AuthService.UsernameExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.error(40901, e.getMessage()));
    }

    /** v0.3：登录凭据错误 -> 401 + 40101（不区分用户名/密码哪个错，避免账号枚举） */
    @ExceptionHandler(AuthService.InvalidCredentialsException.class)
    public ResponseEntity<Result<Void>> handleInvalidCredentials(AuthService.InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.error(40101, e.getMessage()));
    }

    /** v0.4：图书不存在 -> 404 + 40401 */
    @ExceptionHandler(BorrowService.BookNotFoundException.class)
    public ResponseEntity<Result<Void>> handleBookNotFound(BorrowService.BookNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.error(40401, e.getMessage()));
    }

    /** v0.4：借还业务冲突（抢借失败 / 无在借记录可还）-> 409 + 40901 */
    @ExceptionHandler(BorrowService.BorrowConflictException.class)
    public ResponseEntity<Result<Void>> handleBorrowConflict(BorrowService.BorrowConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.error(40901, e.getMessage()));
    }

    /** v0.4：reader 查他人记录 -> 403 + 40301 */
    @ExceptionHandler(BorrowService.RecordAccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleRecordAccessDenied(BorrowService.RecordAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.error(40301, e.getMessage()));
    }

    /** v0.4：status 非法 -> 400 + 40001 */
    @ExceptionHandler(BorrowService.InvalidStatusException.class)
    public ResponseEntity<Result<Void>> handleInvalidStatus(BorrowService.InvalidStatusException e) {
        return ResponseEntity.badRequest().body(Result.error(40001, e.getMessage()));
    }

    /** 兜底 -> 50001，响应只回概要，不回堆栈；详情仅记入服务端日志 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(50001, "服务器内部错误，请稍后重试"));
    }
}
