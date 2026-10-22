package com.docuagent.docuagent.metrics;

import io.micrometer.observation.*;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Component;

/** 观察底层模型调用，包括工具触发的再次调用，而不是只读最后一次ChatClient结果。 */
@Component
public class UsageObservationHandler implements ObservationHandler<Observation.Context> {
    private final AiUsage usage;
    public UsageObservationHandler(AiUsage usage){this.usage=usage;}
    public boolean supportsContext(Observation.Context context){return context instanceof ChatModelObservationContext||context instanceof EmbeddingModelObservationContext;}
    public void onStart(Observation.Context context){if(usage.current()!=null)context.put("docuagent.usageId",usage.current());}
    public void onStop(Observation.Context context){
        Usage tokens=null;boolean embedding=context instanceof EmbeddingModelObservationContext;
        if(context instanceof ChatModelObservationContext c&&c.getResponse()!=null)tokens=c.getResponse().getMetadata().getUsage();
        if(context instanceof EmbeddingModelObservationContext c&&c.getResponse()!=null)tokens=c.getResponse().getMetadata().getUsage();
        usage.record(context.get("docuagent.usageId"),embedding,tokens==null?null:tokens.getPromptTokens(),embedding?0:tokens==null?null:tokens.getCompletionTokens());
    }
}
