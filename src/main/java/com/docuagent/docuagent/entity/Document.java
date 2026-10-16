package com.docuagent.docuagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("document")
public class Document {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private String originalName;
    private String storagePath;
    private String fileType;
    private Integer charCount;
    private Long fileSize;
    private String status;
    private String failureReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
