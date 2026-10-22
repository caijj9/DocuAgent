package com.docuagent.docuagent.controller;
import com.docuagent.docuagent.common.Result;
import com.docuagent.docuagent.metrics.AiUsage;
import org.springframework.web.bind.annotation.*;
@RestController
public class UsageController {
    private final AiUsage usage;
    public UsageController(AiUsage usage){this.usage=usage;}
    @GetMapping("/diagnostics/usage/{id}")
    public Result<AiUsage.Snapshot> get(@PathVariable String id){var snapshot=usage.get(id);return snapshot==null?new Result<>(404,"该请求用量已过期",null):Result.success(snapshot);}
}
