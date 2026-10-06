# Search Content Worker

공유 DB를 사용하는 독립 실행 모듈입니다. API 애플리케이션과 별도 JAR/이미지로 배포합니다.
100개/1분 배치와 GraalJS 설정, 최초 전환은 [문서 갱신 Worker 안내](../docs/independent-crdt-worker.md)를 참고합니다.

## 모듈과 실행 경계

- Worker 기능 모듈은 `search-content`와 `embedding` 두 개입니다.
- `search-content`가 Worker 실행 앱이며, 청킹·색인 실행·청크와 벡터 저장을 담당합니다.
- `embedding`은 Worker 전용 문서 임베딩 호출을 담당합니다.
- 기존 `search-storage`, `search-indexing-core`, `search-worker` 모듈은 제거했습니다.
- API는 두 Worker 모듈을 의존하지 않습니다. 따라서 청킹·문서 임베딩·색인 저장 코드가 API JAR에 포함되지 않습니다.
- API에는 읽기 전용 `IndexedChunkReader`와 질문 한 건을 임베딩하는 `embedQuestion`만 남습니다.
- API는 권한과 delta 저장을 담당합니다. Worker는 DOCUMENT_REFRESH 하나로 snapshot·본문 저장부터 청킹·임베딩 완료까지 수행합니다.
- Worker는 `common`, `workspace-service`, `platform-service`, `knowledge-assistant-service`에 의존하지 않습니다.
- 기존 API의 문서·데일리 노트 `/indexing` 엔드포인트는 제거했습니다.
- SQS 수신을 활성화하면 API → Outbox → Relay → SQS → Worker → 검색 데이터 저장이 연결됩니다.
- 프론트에서 직접 `/indexing`을 호출할 필요가 없습니다. 기존 색인 조회는 유지됩니다.

## 실행 구성

`SearchWorkerApplication`은 `application-worker.yaml`을 기본 설정으로 사용합니다.
API의 `application.yaml`과 common/platform/workspace 설정은 불러오지 않습니다.
Worker 패키지와 CRDT·청킹·임베딩 패키지를 컴포넌트 스캔합니다.
검색 데이터 엔티티와 Worker 전용 원문 읽기 모델·저장소를 JPA에 등록합니다.
웹 서버는 실행하지 않으며, `spring.main.keep-alive`로 프로세스를 유지합니다.

| 환경변수 | 용도 |
|---|---|
| `SPRING_DATASOURCE_URL` | 기존 API와 같은 PostgreSQL DB |
| `SPRING_DATASOURCE_USERNAME` | Worker DB 계정 |
| `SPRING_DATASOURCE_PASSWORD` | Worker DB 암호 |
| `OPENAI_API_KEY` | 임베딩 호출 시 필요 |
| `OPENAI_EMBEDDING_MODEL` | API와 동일하게 설정, 기본값 `text-embedding-3-small` |
| `WORKER_DB_POOL_SIZE` | 최대 DB 연결 수, 기본값 3 |
| `WORKER_DB_MIN_IDLE` | 최소 유휴 DB 연결 수, 기본값 1 |

Worker는 `ddl-auto: validate`로 기존 스키마만 확인합니다.
`document`, `daily_note`, `daily_note_plan`, `plan`, `content_chunk`, `content_title_embedding`이 준비된 DB에 연결합니다.
Worker는 document의 snapshot과 검색 본문을 같은 revision으로 함께 저장합니다. 스키마 생성·변경은 수행하지 않습니다.
snapshot_revision과 문서 배치 컬럼 스키마, [최초 전환 절차](../docs/independent-crdt-worker.md)가 필요합니다.
청크 유일 제약과 제목 테이블은 `docs/migrations/2026-09-17-idempotent-content-chunks.sql`,
`docs/migrations/2026-09-19-title-embeddings.sql`의 적용 상태를 먼저 확인합니다.
임베딩은 Hibernate `float[]` ↔ PostgreSQL `vector(1536)`으로 저장합니다.
pgvector 0.8 이상과 `docs/migrations/2026-09-28-pgvector-search.sql` 적용이 필요합니다.
기존 TEXT 저장 버전과 호환되지 않으므로 [전환 절차](../docs/pgvector-search.md)에 따라 API/Worker를 함께 반영합니다.
별도 설정 파일이 필요하면 `SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:/app/config/`으로 지정하고
해당 디렉터리에 `application-worker.yaml`을 배치합니다.

## 빌드·이미지 생성

아래는 CI/배포 환경에서 실행할 명령입니다. GraalJS 번들 변경 시 안내 문서의 npm 빌드를 먼저 수행합니다.

```sh
./gradlew :search-content:bootJar
docker build -f search-content/Dockerfile -t note-vault-search-worker .
```

Docker 빌드 컨텍스트는 저장소 루트입니다. Worker 이미지는 서비스 포트를 노출하지 않습니다.

