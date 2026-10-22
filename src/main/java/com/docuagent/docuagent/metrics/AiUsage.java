package com.docuagent.docuagent.metrics;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.*;

/** 请求内汇总供应商返回的真实token用量；不记录提示词、回答或凭证。 */
@Component
public class AiUsage {
    public record Snapshot(String id,long chatCalls,long embeddingCalls,long inputTokens,long outputTokens,long embeddingTokens,long missingUsageCalls,long elapsedMs) { }
    private static final class Bucket {long chat,embedding,input,output,embedded,missing;final long start=System.nanoTime();long elapsed;}
    private final ThreadLocal<String> current=new ThreadLocal<>();
    private final Map<String,Bucket> requests=new ConcurrentHashMap<>();
    private final Queue<String> order=new ConcurrentLinkedQueue<>();
    public String begin(){String id=UUID.randomUUID().toString();requests.put(id,new Bucket());order.add(id);while(order.size()>512)requests.remove(order.poll());return id;}
    public String current(){return current.get();}
    public void bind(String id){current.set(id);}
    public void unbind(){current.remove();}
    public void finish(String id){Bucket b=requests.get(id);if(b!=null)synchronized(b){b.elapsed=(System.nanoTime()-b.start)/1_000_000;}}
    public void record(String id,boolean embedding,Integer input,Integer output){
        Bucket b=id==null?null:requests.get(id);if(b==null)return;
        synchronized(b){if(embedding)b.embedding++;else b.chat++;if(input==null||output==null)b.missing++;
            if(embedding)b.embedded+=Math.max(0,input==null?0:input);else{b.input+=Math.max(0,input==null?0:input);b.output+=Math.max(0,output==null?0:output);}}
    }
    public Snapshot get(String id){Bucket b=requests.get(id);if(b==null)return null;synchronized(b){return new Snapshot(id,b.chat,b.embedding,b.input,b.output,b.embedded,b.missing,b.elapsed);}}
}
