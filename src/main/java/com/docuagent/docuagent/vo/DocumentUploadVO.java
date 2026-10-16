package com.docuagent.docuagent.vo;

import lombok.Data;

@Data

    public class DocumentUploadVO{
        private Long id;               // 课件ID（document 表主键）
        private String originalName;   // 原始文件名
        private Integer charCount;     // 提取出的字数
        private String status;         // 解析状态（DONE / FAILED）
        private String preview;         // 前 200 字预览
    }

