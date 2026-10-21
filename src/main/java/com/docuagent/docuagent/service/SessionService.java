package com.docuagent.docuagent.service;

import com.docuagent.docuagent.vo.StudyViews.SessionView;
import java.util.List;

/** 单用户Redis会话；只保留最近六轮完整成功的问答。 */
public interface SessionService {
    record Turn(String user,String assistant) { }
    record Context(String sessionId,Long courseId,List<Turn> turns,String lockToken) { }
    SessionView create(Long courseId);
    List<SessionView> list();
    void delete(String id);
    Context open(String id,Long requestedCourse);
    void complete(Context context,String user,String assistant);
    void close(Context context);
}
