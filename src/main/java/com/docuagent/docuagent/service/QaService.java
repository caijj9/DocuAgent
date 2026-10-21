package com.docuagent.docuagent.service;

import com.docuagent.docuagent.dto.StudyRequests.Ask;
import com.docuagent.docuagent.vo.StudyViews.*;

public interface QaService {
    Qa ask(Ask request);
    AgentView agent(Ask request);
}
