package com.docuagent.docuagent.controller;

import com.docuagent.docuagent.common.Result;
import com.docuagent.docuagent.service.DocumentService;
import com.docuagent.docuagent.vo.DocumentUploadVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/document")
public class DocumentController {
    @Autowired
    private DocumentService documentService;
    @GetMapping public Result<java.util.List<com.docuagent.docuagent.vo.DocumentSummaryVO>> list(@RequestParam Long courseId){return Result.success(documentService.list(courseId));}
    @PostMapping("/upload")
    public Result<DocumentUploadVO> upload(@RequestParam Long courseId,
                                           @RequestParam("file") MultipartFile file) throws IOException{
        return Result.success(documentService.uploadDocument(courseId, file));
    }

}
