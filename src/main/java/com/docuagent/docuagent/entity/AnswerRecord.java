package com.docuagent.docuagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 一次提交的持久化结果，requestId唯一，重试从本记录重放。 */
@Data @TableName("answer_record")
public class AnswerRecord {
    @TableId(type=IdType.AUTO) private Long id;
    private Long questionId;
    private String userAnswer;
    private BigDecimal score;
    private BigDecimal maxScore;
    private Integer isCorrect;
    private String comment;
    private Integer durationMs;
    private String requestId;
    private String hitPoints;
    private String missedPoints;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
