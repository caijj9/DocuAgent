package com.docuagent.docuagent.service.impl;

import com.docuagent.docuagent.common.CodeEnum;
import com.docuagent.docuagent.exception.BizException;
import com.docuagent.docuagent.mapper.CourseMapper;
import com.docuagent.docuagent.service.SessionService;
import com.docuagent.docuagent.vo.StudyViews.SessionView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

/** 会话metadata、历史和有期限的处理锁均按UUID隔离，不复用CampusMart键。 */
@Service
public class RedisSessionService implements SessionService {
    private static final String REGISTRY="docuagent:sessions";
    private static final Duration TTL=Duration.ofDays(7);
    private final StringRedisTemplate redis; private final ObjectMapper mapper; private final CourseMapper courses;
    public RedisSessionService(StringRedisTemplate redis,ObjectMapper mapper,CourseMapper courses){this.redis=redis;this.mapper=mapper;this.courses=courses;}
    private String key(String id){
        if(id==null||!id.matches("[0-9a-fA-F-]{36}"))throw new BizException(CodeEnum.SESSION_NOT_FOUND);
        return "docuagent:session:"+id;
    }
    @Override public SessionView create(Long courseId){
        if(courseId!=null&&courses.selectById(courseId)==null)throw new BizException(CodeEnum.COURSE_NOT_FOUND);
        String id=UUID.randomUUID().toString(),created=Instant.now().toString();
        try{
            redis.opsForHash().putAll(key(id),Map.of("courseId",courseId==null?"0":courseId.toString(),"createdAt",created));
            redis.expire(key(id),TTL);redis.opsForZSet().add(REGISTRY,id,System.currentTimeMillis());
            return new SessionView(id,courseId,created);
        }catch(Exception exception){throw new BizException(CodeEnum.SESSION_STORE_UNAVAILABLE);}
    }
    @Override public List<SessionView> list(){
        try{
            List<SessionView> result=new ArrayList<>();Set<String> ids=redis.opsForZSet().reverseRange(REGISTRY,0,99);
            if(ids==null)return result;
            for(String id:ids){Map<Object,Object> data=redis.opsForHash().entries(key(id));
                if(data.isEmpty()){redis.opsForZSet().remove(REGISTRY,id);continue;}
                long course=Long.parseLong(data.get("courseId").toString());
                result.add(new SessionView(id,course==0?null:course,data.get("createdAt").toString()));}
            return result;
        }catch(Exception exception){throw new BizException(CodeEnum.SESSION_STORE_UNAVAILABLE);}
    }
    @Override public Context open(String id,Long requestedCourse){
        if(id==null){if(requestedCourse!=null&&courses.selectById(requestedCourse)==null)throw new BizException(CodeEnum.COURSE_NOT_FOUND);
            return new Context(null,requestedCourse,List.of(),null);}
        String base=key(id),token=UUID.randomUUID().toString();
        try{
            if(!Boolean.TRUE.equals(redis.hasKey(base)))throw new BizException(CodeEnum.SESSION_NOT_FOUND);
            if(!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(base+":lock",token,Duration.ofMinutes(10))))throw new BizException(CodeEnum.SESSION_BUSY);
            Map<Object,Object> data=redis.opsForHash().entries(base);
            if(data.isEmpty())throw new BizException(CodeEnum.SESSION_NOT_FOUND);
            long course=Long.parseLong(data.get("courseId").toString());Long scope=course==0?null:course;
            if(requestedCourse!=null&&!requestedCourse.equals(scope))throw new BizException(CodeEnum.SESSION_COURSE_MISMATCH);
            if(scope!=null&&courses.selectById(scope)==null)throw new BizException(CodeEnum.COURSE_NOT_FOUND);
            List<Turn> turns=new ArrayList<>();List<String> values=redis.opsForList().range(base+":history",0,-1);
            if(values!=null)for(String value:values)turns.add(mapper.readValue(value,Turn.class));
            return new Context(id,scope,turns,token);
        }catch(BizException exception){release(base,token);throw exception;}
        catch(Exception exception){release(base,token);throw new BizException(CodeEnum.SESSION_STORE_UNAVAILABLE);}
    }
    @Override public void complete(Context context,String user,String assistant){
        if(context.sessionId()==null)return;
        String base=key(context.sessionId());
        try{
            var script=new DefaultRedisScript<Long>("if redis.call('GET',KEYS[3])~=ARGV[2] or redis.call('EXISTS',KEYS[1])==0 then return 0 end "
                    +"redis.call('RPUSH',KEYS[2],ARGV[1]);redis.call('LTRIM',KEYS[2],-6,-1);redis.call('EXPIRE',KEYS[1],604800);redis.call('EXPIRE',KEYS[2],604800);return 1",Long.class);
            Long saved=redis.execute(script,List.of(base,base+":history",base+":lock"),mapper.writeValueAsString(new Turn(user,assistant)),context.lockToken());
            if(!Long.valueOf(1).equals(saved))throw new BizException(CodeEnum.SESSION_NOT_FOUND);
        }catch(BizException exception){throw exception;}
        catch(Exception exception){throw new BizException(CodeEnum.SESSION_STORE_UNAVAILABLE);}
    }
    @Override public void close(Context context){if(context.sessionId()!=null)release(key(context.sessionId()),context.lockToken());}
    @Override public void delete(String id){Context context=open(id,null);try{redis.delete(List.of(key(id),key(id)+":history"));redis.opsForZSet().remove(REGISTRY,id);}finally{close(context);}}
    private void release(String base,String token){
        try{redis.execute(new DefaultRedisScript<Long>("if redis.call('GET',KEYS[1])==ARGV[1] then return redis.call('DEL',KEYS[1]) else return 0 end",Long.class),List.of(base+":lock"),token);}
        catch(Exception ignored){/* 带TTL的锁最终会释放，不覆盖原始业务异常。 */}
    }
}
