package com.docuagent.docuagent.study;

import com.docuagent.docuagent.ai.*;
import com.docuagent.docuagent.common.CodeEnum;
import com.docuagent.docuagent.dto.StudyRequests.*;
import com.docuagent.docuagent.entity.*;
import com.docuagent.docuagent.exception.BizException;
import com.docuagent.docuagent.mapper.*;
import com.docuagent.docuagent.service.RetrievalService;
import com.docuagent.docuagent.service.impl.LearningServiceImpl;
import com.docuagent.docuagent.vo.RetrievalHitVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class LearningServiceTest {
    private final QuestionMapper questions=mock(QuestionMapper.class);
    private final AnswerRecordMapper answers=mock(AnswerRecordMapper.class);
    private final WrongQuestionMapper wrong=mock(WrongQuestionMapper.class);
    private final CourseMapper courses=mock(CourseMapper.class);
    private final ChunkMapper chunks=mock(ChunkMapper.class);
    private final RetrievalService retrieval=mock(RetrievalService.class);
    private final StudyModel model=mock(StudyModel.class);
    private final ObjectMapper mapper=new ObjectMapper();
    private final TransactionTemplate transactions=mock(TransactionTemplate.class);
    private final LearningServiceImpl service=new LearningServiceImpl(questions,answers,wrong,courses,chunks,retrieval,model,new StudyOutputValidator(mapper),mapper,transactions);

    @Test void malformedModelQuestionMustNotWriteAnyRows() throws Exception {
        when(courses.selectById(1L)).thenReturn(new Course());
        when(retrieval.search(anyString(),eq(1L),eq("both"),eq(true),eq(false),eq(10))).thenReturn(List.of(new RetrievalHitVO(1L,1L,"课件",0.5,"原文")));
        when(model.json(anyString(),any(),anyInt())).thenReturn(mapper.readTree("{\"notAnArray\":true}"));
        assertThatThrownBy(()->service.generate(new Generate(1L,5,"mixed",null))).isInstanceOf(BizException.class);
        verify(questions,never()).insert(any(Question.class));verifyNoInteractions(transactions);
    }
    @Test void replayReturnsOriginalResultWithoutCallingModelOrIncrementingWrongCount(){
        AnswerRecord row=new AnswerRecord();row.setId(10L);row.setQuestionId(2L);row.setUserAnswer("B");row.setDurationMs(20);row.setRequestId("same-request");
        row.setScore(BigDecimal.ZERO);row.setMaxScore(BigDecimal.TEN);row.setIsCorrect(0);row.setComment("错误");row.setHitPoints("[]");row.setMissedPoints("[\"要点\"]");
        when(answers.selectOne(any())).thenReturn(row);
        var result=service.submit(new Submit(2L,"B",20,"same-request"));assertThat(result.answerRecordId()).isEqualTo(10L);
        verifyNoInteractions(model,wrong,transactions,questions);
        assertThatThrownBy(()->service.submit(new Submit(2L,"A",20,"same-request"))).isInstanceOf(BizException.class);
    }

    @Test void failedSecondTypeOfMixedGenerationMustNotPersistTheFirstType() throws Exception {
        when(courses.selectById(1L)).thenReturn(new Course());
        when(retrieval.search(anyString(),eq(1L),eq("both"),eq(true),eq(false),eq(10)))
                .thenReturn(List.of(new RetrievalHitVO(9L,1L,"课件",0.5,"• 词法分析器读入字符流，输出词法单元序列")));
        when(model.json(anyString(),any(),anyInt())).thenReturn(mapper.readTree("[{\"type\":\"single_choice\",\"assertionId\":\"A0\"}]"),mapper.readTree("{\"invalid\":true}"));
        assertThatThrownBy(()->service.generate(new Generate(1L,2,"mixed",null))).isInstanceOf(BizException.class);
        verify(model,times(2)).json(anyString(),any(),anyInt());verifyNoInteractions(questions,transactions);
    }

    @Test void failedIndependentReviewMustNotPersistEvenWhenJsonFormatIsValid() throws Exception {
        when(courses.selectById(1L)).thenReturn(new Course());
        when(retrieval.search(anyString(),eq(1L),eq("both"),eq(true),eq(false),eq(10))).thenReturn(List.of(new RetrievalHitVO(9L,1L,"课件",0.5,"原文明确说明主键唯一标识记录")));
        var generated=mapper.readTree("[{\"stem\":\"主键作用\",\"type\":\"single_choice\",\"options\":[\"A. 标识记录\",\"B. 备份\"],\"answer\":\"A\",\"keyPoints\":[\"唯一标识\"],\"sourceChunkId\":9,\"sourceQuote\":\"原文明确说明主键唯一标识记录\"}]");
        when(model.json(anyString(),any(),anyInt())).thenReturn(generated,mapper.readTree("{\"supported\":{\"Q0\":false}}"));
        assertThatThrownBy(()->service.generate(new Generate(1L,1,"single_choice",null))).isInstanceOf(BizException.class);
        verify(questions,never()).insert(any(Question.class));verifyNoInteractions(transactions);
    }

    @Test void freshChoiceSubmissionNeverCallsModelAndUpdatesWrongCountInTransaction(){
        Question q=new Question();q.setId(2L);q.setCourseId(1L);q.setType("single_choice");q.setAnswer("A");q.setKeyPoints("[\"要点\"]");
        when(questions.selectById(2L)).thenReturn(q);when(questions.lockQuestion(2L)).thenReturn(q);when(courses.selectById(1L)).thenReturn(new Course());
        when(answers.insert(any(AnswerRecord.class))).thenAnswer(i->{((AnswerRecord)i.getArgument(0)).setId(99L);return 1;});
        when(transactions.execute(any())).thenAnswer(i->((TransactionCallback<?>)i.getArgument(0)).doInTransaction(null));
        var result=service.submit(new Submit(2L,"B",100,"fresh-choice"));
        assertThat(result.score()).isEqualByComparingTo("0.00");verifyNoInteractions(model);verify(wrong).addAutomatic(2L);
    }

    @Test void multipleCorrectOptionsFromBlindReviewMustNeverBeSaved() throws Exception {
        when(courses.selectById(1L)).thenReturn(new Course());
        when(retrieval.search(anyString(),eq(1L),eq("both"),eq(true),eq(false),eq(10))).thenReturn(List.of(new RetrievalHitVO(9L,1L,"课件",0.5,"可以删除字符，也可以插入字符")));
        var generated=mapper.readTree("[{\"stem\":\"哪项可恢复\",\"type\":\"single_choice\",\"options\":[\"A. 删除字符\",\"B. 插入字符\"],\"answer\":\"A\",\"keyPoints\":[\"删除字符\"],\"sourceChunkId\":9}]");
        var review=mapper.readTree("{\"supported\":{\"Q0\":true},\"choiceAnswers\":{\"Q0\":[\"A\",\"B\"]}}");
        when(model.json(anyString(),any(),anyInt())).thenReturn(generated,review,generated,review);
        assertThatThrownBy(()->service.generate(new Generate(1L,1,"single_choice",null))).isInstanceOf(BizException.class);
        verify(questions,never()).insert(any(Question.class));verifyNoInteractions(transactions);
        var inputs=org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(model,times(4)).json(anyString(),inputs.capture(),anyInt());
        var blindInput=(java.util.Map<?,?>)inputs.getAllValues().get(1);
        var candidates=(java.util.List<?>)blindInput.get("questions");
        assertThat(((java.util.Map<?,?>)candidates.get(0)).containsKey("answer")).isFalse();
    }
    @Test void manualCollectionMustNotUseAutomaticCounter(){
        Question question=new Question();question.setId(2L);question.setCourseId(1L);
        when(questions.selectById(2L)).thenReturn(question);when(courses.selectById(1L)).thenReturn(new Course());
        service.saveManual(1L,2L);verify(wrong).saveManual(2L);verify(wrong,never()).addAutomatic(anyLong());
        assertThatThrownBy(()->service.saveManual(3L,2L)).isInstanceOf(BizException.class);
    }
}
