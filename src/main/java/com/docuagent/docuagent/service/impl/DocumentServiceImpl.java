package com.docuagent.docuagent.service.impl;

import com.docuagent.docuagent.common.CodeEnum;
import com.docuagent.docuagent.entity.Document;
import com.docuagent.docuagent.exception.BizException;
import com.docuagent.docuagent.mapper.DocumentMapper;
import com.docuagent.docuagent.service.DocumentService;
import com.docuagent.docuagent.vo.DocumentUploadVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DocumentServiceImpl implements DocumentService {
    private static final Set<String> ALLOWED_EXT =
            Set.of("pdf", "ppt", "pptx", "doc", "docx");
    @Autowired
    private DocumentMapper documentMapper;
    @Override
    public DocumentUploadVO uploadDocument(Long courseId, MultipartFile file) throws IOException {
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

        documentMapper.insert(document);              // ← 先落库，数据库生成的 id 才会回填
        documentUploadVO.setId(document.getId());
        documentUploadVO.setOriginalName(document.getOriginalName());
        documentUploadVO.setCharCount(document.getCharCount());
        documentUploadVO.setStatus(document.getStatus());
        documentUploadVO.setPreview(text.length() > 200 ? text.substring(0, 200) : text);

        return documentUploadVO;
    }
}
