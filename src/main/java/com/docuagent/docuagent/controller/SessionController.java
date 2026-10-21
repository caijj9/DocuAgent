package com.docuagent.docuagent.controller;

import com.docuagent.docuagent.common.Result;
import com.docuagent.docuagent.dto.StudyRequests.SessionCreate;
import com.docuagent.docuagent.service.SessionService;
import com.docuagent.docuagent.vo.StudyViews.SessionView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/session")
public class SessionController {
    private final SessionService service;
    public SessionController(SessionService service){this.service=service;}
    @PostMapping public Result<SessionView> create(@Valid @RequestBody(required=false) SessionCreate request){return Result.success(service.create(request==null?null:request.courseId()));}
    @GetMapping public Result<List<SessionView>> list(){return Result.success(service.list());}
    @DeleteMapping("/{id}") public Result<Void> delete(@PathVariable String id){service.delete(id);return Result.success(null);}
}
