package com.docuagent.docuagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 已校验并保存的题目及评分标准。 */
@Data @TableName("question")
public class Question {
    @TableId(type=IdType.AUTO) private Long id;
    private Long courseId;
    private String type;
    private String stem;
    private String options;
    private String answer;
    private String keyPoints;
    private Long sourceChunkId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
    @TableField(exist=false) private boolean evidenceChoice;
}
