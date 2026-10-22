package com.docuagent.docuagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docuagent.docuagent.ai.chunk.SimpleChunker;
import com.docuagent.docuagent.common.CodeEnum;
import com.docuagent.docuagent.entity.Chunk;
import com.docuagent.docuagent.entity.Document;
import com.docuagent.docuagent.exception.BizException;
import com.docuagent.docuagent.mapper.ChunkMapper;
import com.docuagent.docuagent.mapper.DocumentMapper;
import com.docuagent.docuagent.mapper.CourseMapper;
import com.docuagent.docuagent.service.DocumentService;
import com.docuagent.docuagent.vo.DocumentUploadVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DocumentServiceImpl implements DocumentService {
    private static final Set<String> ALLOWED_EXT =
            Set.of("pdf", "ppt", "pptx", "doc", "docx", "txt");
    @Autowired
    private DocumentMapper documentMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private ChunkMapper chunkMapper;
    @Autowired
    private SimpleChunker simpleChunker;
    @Autowired
    private SimpleVectorStore vectorStore;
    @Value("${docuagent.vector-store.path:data/vector-store.json}")
    private String storePath;

    @Override public List<com.docuagent.docuagent.vo.DocumentSummaryVO> list(Long courseId){
        if(courseId==null||courseId<1||courseMapper.selectById(courseId)==null)throw new BizException(CodeEnum.COURSE_NOT_FOUND);
        return documentMapper.selectList(new LambdaQueryWrapper<Document>().eq(Document::getCourseId,courseId).orderByDesc(Document::getId))
                .stream().map(d->new com.docuagent.docuagent.vo.DocumentSummaryVO(d.getId(),d.getOriginalName(),d.getStatus(),d.getCharCount())).toList();
    }

    @Override
    public synchronized DocumentUploadVO uploadDocument(Long courseId, MultipartFile file) throws IOException {
        // 单机文件索引一次处理一份课件，避免并发保存覆盖；先确认课程存在。
        if (courseId == null || courseId < 1 || courseMapper.selectById(courseId) == null) {
            throw new BizException(CodeEnum.COURSE_NOT_FOUND);
        }

        // 同一课程下同名文件视为重复上传，直接拦住。
        // 否则会多出一条 document + 一整批 chunk + 一整批向量，
        // 检索时同一内容会占掉多个 Top-K 名额（还容易让人误以为是相似度算错了）
        Long dup = documentMapper.selectCount(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getCourseId, courseId)
                        .eq(Document::getOriginalName, file.getOriginalFilename()));
        if (dup > 0) {
            throw new BizException(CodeEnum.DOCUMENT_ALREADY_EXISTS);
        }

        Document document = new Document();
        document.setCourseId(courseId);
        document.setOriginalName(file.getOriginalFilename());
        document.setFileSize(file.getSize());
        document.setStatus("PARSING");
        DocumentUploadVO documentUploadVO = new DocumentUploadVO();
        String originalName = file.getOriginalFilename();
        String ext = "";
        String text = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();   // ← 转小写，PDF 也能过
        }

        try {
            if (!ALLOWED_EXT.contains(ext)) {
                throw new BizException(CodeEnum.UNSUPPORTED_FILE_TYPE);
            }
            File dir = new File(System.getProperty("user.dir"), "upload/" + courseId);
            dir.mkdirs();

            String fileName = UUID.randomUUID() + "." + ext;
            File saved = new File(dir, fileName);
            file.transferTo(saved);
            document.setStoragePath("upload/" + courseId + "/" + fileName);
            document.setFileType(ext);
            TikaDocumentReader reader = new TikaDocumentReader(new FileSystemResource(saved));
            text = reader.get().stream()
                    .map(d -> d.getText())
                    .collect(Collectors.joining("\n"));

            // 解析出来了但一个字都没有 → 当失败处理（图片、空白文件、纯扫描件都会这样）
            // ⚠️ 这里故意抛 RuntimeException 而不是 BizException：
            //    BizException 会被上面的 catch 原样抛出、不落库；
            //    但此时文件已经存盘了，必须留下一条 FAILED 记录
            if (text.isBlank()) {
                throw new RuntimeException("解析后没有内容（可能是图片或空白文件）");
            }


            document.setCharCount(text.length());
            document.setStatus("DONE");
            document.setFailureReason(null);

            log.info("课件解析成功：{}，字数={}", originalName, text.length());

        } catch (BizException e) {
            // 业务异常（比如文件类型不合法）：原样抛出，交给全局 handler 返回 1002
            throw e;
        } catch (Exception e) {
            log.error("课件解析失败：{}", originalName, e);
            document.setStatus("FAILED");
            document.setFailureReason(e.getMessage());
            text = "解析失败：" + e.getMessage();      // 失败时也给前端一句能看的话
        }

        documentMapper.insert(document);// ← 先落库，数据库生成的 id 才会回填
        if ("DONE".equals(document.getStatus())) {
            document.setStatus("INDEXING");
            documentMapper.updateById(document);
            List<String> newVectorIds = new ArrayList<>();
            try {

            List<String> chunks = simpleChunker.split(text);

            List<Chunk> chunkList = new ArrayList<>();

            for (int idx = 0; idx < chunks.size(); idx++) {
                Chunk c = new Chunk();
                c.setDocumentId(document.getId());
                c.setChunkIndex(idx);
                c.setContent(chunks.get(idx));
                c.setCharCount(chunks.get(idx).length());
                chunkMapper.insert(c);                          // ← 插完 c.getId() 才有值
                chunkList.add(c);                               // ← ② 收集（此时 id 已回填）
            }

            // ⚠️ 这里的 Document 必须是 Spring AI 的！
            //    本文件第 6 行 import 的 Document 是你自己的实体，两者同名 →
            //    只能写全限定名
            List<org.springframework.ai.document.Document> aiDocs = new ArrayList<>();

            for (Chunk c : chunkList) {
                Map<String, Object> meta = Map.of(
                        "courseId",   courseId,
                        "documentId", document.getId(),
                        "chunkId",    c.getId()
                );
                aiDocs.add(new org.springframework.ai.document.Document(
                        String.valueOf(c.getId()), c.getContent(), meta));
                newVectorIds.add(String.valueOf(c.getId()));
            }

            // ⚠️ 硅基流动的 embedding 接口单批只能十几条（实测 16 条 OK、32 条就 400 / 20015），
            //    不能一次把 40 多块全丢进去，这里自己按 10 条一批调，不依赖框架的分批策略
            int batchSize = 10;
            for (int i = 0; i < aiDocs.size(); i += batchSize) {
                int end = Math.min(i + batchSize, aiDocs.size());
                vectorStore.add(aiDocs.subList(i, end));
                log.info("向量入库进度：{}/{}", end, aiDocs.size());
            }

            vectorStore.save(new File(storePath));        // 所有批次加完，最后落盘一次

            log.info("向量入库完成：{} 块", aiDocs.size());
            document.setStatus("DONE");
            documentMapper.updateById(document);
            } catch (Exception exception) {
                // 失败课件不参与检索；删除本次已生成的部分向量，避免下次启动残留半批数据。
                log.error("课件向量入库失败，documentId={}", document.getId(), exception);
                try {
                    if (!newVectorIds.isEmpty()) vectorStore.delete(newVectorIds);
                    vectorStore.save(new File(storePath));
                } catch (Exception cleanup) {
                    log.error("向量清理失败，documentId={}", document.getId(), cleanup);
                }
                document.setStatus("FAILED");
                document.setFailureReason("向量化或索引保存失败，请查看日志后重新上传");
                documentMapper.updateById(document);
                throw new BizException(CodeEnum.INDEX_OPERATION_FAILED);
            }
        }
        documentUploadVO.setId(document.getId());
        documentUploadVO.setOriginalName(document.getOriginalName());
        documentUploadVO.setCharCount(document.getCharCount());
        documentUploadVO.setStatus(document.getStatus());
        documentUploadVO.setPreview(text.length() > 200 ? text.substring(0, 200) : text);

        return documentUploadVO;
    }
}
