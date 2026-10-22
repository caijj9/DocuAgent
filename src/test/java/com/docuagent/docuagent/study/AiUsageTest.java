package com.docuagent.docuagent.study;
import com.docuagent.docuagent.metrics.AiUsage;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class AiUsageTest {
    @Test void toolRoundsAndEmbeddingMustBeCountedSeparatelyAndUnknownUsageMarked(){
        var usage=new AiUsage();String a=usage.begin(),b=usage.begin();usage.record(a,false,100,10);usage.record(a,false,200,20);usage.record(a,true,50,0);usage.record(b,false,null,null);
        assertThat(usage.get(a).chatCalls()).isEqualTo(2);assertThat(usage.get(a).inputTokens()).isEqualTo(300);assertThat(usage.get(a).outputTokens()).isEqualTo(30);assertThat(usage.get(a).embeddingTokens()).isEqualTo(50);assertThat(usage.get(a).missingUsageCalls()).isZero();assertThat(usage.get(b).missingUsageCalls()).isEqualTo(1);
    }
}
