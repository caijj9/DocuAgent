package com.docuagent.docuagent.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CodeEnum {
    SUCCESS(0, "成功"),
    COURSE_NOT_FOUND(1001, "课程不存在"),
    UNSUPPORTED_FILE_TYPE(1002, "不支持的文件类型");

    private final int code;
    private final String msg;
}