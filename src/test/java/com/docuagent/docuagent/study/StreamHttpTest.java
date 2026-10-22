package com.docuagent.docuagent.study;
import com.docuagent.docuagent.service.QaService;
import com.docuagent.docuagent.vo.StudyViews.*;
import com.docuagent.docuagent.exception.BizException;
import com.docuagent.docuagent.common.CodeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class StreamHttpTest {
    @LocalServerPort int port;
    @MockitoBean QaService qa;
    private HttpResponse<String> send()throws Exception{
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/qa/stream")).timeout(Duration.ofSeconds(20)).header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"question\":\"测试问题\",\"courseId\":1}")).build();
        return HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());
    }
    @Test void checkedTextMustBeSentAsSeparateUnicodeEventsAndFinishWithDone()throws Exception{
        when(qa.ask(any())).thenReturn(new Qa("课件𝑀结论[1]",List.of(new Reference(1,1L,1L,"测试课件","原文",.8)),"测试问题",null));
        var response=send();assertThat(response.statusCode()).isEqualTo(200);assertThat(response.headers().firstValue("Content-Type").orElse("")).contains("text/event-stream");
        assertThat(response.body()).contains("event:status","event:references","event:delta","event:done");
        var json=new com.fasterxml.jackson.databind.ObjectMapper();StringBuilder actual=new StringBuilder();
        for(String packet:response.body().split("\\n\\n"))if(packet.startsWith("event:delta")){
            String data=packet.substring(packet.indexOf("data:")+5);actual.append(json.readTree(data).get("text").asText());
        }
        assertThat(actual.toString()).isEqualTo("课件𝑀结论[1]");
        assertThat(response.body().split("event:delta",-1).length-1).isEqualTo("课件𝑀结论[1]".codePointCount(0,"课件𝑀结论[1]".length()));
        verify(qa).ask(any());
    }
    @Test void businessFailureMustProduceAnErrorEventWithoutPretendingToFinishAnAnswer()throws Exception{
        when(qa.ask(any())).thenThrow(new BizException(CodeEnum.MODEL_OUTPUT_INVALID));var response=send();
        assertThat(response.statusCode()).isEqualTo(200);assertThat(response.body()).contains("event:error","1101").doesNotContain("event:done","event:delta");
    }
}