```sh
docker run --stop-timeout 100 --env-file /path/to/worker.env note-vault-search-worker
```

기존 API CI는 `:bootJar`를 명시해 API 산출물만 빌드합니다.
Deploy API는 같은 커밋의 Worker 배포가 성공한 뒤 API를 배포합니다. Worker 단독 수동 배포도 유지합니다.

## SQS 수신 설정

`search-content/src/main/resources/application-worker.yaml`의 기본값은 수신 비활성화입니다.
Worker 서버의 외부 `application-worker.yaml`에 다음 값을 설정합니다.
IntelliJ에서도 Worker 실행 설정에 `--spring.config.additional-location=optional:file:/설정디렉터리/`를
프로그램 인수로 넣어 같은 파일을 읽을 수 있습니다. 별도 인증 환경변수는 필요 없습니다.

```yaml
spring:
  cloud:
    aws:
      credentials:
        access-key: 실제_액세스_키
        secret-key: 실제_시크릿_키

worker:
  sqs:
    enabled: true
    queue-url: https://sqs.us-east-2.amazonaws.com/계정ID/AsyncWorkerQueue
    region: us-east-2
    concurrency: 2

openai:
  api-key: 실제_OpenAI_키
  embedding:
    model: text-embedding-3-small
```

인증 속성 이름은 API의 S3/Relay와 같지만 API의 `application-common.yaml`은 자동으로 불러오지 않습니다.
Worker용 외부 YAML에 값을 제공해야 합니다. 환경변수 방식도 기본 YAML의 치환식을 통해 사용할 수 있습니다.
큐 URL은 API Relay와 동일한 **Standard 작업 큐**이며 DLQ URL이 아닙니다.
API의 질문 임베딩과 Worker의 문서 임베딩 모델은 같아야 합니다.
수신을 켜면 AWS 인증 키, 큐 URL, 리전, OpenAI 키·모델을 검증합니다.
동시 처리 수는 1~16이며, 값을 늘릴 때 DB 풀과 OpenAI 호출 한도도 함께 조정합니다.
명시적인 backfill 실행 옵션과 SQS 수신을 동시에 활성화하면 시작에 실패합니다.

## 처리와 저장 경계

- `SqsMessageConsumer`: 20초 long polling, 처리 스레드별 한 번에 1개 수신. 메시지가 없으면 원문 DB를 조회하지 않습니다.
- `SearchSyncMessage`: v2 messageType 계약 검증과 큐에 남은 v1 REFRESH/DELETE 수용. 숫자로 표현한 enum, 잘못된 revision, 미지원 버전은 실패 처리합니다.
- `WorkerMessageDispatcher`: DOCUMENT_REFRESH / SEARCH_REFRESH는 갱신, SEARCH_DELETE는 삭제 Handler로 전달합니다.
- `SearchRefreshHandler`: DOCUMENT는 GraalJS 한 번으로 snapshot과 본문을 만든 뒤 청킹·임베딩까지 처리합니다. DAILY_NOTE는 기존 문자열/Plan을 사용합니다.
- 남아 있는 CRDT_COMPACT와 DOCUMENT 대상 SEARCH_REFRESH도 전체 문서 갱신 경로로 처리합니다.
- `SearchDeleteHandler`: 원문이 삭제됐는지 확인하고 검색 데이터만 정리합니다.
- `ContentIndexingService`: 제목·본문 임베딩 및 기존 READY 벡터 재사용.
- `ContentIndexingTransactions`: JPA Repository로 준비·완료·실패를 별도 짧은 트랜잭션에서 처리하고, 성공 직전 제목과 모든 청크를 확인합니다.

원문과 연결된 Plan 조회·잠금은 Worker 전용 읽기 모델과 Repository가 담당합니다.
CRDT는 JdbcTemplate으로 잠금·용량 검사·연속 이력 조회와 revision 조건부 UPDATE를 수행합니다.
트랜잭션 제한은 5초이며 OpenAI 호출 중에는 DB 연결과 원문 잠금을 유지하지 않습니다.
DailyNote 검증은 노트 → Plan(ID 순서) → 연결 행 순서로 잠급니다.
API의 노트 삭제도 노트를 먼저 잠급니다. 새 연결을 차단하려면 daily_note_plan의 원문 FK가 실제 DB에 있어야 합니다.

본문 revision이 오래된 이벤트도 현재 원문을 처리합니다. 제목 변경과 Plan 변경은 동일 revision에서도 반영합니다.
DailyNote의 본문과 Plan을 합치는 순서·구분자·해시는 API 검색 조회와 동일하게 유지합니다.
문서는 기존 revision, DailyNote는 합쳐진 본문의 SHA-256을 source_version으로 사용합니다.
원문이 없어졌으면 REFRESH/DELETE 모두 청크와 제목 임베딩을 한 트랜잭션에서 삭제합니다.
DELETE를 받았는데 원문이 존재하면 실패하여 재시도합니다. DELETE는 검색 생성이나 compact를 실행하지 않습니다.

