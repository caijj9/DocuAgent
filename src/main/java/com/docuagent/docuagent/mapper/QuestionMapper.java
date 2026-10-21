package com.docuagent.docuagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docuagent.docuagent.entity.Question;
import org.apache.ibatis.annotations.*;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
    /** 事务中锁定题目，串行化同题答题的落库与错题累计。 */
    @Select("SELECT * FROM question WHERE id=#{id} AND deleted=0 FOR UPDATE")
    Question lockQuestion(@Param("id") Long id);
}
