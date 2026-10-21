package com.docuagent.docuagent.service;

import com.docuagent.docuagent.dto.StudyRequests.*;
import com.docuagent.docuagent.vo.StudyViews.*;
import java.util.List;

/** 出题、答题与错题业务；模型结果必须在事务落库之前校验。 */
public interface LearningService {
    List<QuestionView> generate(Generate request);
    AnswerView submit(Submit request);
    List<WrongView> wrongQuestions(Long courseId,int page,int size);
    List<AnswerHistory> history(Long courseId,int days);
    String saveManual(Long courseId,Long questionId);
}
