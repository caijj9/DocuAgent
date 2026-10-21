package com.docuagent.docuagent.vo;

/** 数据库中仍可引用的分块，课程与课件状态已在查询时校验。 */
public record RetrievalCandidate(Long chunkId, Long documentId, Long courseId,
                                 String documentName, String content) {
}
