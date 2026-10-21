package com.docuagent.docuagent.dto;

import jakarta.validation.constraints.*;

/** 第二段接口入参；课程和题目均由服务层再次检查归属与状态。 */
public final class StudyRequests {
    private StudyRequests() { }
    public record Ask(@NotBlank @Size(max=1000) String question, @Min(1) Long courseId,
                      @Pattern(regexp="[0-9a-fA-F-]{36}") String sessionId) { }
    public record Generate(@NotNull @Min(1) Long courseId, @NotNull @Min(1) @Max(10) Integer count,
                           @NotNull @Pattern(regexp="single_choice|short_answer|mixed") String type,
                           @Size(max=200) String keyword) { }
    public record Submit(@NotNull @Min(1) Long questionId, @NotBlank @Size(max=5000) String userAnswer,
                         @NotNull @Min(0) Integer durationMs,
                         @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,64}") String requestId) { }
    public record SessionCreate(@Min(1) Long courseId) { }
}
