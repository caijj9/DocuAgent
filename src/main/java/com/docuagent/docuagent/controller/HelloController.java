package com.docuagent.docuagent.controller;

import com.docuagent.docuagent.ai.chunk.SimpleChunker;
import com.docuagent.docuagent.common.CodeEnum;
import com.docuagent.docuagent.common.Result;
import com.docuagent.docuagent.exception.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class HelloController {

    @Autowired
    private SimpleChunker simpleChunker;

    @GetMapping("/hello")
    public Result<String> hello() {
        return new Result<>(0, "成功", "DocuAgent 已启动");
    }
    @GetMapping("/test-error")
    public Result<Void> testError() {
        throw new BizException(CodeEnum.COURSE_NOT_FOUND);
    }
    @GetMapping("/test-500")
    public Result<Void> test500() {
        throw new RuntimeException("故意抛一个非业务异常");
    }

    /** 切块器验证：把一段文字切开，看 overlap 有没有生效 */
    @GetMapping("/test-chunk")
    public Result<List<String>> testChunk() {
        String text = "数据库系统由数据库、数据库管理系统、应用程序和数据库管理员组成";
        return Result.success(simpleChunker.split(text));
    }

}