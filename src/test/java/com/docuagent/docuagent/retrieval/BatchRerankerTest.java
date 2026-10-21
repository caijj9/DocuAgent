package com.docuagent.docuagent.retrieval;

import com.docuagent.docuagent.ai.retrieval.BatchReranker;
import com.docuagent.docuagent.vo.RetrievalHitVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.*;

class BatchRerankerTest {
    private final BatchReranker reranker = new BatchReranker(mock(ChatModel.class), new ObjectMapper());
    private final List<RetrievalHitVO> candidates = List.of(new RetrievalHitVO(1L,1L,"a",0.1,"a"),new RetrievalHitVO(2L,1L,"b",0.2,"b"));

    @Test void validScoresShouldReorderCandidates() throws Exception {
        var result = reranker.parse("{\"scores\":{\"C0\":3,\"C1\":9}}", candidates);
        assertThat(result).extracting(RetrievalHitVO::chunkId).containsExactly(2L,1L);
        assertThat(result.get(0).scoreType()).isEqualTo("llm_relevance_0_10");
    }

    @Test void missingDuplicateUnknownAndOutOfRangeScoresShouldFail() {
        for (String json : List.of("{\"scores\":{}}",
                "{\"scores\":{\"C0\":3}}",
                "{\"scores\":{\"C0\":3,\"C2\":5}}",
                "{\"scores\":{\"C0\":11,\"C1\":4}}")) {
            assertThatThrownBy(() -> reranker.parse(json,candidates)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test void duplicateKeysAndTrailingJsonShouldFail() {
        assertThatThrownBy(() -> reranker.parse("{\"scores\":{\"C0\":3,\"C0\":4,\"C1\":5}}",candidates))
                .isInstanceOf(com.fasterxml.jackson.core.JsonParseException.class);
        assertThatThrownBy(() -> reranker.parse("{\"scores\":{\"C0\":3,\"C1\":5}} {}",candidates))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
