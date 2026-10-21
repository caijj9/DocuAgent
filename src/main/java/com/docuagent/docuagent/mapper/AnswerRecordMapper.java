package com.docuagent.docuagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docuagent.docuagent.entity.AnswerRecord;
import com.docuagent.docuagent.vo.StudyViews.AnswerHistory;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AnswerRecordMapper extends BaseMapper<AnswerRecord> {
    @Select("SELECT a.question_id AS questionId,LEFT(q.stem,200) AS stem,LEFT(a.user_answer,1000) AS myAnswer,a.score,a.max_score AS maxScore,"
            +"COALESCE(w.wrong_count,0) AS wrongCount,w.source,a.create_time AS answeredAt,a.missed_points AS missedPoints "
            +"FROM answer_record a JOIN question q ON q.id=a.question_id JOIN course c ON c.id=q.course_id "
            +"LEFT JOIN wrong_question w ON w.question_id=q.id AND w.deleted=0 "
            +"WHERE a.deleted=0 AND q.deleted=0 AND c.deleted=0 AND q.course_id=#{courseId} "
            +"AND a.create_time >= DATE_SUB(NOW(),INTERVAL #{days} DAY) ORDER BY a.id DESC LIMIT 20")
    List<AnswerHistory> history(@Param("courseId") Long courseId,@Param("days") int days);
}
