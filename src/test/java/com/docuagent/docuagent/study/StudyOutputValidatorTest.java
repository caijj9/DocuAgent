package com.docuagent.docuagent.study;

import com.docuagent.docuagent.ai.*;
import com.docuagent.docuagent.vo.RetrievalHitVO;
import com.docuagent.docuagent.service.impl.QaServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class StudyOutputValidatorTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private final StudyOutputValidator validator=new StudyOutputValidator(mapper);
    private final List<RetrievalHitVO> sources=List.of(new RetrievalHitVO(9L,1L,"课件",0.8,"两个知识要点"));
    private final String valid="[{\"stem\":\"主键是什么\",\"type\":\"single_choice\",\"options\":[\"A. 唯一标识\",\"B. 备份\"],\"answer\":\"A\",\"keyPoints\":[\"唯一标识\"],\"sourceChunkId\":9,\"sourceQuote\":\"两个知识要点\"}]";

    @Test void validQuestionShouldKeepOnlyTheRequestedCourseAndKnownSource() throws Exception {
        var questions=validator.questions(mapper.readTree(valid),2L,1,"single_choice",sources);
        assertThat(questions.get(0).getCourseId()).isEqualTo(2L);
        assertThat(questions.get(0).getSourceChunkId()).isEqualTo(9L);
    }
    @Test void unknownSourceInvalidChoiceWrongCountAndInvalidTypeShouldFailBeforePersistence(){
        for(String invalid:List.of(valid.replace("\"sourceChunkId\":9","\"sourceChunkId\":999"),valid.replace("\"answer\":\"A\"","\"answer\":\"F\""),"[]",valid.replace("single_choice","unknown"))){
            assertThatThrownBy(()->validator.questions(mapper.readTree(invalid),1L,1,"single_choice",sources)).isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void partialCreditIsCalculatedByServerAndNeverForcedToZeroOrFull() throws Exception {
        var grade=validator.grade(mapper.readTree("{\"points\":{\"P0\":\"要点一\",\"P1\":\"\"},\"comment\":\"只答对一点\"}"),List.of("要点一","要点二"),"要点一");
        assertThat(grade.score()).isEqualByComparingTo("5.00");assertThat(grade.correct()).isFalse();
        assertThat(grade.hit()).containsExactly("要点一");assertThat(grade.missed()).containsExactly("要点二");
    }
    @Test void missingOrUnknownGradingPointShouldFail() throws Exception {
        assertThatThrownBy(()->validator.grade(mapper.readTree("{\"points\":{\"P0\":\"\",\"P2\":\"\"},\"comment\":\"x\"}"),List.of("a","b"),"a")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->validator.grade(mapper.readTree("{\"points\":{\"P0\":\"标准答案\",\"P1\":\"\"},\"comment\":\"x\"}"),List.of("a","b"),"完全不知道")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void citationsAreRemovedIfOutOfRangeAndValidReferencesAreRenumbered() throws Exception {
        var result=QaServiceImpl.validateAnswer(mapper.readTree("{\"supported\":true,\"answer\":\"结论[99][1]。\"}"),sources,"问题",null);
        assertThat(result.answer()).isEqualTo("结论[1]。");assertThat(result.references()).hasSize(1);
        assertThat(result.references().get(0).chunkId()).isEqualTo(9L);
        assertThatThrownBy(()->QaServiceImpl.validateAnswer(mapper.readTree("{\"supported\":true,\"answer\":\"结论[99]\"}"),sources,"问题",null)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void refusalNeverReturnsUnrelatedReferences() throws Exception {
        var result=QaServiceImpl.validateAnswer(mapper.readTree("{\"supported\":false,\"answer\":\"资料不足\"}"),sources,"问题",null);
        assertThat(result.answer()).contains("资料中没有相关内容");assertThat(result.references()).isEmpty();
    }
    @Test void regexQuestionMarkFormulaCannotBeMisappliedToStar(){
        var references=List.of(new com.docuagent.docuagent.vo.StudyViews.Reference(1,1L,1L,"课件","r+ 等价于 rr*；r? 等价于 ε | r",0.9));
        assertThat(QaServiceImpl.equationsSupported("r+等价于rr*[1]。",references)).isTrue();
        assertThat(QaServiceImpl.equationsSupported("r*等价于ε | r[1]。",references)).isFalse();
        assertThat(QaServiceImpl.equationsSupported("r*表示零个或多个实例，等价于ε | r[1]。",references)).isFalse();
    }
    @Test void evidenceChoiceHasTwoExclusiveOptionsAndServerAssignedAnswer() throws Exception {
        var material=List.of(new RetrievalHitVO(9L,1L,"课件",0.8,"• 词法分析器读入字符流，输出词法单元序列"));
        for(int i=0;i<8;i++){
            var q=validator.questions(mapper.readTree("[{\"type\":\"single_choice\",\"assertionId\":\"A0\"}]"),1L,1,"single_choice",material).get(0);
            var options=mapper.readTree(q.getOptions());assertThat(options.size()).isEqualTo(2);
            String selected=options.get(q.getAnswer().charAt(0)-'A').asText();
            assertThat(selected).endsWith(q.getStem().contains("并非")?"错误":"正确");
            assertThat(q.isEvidenceChoice()).isTrue();assertThat(q.getSourceChunkId()).isEqualTo(9L);
        }
        assertThatThrownBy(()->validator.questions(mapper.readTree("[{\"type\":\"single_choice\",\"assertionId\":\"UNKNOWN\"}]"),1L,1,"single_choice",material))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void actionsWithoutASubjectMustNotBecomeStandaloneChoiceStatements(){
        var material=List.of(new RetrievalHitVO(9L,1L,"课件",0.8,"Ø 读入字符流，组成词素，输出词法单元序列\n• 词法分析器可以首先完成一些简单的处理工作"));
        var assertions=StudyAssertions.from(material);
        assertThat(assertions).hasSize(1);assertThat(assertions.get("A0").text()).startsWith("词法分析器");
    }
    @Test void trueButIrrelevantQuestionMarkFactsCannotAnswerAStarComparison(){
        assertThat(QaServiceImpl.symbolsCovered("r+与r*有什么区别？","r+等价于rr*，r?等价于ε|r")).isFalse();
        assertThat(QaServiceImpl.symbolsCovered("r+与r*有什么区别？","r+表示一次或多次，r*表示零次或多次")).isTrue();
        assertThat(QaServiceImpl.symbolsCovered("C++和Java有什么区别？","比较Java与C++")).isTrue();
    }
}
