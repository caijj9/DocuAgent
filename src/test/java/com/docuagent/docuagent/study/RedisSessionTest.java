package com.docuagent.docuagent.study;

import com.docuagent.docuagent.service.SessionService;
import com.docuagent.docuagent.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;

/** 使用独立随机会话验证真实Redis窗口、删除、并发锁，不调用模型。 */
@SpringBootTest
class RedisSessionTest {
    @Autowired SessionService sessions;
    @Test void onlySixSuccessfulTurnsShouldRemainAndAnotherRequestMustNotStealLock(){
        var created=sessions.create(null);String id=created.sessionId();
        try{
            for(int i=0;i<8;i++){
                var context=sessions.open(id,null);
                try{sessions.complete(context,"q"+i,"a"+i);}finally{sessions.close(context);}
            }
            var context=sessions.open(id,null);
            try{
                assertThat(context.turns()).hasSize(6);assertThat(context.turns().get(0).user()).isEqualTo("q2");
                assertThatThrownBy(()->sessions.open(id,null)).isInstanceOf(BizException.class);
                sessions.complete(context,"q8","a8");
            }finally{sessions.close(context);}
            assertThatThrownBy(()->sessions.open(id,1L)).isInstanceOf(BizException.class);
            assertThat(sessions.list()).anyMatch(s->s.sessionId().equals(id));
        }finally{sessions.delete(id);}
        assertThatThrownBy(()->sessions.open(id,null)).isInstanceOf(BizException.class);
    }
}
