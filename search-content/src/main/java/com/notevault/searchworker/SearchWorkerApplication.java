package com.notevault.searchworker;

import com.example.search.content.ContentChunk;
import com.example.search.content.ContentChunkRepository;
import com.example.search.embedding.OpenAiEmbeddingClient;
import com.example.search.indexing.ContentChunker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Map;

@SpringBootApplication(scanBasePackageClasses = {
        SearchWorkerApplication.class, ContentChunker.class, OpenAiEmbeddingClient.class,
        com.example.search.crdt.WorkerCrdtConfiguration.class
})
@EntityScan(basePackageClasses = ContentChunk.class)
@EnableJpaRepositories(basePackageClasses = ContentChunkRepository.class)
@EnableJpaAuditing
public class SearchWorkerApplication {

    public static void main(String[] args) throws java.io.IOException {
        java.nio.file.Files.deleteIfExists(WorkerReadiness.FILE);
        SpringApplication application = new SpringApplication(SearchWorkerApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setDefaultProperties(Map.of("spring.config.name", "application-worker"));
        application.run(args);
    }
}
