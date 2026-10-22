package com.docuagent.docuagent.metrics;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
@Component
public class UsageFilter extends OncePerRequestFilter {
    private final AiUsage usage;
    public UsageFilter(AiUsage usage){this.usage=usage;}
    protected boolean shouldNotFilter(HttpServletRequest request){return request.getRequestURI().startsWith("/diagnostics/")||request.getRequestURI().contains(".");}
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        String id=usage.begin();usage.bind(id);response.setHeader("X-Usage-Id",id);request.setAttribute("usageId",id);
        try{chain.doFilter(request,response);}finally{usage.finish(id);usage.unbind();}
    }
}
