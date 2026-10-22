package com.docuagent.docuagent.retrieval;
import com.docuagent.docuagent.ai.chunk.SimpleChunker;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
class UnicodeChunkerTest {
    @Test void mathematicalSymbolsMustNotBeSplitIntoInvalidSurrogateHalves(){
        var chunker=new SimpleChunker();ReflectionTestUtils.setField(chunker,"chunkSize",2);ReflectionTestUtils.setField(chunker,"overlap",0);
        var chunks=chunker.split("A𝑀𝑁B");assertThat(chunks).containsExactly("A𝑀","𝑁B");assertThat(String.join("",chunks)).isEqualTo("A𝑀𝑁B");
    }
}
