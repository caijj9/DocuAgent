package com.docuagent.docuagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docuagent.docuagent.ai.retrieval.BatchReranker;
import com.docuagent.docuagent.ai.retrieval.RetrievalRanking;
import com.docuagent.docuagent.common.CodeEnum;
import com.docuagent.docuagent.entity.Course;
import com.docuagent.docuagent.exception.BizException;
import com.docuagent.docuagent.mapper.ChunkMapper;
import com.docuagent.docuagent.mapper.CourseMapper;
import com.docuagent.docuagent.service.RetrievalService;
import com.docuagent.docuagent.vo.RetrievalCandidate;
import com.docuagent.docuagent.vo.RetrievalHitVO;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;

/** 向量与关键词检索的业务边界，只有数据库仍有效的分块才能进入引用。 */
@Service
public class RetrievalServiceImpl implements RetrievalService {
    private static final int MAX_CORPUS = 20000;
    private static final Set<String> MODES = Set.of("vector", "keyword", "both");
    private final SimpleVectorStore vectorStore;
    private final ChunkMapper chunks;
    private final CourseMapper courses;
    private final BatchReranker reranker;
    private final int defaultTopK;
    private final double threshold;
    private final int rrfK;
    private final int poolSize;

    public RetrievalServiceImpl(SimpleVectorStore vectorStore, ChunkMapper chunks, CourseMapper courses,
                                BatchReranker reranker, @Value("${docuagent.retrieval.top-k:5}") int topK,
                                @Value("${docuagent.retrieval.similarity-threshold:0.0}") double threshold,
                                @Value("${docuagent.retrieval.rrf-k:60}") int rrfK,
                                @Value("${docuagent.retrieval.candidate-limit:10}") int poolSize) {
        this.vectorStore = vectorStore;
        this.chunks = chunks;
        this.courses = courses;
        this.reranker = reranker;
        this.defaultTopK = topK;
        this.threshold = threshold;
        this.rrfK = rrfK;
        this.poolSize = poolSize;
        if (topK < 1 || topK > 10 || threshold < 0 || threshold > 1 || rrfK < 1 || poolSize < topK || poolSize > 10)
            throw new IllegalArgumentException("检索配置超出范围");
    }

    @Override
    public List<RetrievalHitVO> search(String query, Long courseId, String mode, boolean fusion, boolean rerank, Integer topK) {
        int limit = topK == null ? defaultTopK : topK;
        if (query == null || query.isBlank() || query.length() > 1000 || !MODES.contains(mode)
                || limit < 1 || limit > poolSize || (courseId != null && courseId < 1)
                || (fusion && !"both".equals(mode)) || (rerank && !fusion))
            throw new BizException(CodeEnum.INVALID_RETRIEVAL_REQUEST);
        if (courseId != null && courses.selectCount(new LambdaQueryWrapper<Course>().eq(Course::getId, courseId)) == 0)
            throw new BizException(CodeEnum.COURSE_NOT_FOUND);
        List<RetrievalCandidate> eligible = chunks.selectRetrievable(courseId, MAX_CORPUS + 1);
        if (eligible.size() > MAX_CORPUS) throw new BizException(CodeEnum.RETRIEVAL_LIMIT_EXCEEDED);
        if (eligible.isEmpty()) return List.of();
        List<RetrievalHitVO> vector = "keyword".equals(mode) ? List.of() : vector(query.trim(), courseId, eligible);
        if ("vector".equals(mode)) return take(vector, limit);
        List<RetrievalHitVO> keyword = RetrievalRanking.keyword(query, eligible, poolSize);
        if ("keyword".equals(mode)) return take(keyword, limit);
        if (!fusion) {
            // 未融合模式只并列展示两路，每路独立 topK，不混排不同量纲的分数。
            List<RetrievalHitVO> separate = new ArrayList<>(take(vector, limit));
            separate.addAll(take(keyword, limit));
            return separate;
        }
        List<RetrievalHitVO> fused = RetrievalRanking.fuse(vector, keyword, rrfK, poolSize);
        return take(rerank ? reranker.rerank(query, fused) : fused, limit);
    }

    private List<RetrievalHitVO> vector(String query, Long courseId, List<RetrievalCandidate> eligible) {
        Map<Long, RetrievalCandidate> byId = new HashMap<>();
        for (RetrievalCandidate candidate : eligible) byId.put(candidate.chunkId(), candidate);
        // SimpleVectorStore 本来就在内存全表扫描；先给过滤留足空间，孤儿不能抢占最终 topK。
        SearchRequest.Builder request = SearchRequest.builder().query(query).topK(MAX_CORPUS)
                .similarityThreshold(threshold);
        if (courseId != null) request.filterExpression("courseId == " + courseId);
        List<org.springframework.ai.document.Document> vectors = vectorStore.similaritySearch(request.build());
        List<RetrievalHitVO> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (org.springframework.ai.document.Document vector : vectors) {
            Long id = number(vector.getMetadata().get("chunkId"));
            RetrievalCandidate candidate = byId.get(id);
            // 双重校验课程、文档、正文和 ID，不能只信向量 metadata。
            if (candidate == null || !candidate.documentId().equals(number(vector.getMetadata().get("documentId")))
                    || !candidate.courseId().equals(number(vector.getMetadata().get("courseId")))
                    || !candidate.content().equals(vector.getText()) || !seen.add(id)) continue;
            result.add(new RetrievalHitVO(candidate.chunkId(), candidate.documentId(), candidate.documentName(),
                    vector.getScore() == null ? 0.0 : vector.getScore(), candidate.content(), "vector_cosine", null));
            if (result.size() == poolSize) break;
        }
        return result;
    }

    private static Long number(Object value) {
        try { return value == null ? null : Long.valueOf(value.toString()); }
        catch (NumberFormatException exception) { return null; }
    }

    private static List<RetrievalHitVO> take(List<RetrievalHitVO> hits, int limit) {
        return new ArrayList<>(hits.subList(0, Math.min(limit, hits.size())));
    }
}
