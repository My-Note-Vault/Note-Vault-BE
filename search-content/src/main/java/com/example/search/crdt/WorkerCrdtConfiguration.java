package com.example.search.crdt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ProjectionProperties.class)
public class WorkerCrdtConfiguration {
    @Bean(destroyMethod = "close")
    GraalJsYjsDocumentConverter yjsRuntime(ProjectionProperties properties, ObjectMapper mapper,
            org.springframework.jdbc.core.JdbcTemplate jdbc) throws IOException {
        jdbc.execute("SELECT snapshot_revision FROM document WHERE false");
        return new GraalJsYjsDocumentConverter(properties, mapper);
    }
}