중복 수신 시 준비된 READY 벡터를 재사용합니다. 새 revision에서도 해시·본문이 같은 기존 청크 행과 벡터를 보존하고,
revision·순서 등 메타데이터만 갱신합니다. 제거되거나 변경된 청크만 교체하며 모델이 달라지면 다시 임베딩합니다. 동시 처리의 이전 결과는 원문 검증과 임베딩 시도 번호로 차단합니다.
OpenAI 호출 자체를 정확히 한 번만 실행한다고 보장하지는 않습니다.
임베딩은 최대 64개 입력씩 요청하고 연결 제한 5초, 응답 제한 60초를 적용합니다.
응답 개수·순서·벡터 값을 검증한 뒤 저장합니다.

## 재시도, DLQ, 종료

수신 시 visibility timeout은 120초입니다. 별도 스레드가 처리 중 30초마다 120초로 연장합니다.
연장 실패 또는 처리 시간 10분 초과 시 현재 처리를 중단하도록 interrupt하고, 메시지를 삭제하지 않습니다.
DB 상태 검증에 성공한 뒤 현재 receiptHandle로 DeleteMessage를 호출합니다.
DB 커밋 후 DeleteMessage가 실패해도 메시지가 남습니다. 다음 수신에서 snapshot·본문이 최신이면 복원을 건너뛰지만,
청킹과 임베딩 완료 확인까지 수행합니다. 본문 저장 후 임베딩 실패 시에도 같은 메시지를 재시도합니다.

실패 시 메시지를 삭제하거나 새로 발행하지 않습니다. 수신 횟수에 따라 30초부터 최대 5분까지
무작위 지연을 적용해 다시 보이게 합니다. 잘못된 JSON과 미지원 스키마도 같은 방식으로 DLQ에 도달합니다.
수신 API 자체가 실패하면 5초 후 다시 요청합니다. Worker는 Outbox 상태를 수정하지 않습니다.

기존 큐·DLQ와 IAM 설정을 그대로 사용합니다. 아래 항목은 기존 설정 확인용입니다.

1. 작업 큐와 같은 리전 `us-east-2`에 기존 Standard DLQ `AsyncWorkerDLQ`가 있는지 확인합니다.
2. `AsyncWorkerQueue`의 배달 못한 편지 대기열 설정에 연결하고 `maxReceiveCount=5`로 시작합니다.
3. DLQ의 보관 기간을 작업 큐보다 길게 설정합니다. 예: 작업 큐 4일, DLQ 14일.
4. Worker의 IAM 사용자에 작업 큐 ARN 대상으로 아래 정책을 추가합니다. 계정ID는 실제 값으로 바꿉니다.

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": ["sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:ChangeMessageVisibility"],
    "Resource": "arn:aws:sqs:us-east-2:계정ID:AsyncWorkerQueue"
  }]
}
```

고객 관리 KMS 키로 암호화한 큐는 Worker에 해당 키의 복호화 권한도 필요합니다.
DLQ 이동은 SQS redrive 정책이 담당하며, Worker가 DLQ에 직접 발행하지 않습니다.
이 코드 변경은 AWS 큐·정책을 자동 생성하거나 수정하지 않습니다.

종료 시 신규 수신을 중단하고 최대 90초 동안 진행 중인 작업을 기다립니다.
Spring 종료 유예는 95초이며 컨테이너 종료 유예도 100초 이상으로 설정합니다.
완료하지 못한 메시지는 삭제하지 않아 visibility timeout 이후 다시 처리됩니다.

## 확인 방법

성공 로그 `Worker completed`에는 eventId/messageType/sourceType/sourceId/result/receiveCount/durationMs가 남습니다.
실패 로그 `Worker failed`에는 같은 식별자와 실패 원인이 남고, 본문·벡터·인증 키는 기록하지 않습니다.
잘못된 계약은 eventId 대신 SQS messageId로 추적합니다.
Outbox의 PUBLISHED는 발행 완료입니다. snapshot·본문 저장은 document.snapshot_revision/search_revision으로 확인합니다.
전체 작업 완료는 content_chunk/content_title_embedding의 READY와 Worker completed 로그로 확인합니다.
빈 본문이나 삭제된 원문은 행이 없는 것이 정상입니다.

운영 연결 후 문서 제목·본문 변경, DailyNote Plan 변경, 삭제, 중복/역순 수신,
처리 중 원문 변경·삭제, 실패 후 재수신과 DLQ 이동을 확인합니다.
기존 데이터 전체 재색인은 별도 명시적 backfill이며 Worker 시작 시 자동 실행하지 않습니다.
