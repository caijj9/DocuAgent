package com.docuagent.docuagent.controller;

import com.docuagent.docuagent.common.Result;
import com.docuagent.docuagent.dto.StudyRequests.*;
import com.docuagent.docuagent.service.LearningService;
import com.docuagent.docuagent.vo.StudyViews.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** 题目、答题、错题入口，不在Controller执行模型调用或数据库业务。 */
@RestController
public class LearningController {
    private final LearningService service;
    public LearningController(LearningService service){this.service=service;}
    @GetMapping("/question/{id}") public Result<QuestionView> question(@PathVariable Long id){return Result.success(service.getQuestion(id));}
    @PostMapping("/question/generate") public Result<List<QuestionView>> generate(@Valid @RequestBody Generate request){return Result.success(service.generate(request));}
    @PostMapping("/answer/submit") public Result<AnswerView> submit(@Valid @RequestBody Submit request){return Result.success(service.submit(request));}
    @GetMapping("/wrong-question") public Result<List<WrongView>> wrong(@RequestParam(required=false) Long courseId,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size){return Result.success(service.wrongQuestions(courseId,page,size));}
}
