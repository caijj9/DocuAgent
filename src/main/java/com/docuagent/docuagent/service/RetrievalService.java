package com.docuagent.docuagent.service;

import com.docuagent.docuagent.vo.RetrievalHitVO;
import java.util.List;

/** 检索入口，支持独立双路、融合和批量模型重排。 */
public interface RetrievalService {
    List<RetrievalHitVO> search(String query, Long courseId, String mode, boolean fusion, boolean rerank, Integer topK);
}
