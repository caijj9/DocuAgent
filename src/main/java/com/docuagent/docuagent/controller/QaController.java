package com.docuagent.docuagent.controller;

import com.docuagent.docuagent.common.Result;
import com.docuagent.docuagent.dto.StudyRequests.Ask;
import com.docuagent.docuagent.service.QaService;
import com.docuagent.docuagent.vo.StudyViews.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class QaController {
    private final QaService service;
    public QaController(QaService service){this.service=service;}
    @PostMapping("/qa/ask") public Result<Qa> ask(@Valid @RequestBody Ask request){return Result.success(service.ask(request));}
    @PostMapping("/agent/chat") public Result<AgentView> agent(@Valid @RequestBody Ask request){return Result.success(service.agent(request));}
}
