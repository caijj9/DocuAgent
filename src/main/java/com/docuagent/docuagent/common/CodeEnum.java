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
    RERANK_FAILED(1008, "重排模型调用或结果校验失败，请关闭 rerank 后重试"),
    MODEL_FAILED(1100, "模型调用失败，请稍后重试"),
    MODEL_OUTPUT_INVALID(1101, "模型输出格式或依据校验失败，请查看本地日志"),
    NO_COURSE_MATERIAL(1102, "当前课程没有可用的课件内容"),
    QUESTION_NOT_FOUND(1103, "题目不存在或所属课程不可用"),
    REQUEST_ID_CONFLICT(1104, "requestId已用于其他答题内容，请勿复用"),
    SESSION_NOT_FOUND(1105, "会话不存在或已过期"),
    SESSION_BUSY(1106, "会话正在处理上一条消息，请稍后重试"),
    SESSION_COURSE_MISMATCH(1107, "会话所属课程与请求不一致"),
    TOOL_ACTION_NOT_ALLOWED(1108, "请明确指定课程及要收藏的题目"),
    SESSION_STORE_UNAVAILABLE(1109, "会话存储不可用，请检查Redis服务");

    private final int code;
    private final String msg;
}
