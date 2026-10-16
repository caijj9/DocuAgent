package com.docuagent.docuagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.docuagent.docuagent.mapper")
public class DocuAgentApplication {

    public static void main(String[] args) {

        SpringApplication.run(DocuAgentApplication.class, args);
    }

}
