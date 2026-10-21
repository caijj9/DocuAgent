package com.docuagent.docuagent.retrieval;

import com.docuagent.docuagent.ai.retrieval.RetrievalRanking;
import com.docuagent.docuagent.vo.RetrievalCandidate;
import com.docuagent.docuagent.vo.RetrievalHitVO;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class RetrievalRankingTest {
    private RetrievalHitVO hit(long id) { return new RetrievalHitVO(id, 1L, "课件", 0.8, "原文"); }

    @Test void rrfShouldDeduplicateByChunkAndRewardTwoChannelAgreement() {
        var result = RetrievalRanking.fuse(List.of(hit(1), hit(1), hit(2)), List.of(hit(2), hit(3)), 60, 10);
        assertThat(result).extracting(RetrievalHitVO::chunkId).containsExactly(2L, 1L, 3L);
        assertThat(result.get(0).score()).isCloseTo(1.0 / 62 + 1.0 / 61, within(1e-10));
        assertThat(result).allMatch(h -> "rrf".equals(h.scoreType()));
    }

    @Test void keywordShouldPreserveRegexPlusAndCapRepeatedOccurrences() {
        var candidates = List.of(new RetrievalCandidate(1L, 1L, 1L, "词法分析", "r+ 等价于 rr*"),
                new RetrievalCandidate(2L, 1L, 1L, "词法分析", "r* 表示零个或多个"));
        assertThat(RetrievalRanking.keyword("r+", candidates, 5)).extracting(RetrievalHitVO::chunkId).containsExactly(1L);
        var capped = RetrievalRanking.keyword("词法", List.of(new RetrievalCandidate(3L, 1L, 1L, "课件", "词法".repeat(10000))), 5);
        assertThat(capped.get(0).score()).isLessThan(1.0);
    }
}
