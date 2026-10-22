package com.docuagent.docuagent.ai.verify;
import com.docuagent.docuagent.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.docuagent.docuagent.ai.chunk.SimpleChunker;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
/** 离线导出真实Tika文本和当前参数切块；只读原库，实验不得覆盖原索引。 */
@Component @Profile("export-corpus")
public class ExportCorpusRunner implements ApplicationRunner {
    private final DocumentMapper documents;private final SimpleChunker chunker;private final ObjectMapper mapper;private final ConfigurableApplicationContext context;
    @Value("${docuagent.export.path:data/evidence/corpus-export.json}")private String output;
    public ExportCorpusRunner(DocumentMapper documents,SimpleChunker chunker,ObjectMapper mapper,ConfigurableApplicationContext context){this.documents=documents;this.chunker=chunker;this.mapper=mapper;this.context=context;}
    public void run(ApplicationArguments args)throws Exception{
        List<Map<String,Object>> rows=new ArrayList<>();
        for(var doc:documents.selectList(null))if("DONE".equals(doc.getStatus())){
            Path source=Path.of(System.getProperty("user.dir"),doc.getStoragePath()).normalize();
            String text=new TikaDocumentReader(new FileSystemResource(source)).get().stream().map(d->d.getText()).collect(Collectors.joining("\n"));
            rows.add(Map.of("id",doc.getId(),"courseId",doc.getCourseId(),"name",doc.getOriginalName(),"text",text,"chunks",chunker.split(text)));
        }
        Path target=Path.of(output).toAbsolutePath();Files.createDirectories(target.getParent());mapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(),rows);
        System.out.println("EXPORTED documents="+rows.size());SpringApplication.exit(context);
    }
}
