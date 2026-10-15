package com.docuagent.docuagent.exception;

import com.docuagent.docuagent.common.CodeEnum;

public class BizException extends RuntimeException {
    private final int code;

    public BizException(CodeEnum codeEnum) {
        super(codeEnum.getMsg());
        this.code = codeEnum.getCode();
    }

    public int getCode() {
        return code;
    }
}