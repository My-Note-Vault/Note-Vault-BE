package com.example.search.sync;

import com.example.search.sync.outbox.SearchSyncOutbox;
import com.example.search.sync.outbox.SearchSyncOutboxRepository;
import com.example.search.sync.outbox.SearchSyncOutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/** H2에서 실제 JPA 저장과 트랜잭션 경계를 검증한다. PostgreSQL 전용 relay SQL은 실행하지 않는다. */
@SpringBootTest(classes = SearchSyncRecorderIntegrationTest.Config.class, webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:outbox-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false"
        })
@DisplayName("검색 이벤트 기록과 데이터베이스 트랜잭션 통합 테스트")
class SearchSyncRecorderIntegrationTest {
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = SearchSyncOutbox.class)
    @EnableJpaRepositories(basePackageClasses = SearchSyncOutboxRepository.class)
    @Import(SearchSyncRecorder.class)
    static class Config {
        @Bean ObjectMapper objectMapper() { return JsonMapper.builder().findAndAddModules().build(); }
    }

    @Autowired SearchSyncRecorder recorder;
    @Autowired SearchSyncOutboxRepository outbox;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired ObjectMapper mapper;
    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() {
        transaction = new TransactionTemplate(transactionManager);
        outbox.deleteAll();
    }

    @Test
    @DisplayName("커밋된 문서 변경 이벤트는 버전 2 JSON과 함께 데이터베이스에 저장된다")
    void commitPersistsRefreshContract() throws Exception {
        UUID id = transaction.execute(status -> recorder.refreshDocument(42L, 7L));

        var row = outbox.findById(id).orElseThrow();
        var event = mapper.readValue(row.getPayload(), SearchSyncEvent.class);
        assertThat(event.eventId()).isEqualTo(id);
        assertThat(event.schemaVersion()).isEqualTo(2);
        assertThat(event.sourceType()).isEqualTo(SearchSourceType.DOCUMENT);
        assertThat(event.sourceId()).isEqualTo(42L);
        assertThat(event.contentRevision()).isEqualTo(7L);
        assertThat(event.messageType()).isEqualTo(WorkerMessageType.DOCUMENT_REFRESH);
        assertThat(row.getStatus()).isEqualTo(SearchSyncOutboxStatus.PENDING);
        assertThat(row.isNew()).isFalse();
    }

    @Test
    @DisplayName("호출자 트랜잭션이 롤백되면 이미 flush한 검색 이벤트도 남지 않는다")
    void rollbackRemovesFlushedEvent() {
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            recorder.refreshDailyNote(9L, 2L);
            outbox.flush();
            assertThat(outbox.count()).isEqualTo(1);
            throw new IllegalStateException("business operation failed");
        })).isInstanceOf(IllegalStateException.class).hasMessage("business operation failed");

        assertThat(outbox.count()).isZero();
    }

    @Test
    @DisplayName("트랜잭션 없이 검색 이벤트를 기록하는 호출은 거부한다")
    void requiresCallerTransaction() {
        assertThatThrownBy(() -> recorder.refreshDocument(42L, 1L))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(outbox.count()).isZero();
    }

    @Test
    @DisplayName("일일 노트 삭제 이벤트에는 revision 없이 삭제 명령을 기록한다")
    void persistsDeleteContract() throws Exception {
        UUID id = transaction.execute(status -> recorder.deleteDailyNote(9L));
        var event = mapper.readTree(outbox.findById(id).orElseThrow().getPayload());

        assertThat(event.path("messageType").asText()).isEqualTo("SEARCH_DELETE");
        assertThat(event.path("sourceType").asText()).isEqualTo("DAILY_NOTE");
        assertThat(event.path("contentRevision").isNull()).isTrue();
    }

    @Test
    @DisplayName("선점과 발행 완료는 별도 트랜잭션의 변경 감지로 영속화된다")
    void leaseStateSurvivesTransactionBoundaries() {
        UUID id = transaction.execute(status -> recorder.refreshDocument(42L, 1L));
        Instant now = Instant.now();
        var message = transaction.execute(status -> outbox.findForUpdate(id).orElseThrow().claim(now));

        assertThat(outbox.findById(id).orElseThrow().getStatus()).isEqualTo(SearchSyncOutboxStatus.PROCESSING);
        transaction.executeWithoutResult(status -> {
            var row = outbox.findForUpdate(id).orElseThrow();
            assertThat(row.markPublished(message.leaseToken(), now.plusSeconds(1))).isTrue();
        });

        var published = outbox.findById(id).orElseThrow();
        assertThat(published.getStatus()).isEqualTo(SearchSyncOutboxStatus.PUBLISHED);
        assertThat(published.getLeaseToken()).isNull();
        assertThat(published.getAttemptCount()).isEqualTo(1);
    }
}
