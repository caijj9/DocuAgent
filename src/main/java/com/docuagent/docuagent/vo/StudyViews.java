package com.docuagent.docuagent.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 学习业务的对外数据，不暴露实体、逻辑删除标记或未作答题目的标准答案。 */
public final class StudyViews {
    private StudyViews() { }
    public record Reference(int index, Long chunkId, Long documentId, String documentName, String snippet, Double score) { }
    public record Qa(String answer, List<Reference> references, String rewrittenQuestion, String sessionId) { }
    public record QuestionView(Long id, Long courseId, String type, String stem, List<String> options,
                               int maxScore, Long sourceChunkId) { }
    public record AnswerView(Long answerRecordId, Long questionId, BigDecimal score, BigDecimal maxScore,
                             boolean isCorrect, String comment, List<String> hitPoints, List<String> missedPoints) { }
    public record WrongView(Long questionId, String stem, String type, String myAnswer, String correctAnswer,
                            int wrongCount, String source, LocalDateTime lastWrongTime) { }
    public record AnswerHistory(Long questionId, String stem, String myAnswer, BigDecimal score,
                                BigDecimal maxScore, int wrongCount, String source, LocalDateTime answeredAt, String missedPoints) { }
    public record SessionView(String sessionId, Long courseId, String createdAt) { }
    public record ToolTrace(String tool, String parameters, String result) { }
    public record AgentView(String answer, String sessionId, List<ToolTrace> toolCalls, List<Reference> references) { }
}
