package com.docuagent.docuagent.service;


import com.docuagent.docuagent.vo.DocumentUploadVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface DocumentService {
    public DocumentUploadVO uploadDocument(Long courseId, MultipartFile file) throws IOException;
}
