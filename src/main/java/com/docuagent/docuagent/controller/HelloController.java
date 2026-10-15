package com.docuagent.docuagent.controller;

import com.docuagent.docuagent.common.CodeEnum;
import com.docuagent.docuagent.common.Result;
import com.docuagent.docuagent.exception.BizException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

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

}