package com.docuagent.docuagent.controller;

import com.docuagent.docuagent.common.Result;
import com.docuagent.docuagent.service.RetrievalService;
import com.docuagent.docuagent.vo.RetrievalHitVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** 检索接口只接收参数，过滤、融合、重排由业务层处理。 */
@RestController
@RequestMapping("/retrieval")
public class RetrievalController {
    private final RetrievalService service;
    private final boolean defaultFusion;
    private final boolean defaultRerank;

    public RetrievalController(RetrievalService service,
            @Value("${docuagent.retrieval.fusion-enabled:false}") boolean fusion,
            @Value("${docuagent.retrieval.rerank-enabled:false}") boolean rerank) {
        this.service = service;
        this.defaultFusion = fusion;
        this.defaultRerank = rerank;
    }

    @GetMapping("/search")
    public Result<List<RetrievalHitVO>> search(@RequestParam String q,
            @RequestParam(required = false) Long courseId,
            @RequestParam(defaultValue = "vector") String mode,
            @RequestParam(required = false) Boolean fusion,
            @RequestParam(required = false) Boolean rerank,
            @RequestParam(required = false) Integer topK) {
        return Result.success(service.search(q, courseId, mode,
                fusion == null ? defaultFusion : fusion,
                rerank == null ? defaultRerank : rerank, topK));
    }
}