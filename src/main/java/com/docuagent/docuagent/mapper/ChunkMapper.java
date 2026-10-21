package com.docuagent.docuagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docuagent.docuagent.entity.Chunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.docuagent.docuagent.vo.RetrievalCandidate;
import java.util.List;

@Mapper
public interface ChunkMapper extends BaseMapper<Chunk> {
    /** 同时过滤分块、课件、课程，避免孤儿向量或已删除资料对外提供引用。 */
    @Select("<script>SELECT c.id AS chunkId, d.id AS documentId, d.course_id AS courseId, "
            + "d.original_name AS documentName, c.content FROM chunk c "
            + "JOIN document d ON d.id=c.document_id "
            + "JOIN course k ON k.id=d.course_id "
            + "WHERE c.deleted=0 AND d.deleted=0 AND k.deleted=0 AND d.status='DONE' "
            + "<if test='courseId != null'>AND d.course_id=#{courseId} </if>"
            + "ORDER BY c.id LIMIT #{limit}</script>")
    List<RetrievalCandidate> selectRetrievable(@Param("courseId") Long courseId, @Param("limit") int limit);
}
