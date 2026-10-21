package com.docuagent.docuagent.vo;

/**
 * 检索命中的一条结果。
 *
 * @param chunkId      命中的块 id（要拿它回 chunk 表取原文、做引用溯源）
 * @param documentId   来源课件 id
 * @param documentName 来源课件文件名（直接给用户看"这答案从哪来的"）
 * @param score        当前阶段的排序分数，须结合 scoreType 解释，跨阶段不可直接比较
 * @param content      命中的块原文
 */
public record RetrievalHitVO(
        Long chunkId,
        Long documentId,
        String documentName,
        Double score,
        String content,
        String scoreType,
        Double rerankScore
) {
    public RetrievalHitVO(Long chunkId, Long documentId, String documentName, Double score, String content) {
        this(chunkId, documentId, documentName, score, content, "vector_cosine", null);
    }
}
