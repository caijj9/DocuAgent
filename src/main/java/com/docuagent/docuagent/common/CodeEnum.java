package com.docuagent.docuagent.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CodeEnum {
    SUCCESS(0, "成功"),
    COURSE_NOT_FOUND(1001, "课程不存在"),
    UNSUPPORTED_FILE_TYPE(1002, "不支持的文件类型"),
    DOCUMENT_ALREADY_EXISTS(1004, "该课件已上传过"),
    COURSE_ALREADY_EXISTS(1005, "课程已存在"),
    INVALID_RETRIEVAL_REQUEST(400, "检索参数无效，请检查问题、课程、mode、融合与重排开关"),
    RETRIEVAL_LIMIT_EXCEEDED(1006, "当前本地检索分块数超过上限，请缩小课程范围"),
    INDEX_OPERATION_FAILED(1007, "向量索引操作失败，请查看本地日志"),
    RERANK_FAILED(1008, "重排模型调用或结果校验失败，请关闭 rerank 后重试");

    private final int code;
    private final String msg;
}
