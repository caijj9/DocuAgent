package com.docuagent.docuagent.controller;

import com.docuagent.docuagent.dto.StudyRequests.Ask;
import com.docuagent.docuagent.exception.BizException;
import com.docuagent.docuagent.metrics.AiUsage;
import com.docuagent.docuagent.service.QaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** 保留引用复核：核准完整回答后，在服务端按Unicode字符分段推送；不将未核验模型草稿直传。 */
@RestController
public class QaStreamController {
    private final QaService qa;private final ThreadPoolTaskExecutor executor;private final AiUsage usage;
    public QaStreamController(QaService qa,ThreadPoolTaskExecutor qaStreamExecutor,AiUsage usage){this.qa=qa;this.executor=qaStreamExecutor;this.usage=usage;}
    @PostMapping(value="/qa/stream",produces=MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(@Valid @RequestBody Ask request,HttpServletRequest http)throws Exception{
        SseEmitter emitter=new SseEmitter(240_000L);String usageId=(String)http.getAttribute("usageId");
        AtomicBoolean finished=new AtomicBoolean(false),cancelled=new AtomicBoolean(false);AtomicReference<Future<?>> task=new AtomicReference<>();
        Runnable cancel=()->{cancelled.set(true);Future<?> f=task.get();if(f!=null&&!finished.get())f.cancel(true);};
        emitter.onTimeout(cancel);emitter.onError(error->cancel.run());emitter.onCompletion(()->{if(!finished.get())cancel.run();});
        emitter.send(SseEmitter.event().name("status").data(Map.of("text","正在查找课件并核对回答依据")));
        try{task.set(executor.submit(()->{
            usage.bind(usageId);
            try{
                var answer=qa.ask(request);if(cancelled.get())return;
                emitter.send(SseEmitter.event().name("references").data(answer.references()));
                for(int point:answer.answer().codePoints().toArray()){
                    if(cancelled.get()||Thread.currentThread().isInterrupted())return;
                    emitter.send(SseEmitter.event().name("delta").data(Map.of("text",new String(Character.toChars(point)))));
                    Thread.sleep(18);
                }
                emitter.send(SseEmitter.event().name("done").data(answer));finished.set(true);emitter.complete();
            }catch(Exception error){
                if(!cancelled.get())try{
                    int code=error instanceof BizException b?b.getCode():1100;
                    String message=error instanceof BizException b?b.getMessage():"回答暂时不可用，请稍后重试";
                    emitter.send(SseEmitter.event().name("error").data(Map.of("code",code,"msg",message)));finished.set(true);emitter.complete();
                }catch(Exception ignored){emitter.completeWithError(error);}
            }finally{usage.finish(usageId);usage.unbind();}
        }));}catch(RejectedExecutionException busy){finished.set(true);emitter.send(SseEmitter.event().name("error").data(Map.of("code",1106,"msg","当前回答任务较多，请稍后重试")));emitter.complete();}
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).header("X-Accel-Buffering","no").contentType(MediaType.TEXT_EVENT_STREAM).body(emitter);
    }
}
