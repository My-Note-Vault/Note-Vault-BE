package com.notevault.searchworker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "worker.sqs", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(WorkerSqsProperties.class)
public class WorkerSqsConfiguration {
    @Bean(destroyMethod = "close")
    SqsClient workerSqsClient(WorkerSqsProperties properties,
            @Value("${spring.cloud.aws.credentials.access-key}") String accessKey,
            @Value("${spring.cloud.aws.credentials.secret-key}") String secretKey) {
        if (!StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)) {
            throw new IllegalArgumentException("SQS consumption requires spring.cloud.aws.credentials.access-key and secret-key");
        }
        return SqsClient.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .httpClientBuilder(ApacheHttpClient.builder()
                        .maxConnections(properties.concurrency() * 2 + 2)
                        .connectionTimeout(Duration.ofSeconds(2))
                        .connectionAcquisitionTimeout(Duration.ofSeconds(2))
                        .socketTimeout(Duration.ofSeconds(25)))
                // ReceiveMessage can wait for 20 seconds before responding.
                .overrideConfiguration(config -> config.apiCallTimeout(Duration.ofSeconds(35))
                        .apiCallAttemptTimeout(Duration.ofSeconds(30)))
                .build();
    }

    @Bean(destroyMethod = "shutdownNow", defaultCandidate = false)
    ScheduledExecutorService workerVisibilityScheduler(WorkerSqsProperties properties) {
        return Executors.newScheduledThreadPool(properties.concurrency(),
                Thread.ofPlatform().name("sqs-visibility-", 0).factory());
    }

    @Bean
    SqsMessageConsumer sqsMessageConsumer(SqsClient client, WorkerSqsProperties properties,
            ObjectMapper mapper, WorkerMessageDispatcher dispatcher,
            @Qualifier("workerVisibilityScheduler") ScheduledExecutorService heartbeat,
            @Value("${openai.api-key:}") String embeddingKey,
            @Value("${openai.embedding.model:text-embedding-3-small}") String embeddingModel,
            @Value("${worker.title-backfill:false}") boolean titleBackfill,
            @Value("${worker.daily-note-backfill:false}") boolean noteBackfill) {
        if (!StringUtils.hasText(embeddingKey) || !StringUtils.hasText(embeddingModel)) {
            throw new IllegalArgumentException("SQS consumption requires openai.api-key and openai.embedding.model");
        }
        if (titleBackfill || noteBackfill) {
            throw new IllegalArgumentException("Run backfills with worker.sqs.enabled=false");
        }
        return new SqsMessageConsumer(client, properties, mapper, dispatcher, heartbeat);
    }
}
