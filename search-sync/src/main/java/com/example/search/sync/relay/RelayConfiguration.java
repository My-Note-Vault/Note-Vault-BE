package com.example.search.sync.relay;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "search.sync.relay", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RelayProperties.class)
@EnableScheduling
@Import({OutboxRelay.class, OutboxTransactions.class, RelayMetrics.class})
public class RelayConfiguration {
    // The named scheduler is only selected explicitly by Relay tasks. It does not
    // replace Boot's default scheduler/executor used by CRDT, draw, or web requests.
    @Bean(name = "searchSyncRelayScheduler", defaultCandidate = false)
    ThreadPoolTaskScheduler relayScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("search-sync-relay-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(20);
        return scheduler;
    }

    @Bean(name = "searchSyncSqsClient", destroyMethod = "close")
    SqsClient sqsClient(RelayProperties properties,
                        @Value("${spring.cloud.aws.credentials.access-key}") String accessKey,
                        @Value("${spring.cloud.aws.credentials.secret-key}") String secretKey) {
        return SqsClient.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .httpClientBuilder(ApacheHttpClient.builder()
                        .maxConnections(1)
                        .connectionTimeout(Duration.ofSeconds(2))
                        .connectionAcquisitionTimeout(Duration.ofSeconds(2))
                        .socketTimeout(Duration.ofSeconds(3)))
                .overrideConfiguration(config -> config
                        .apiCallTimeout(Duration.ofSeconds(10))
                        .apiCallAttemptTimeout(Duration.ofSeconds(3)))
                .build();
    }

    @Bean
    SqsEventPublisher sqsEventPublisher(@Qualifier("searchSyncSqsClient") SqsClient client,
                                       RelayProperties properties) {
        return new SqsEventPublisher(client, properties);
    }
}
