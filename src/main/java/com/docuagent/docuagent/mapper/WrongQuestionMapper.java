package com.docuagent.docuagent.mapper;

import com.docuagent.docuagent.vo.StudyViews.WrongView;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface WrongQuestionMapper {
    /** 与答题记录在同一事务内执行；数据库唯一键保证同题只有一行。 */
    @Insert("INSERT INTO wrong_question(question_id,wrong_count,source,first_wrong_time,last_wrong_time) "
            +"VALUES(#{questionId},1,'auto',NOW(),NOW()) ON DUPLICATE KEY UPDATE "
            +"wrong_count=wrong_count+1,source='auto',deleted=0,first_wrong_time=COALESCE(first_wrong_time,NOW()),last_wrong_time=NOW()")
    int addAutomatic(@Param("questionId") Long questionId);

    @Insert("INSERT INTO wrong_question(question_id,wrong_count,source) VALUES(#{questionId},0,'manual') "
            +"ON DUPLICATE KEY UPDATE source='manual',deleted=0")
    int saveManual(@Param("questionId") Long questionId);

    @Select("<script>SELECT q.id AS questionId,q.stem,q.type,a.user_answer AS myAnswer,q.answer AS correctAnswer,"
            +"w.wrong_count AS wrongCount,w.source,w.last_wrong_time AS lastWrongTime "
            +"FROM wrong_question w JOIN question q ON q.id=w.question_id JOIN course c ON c.id=q.course_id "
            +"LEFT JOIN answer_record a ON a.id=(SELECT MAX(a2.id) FROM answer_record a2 WHERE a2.question_id=q.id AND a2.deleted=0) "
            +"WHERE w.deleted=0 AND q.deleted=0 AND c.deleted=0 "
            +"<if test='courseId != null'>AND q.course_id=#{courseId} </if>"
            +"ORDER BY w.update_time DESC,w.id DESC LIMIT #{size} OFFSET #{offset}</script>")
    List<WrongView> list(@Param("courseId") Long courseId,@Param("offset") int offset,@Param("size") int size);
}
