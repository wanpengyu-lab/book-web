package org.example.bookweb;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.dao.DuplicateKeyException;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseBody
    public Map<String, Object> handleDuplicateKey(DuplicateKeyException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "该编号已存在，添加失败");
        return result;
    }
    @ExceptionHandler(SQLException.class)
    @ResponseBody
    public Map<String,Object> handlerSqlError(SQLException e){
        e.printStackTrace();
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        String msg = e.getMessage();
        if (msg != null && msg.contains("Duplicate entry")) {
            result.put("message", "该编号已存在，添加失败");
        } else {
            result.put("message", "数据库操作失败");
        }
        return result;
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Map<String, Object> handleOther(Exception e) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "服务器开小差了：" + e.getMessage());
        return result;
    }


}
