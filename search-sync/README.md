# Worker 작업 요청과 Outbox

API의 workspace-service·platform-service가 사용하는 작업 등록 모듈이다.
Worker는 이 모듈에 의존하지 않으며 기존 SQS 큐에서 작업을 받는다.

## 작업 등록

SearchSyncRecorder는 MANDATORY 트랜잭션으로 호출자의 원문 변경과 Outbox 기록을 함께 커밋한다.
외부 SQS 호출은 하지 않는다. 직렬화·저장 실패 시 호출자의 변경도 롤백한다.

| 메서드/변경 | 메시지 | 발행 시점 |
| --- | --- | --- |
| batchDocumentRefresh / 새 CRDT delta | DOCUMENT_REFRESH | 100개 누적 또는 첫 update부터 1분 |
| refreshDocument / 문서 생성·제목 변경 | DOCUMENT_REFRESH | 즉시 |
| refreshDailyNote / 노트 본문·Plan 변경 | SEARCH_REFRESH | 즉시 |
| deleteDocument / 문서·Workspace 삭제 | SEARCH_DELETE | 즉시 |
| deleteDailyNote / 노트 삭제 | SEARCH_DELETE | 즉시 |
| 협업 이력 조회 | 없음 | 작업 등록 없음 |

Workspace Home도 실제 document PK를 전달한다.
문서 요청 revision은 latestRevision, DailyNote는 contentRevision을 사용한다.
요청은 해당 revision 이상인 최신 상태를 처리하라는 뜻이다.
Worker는 DOCUMENT_REFRESH 하나로 snapshot·본문 저장과 청킹·임베딩 완료까지 처리한다.
compact 완료 시 별도 검색 메시지를 발행하지 않는다.

## 100개 / 1분 배치

DocumentCommandService가 문서 쓰기 잠금을 잡고 새 delta를 저장한 후 DocumentRefreshRequests를 호출한다.
중복 clientUpdateId가 활성 delta에 있으면 기존 결과만 반환하고 배치에는 추가하지 않는다.
첫 update에서 UUID를 발급하고 next_attempt_at을 DB 현재 시각 + 1분으로 설정한다.
문서에는 refresh_batch_event_id와 refresh_batch_base_revision을 저장한다.

다음 update는 같은 Outbox 행을 잠근다. 아직 기한 전이고 PENDING·attempt_count=0인 경우에만
payload의 contentRevision을 최신 값으로 교체한다. 이벤트 ID와 created_at은 유지한다.
latestRevision - refresh_batch_base_revision이 100 이상이면 next_attempt_at을 현재 DB 시각으로 앞당긴다.
그 외에는 기존 마감 시간을 유지한다. 계속 입력해도 1분이 연장되지 않는다.

이미 기한이 도달했거나 Relay가 확보·재시도 중인 요청에는 추가하지 않는다.
다음 배치를 만들어 새 ID와 마감 시간을 기록한다. Relay는 같은 행 잠금을 사용하고,
확보한 이후에는 payload를 바꾸지 않아 전송·재시도가 같은 내용을 사용한다.
100개 미만에서 입력을 멈추거나 API가 재시작돼도 DB에 남은 마감 시각을 Relay가 확인한다.
발행 완료 행이 정리되어 배치 포인터의 대상이 없어졌으면 다음 편집에서 새 배치를 만든다.

1분은 Outbox 배치 대기시간이다. Relay 확인·SQS 대기·실제 처리 시간은 추가된다.
설정은 workspace.crdt.refresh.min-updates=100, workspace.crdt.refresh.max-wait=1m이며
운영 외부 YAML의 search.sync.relay.poll-delay도 10s인지 확인한다.
[전체 처리와 배포 절차](../docs/independent-crdt-worker.md)를 참고한다.

## 계약과 스키마

payload는 eventId, schemaVersion=2, occurredAt, sourceType, sourceId, messageType, contentRevision이다.
본문이나 인증 정보는 포함하지 않는다. SEARCH_DELETE의 revision은 null이다.
새 API는 CRDT_COMPACT를 만들지 않는다. 새 Worker는 남아 있는 v1 REFRESH/DELETE와
v2 CRDT_COMPACT, DOCUMENT 대상 SEARCH_REFRESH도 전체 문서 처리 경로로 받아들인다.

기존 search_sync_outbox 테이블과 인덱스를 사용한다. 새 메시지 종류를 위한 컬럼 추가는 없다.
문서에 배치 포인터 두 컬럼을 추가하는
[배치 마이그레이션](../docs/migrations/2026-10-06-document-refresh-batching.sql)이 필요하다.
최초 Outbox 설치에는 기존 2026-09-21-add-search-sync-outbox.sql도 적용되어 있어야 한다.
PUBLISHED는 SQS 발행 완료이며 Worker 완료 표시는 아니다.

DailyNote는 본문과 Plan을 합친 해시를 검색 버전으로 사용한다. Plan 변경은 contentRevision을 올리지 않는다.
문서 생성과 제목 설정은 기존처럼 별도 트랜잭션이며 즉시 요청이 각각 등록될 수 있다.
제목 변경 요청이 먼저 실행되면 대기 중인 본문 변경도 더 일찍 반영할 수 있다.
권한 실패·중복 delta·수정 충돌에는 새 요청을 남기지 않는다.
문서/Workspace 삭제는 검색 삭제 요청과 같은 트랜잭션으로 처리하며 기존 FK cascade를 유지한다.
archive 저장·만료 삭제는 제거했고 과거 archive 데이터는 별도로 보존한다.

## Polling Relay (4단계)

`OutboxRelay`는 작업 확보 → SQS 전송 → 성공 기록 / 실패 기록 순서로 실행합니다.
`OutboxTransactions`의 public 메서드마다 별도 `@Transactional` 트랜잭션을 사용합니다.
작업을 확보한 트랜잭션이 커밋된 뒤 SQS에 전송하며, HTTP 호출 중에는 DB 트랜잭션을 유지하지 않습니다.
상태 전이·임대 검증·백오프는 `SearchSyncOutbox`가 담당합니다.
조회는 JPA Repository에 모았으며 PostgreSQL의 SKIP LOCKED, DB 시각, 제한된 삭제 배치에 native query를 사용합니다.

### API 설정

API의 설정 파일에 아래 항목을 추가하여 사용합니다. 기본값은 비활성화이며,
비활성화 상태에서는 SQS 클라이언트·Relay 스케줄러를 생성하지 않고 큐 URL·리전도 요구하지 않습니다.

```yaml
search:
  sync:
    relay:
      enabled: ${SEARCH_SYNC_RELAY_ENABLED:false}
      queue-url: ${SEARCH_SYNC_RELAY_QUEUE_URL:}
      region: ${AWS_REGION:us-east-2}
      poll-delay: 10s
```

위 YAML을 추가하는 경우 환경변수로 큐 URL을 전달할 수 있습니다.
Spring 속성을 직접 주입하는 경우 `search.sync.relay.queue-url` 등을 사용합니다.
활성화 시 queue-url과 region은 필수이며, queue-url에는 Standard SQS 큐 URL을 지정합니다.
인증은 기존 S3와 동일하게 `application-common.yaml`의
`spring.cloud.aws.credentials.access-key`와 `spring.cloud.aws.credentials.secret-key`를 읽습니다.
별도의 SQS 인증 키나 IntelliJ AWS 인증 환경변수를 추가할 필요가 없습니다.
해당 자격 증명에는 큐에 대한 `sqs:SendMessage` 권한이 필요합니다. 고객 관리 KMS 키를 쓰는 큐는
해당 키의 발행 권한도 설정해야 합니다. Relay는 큐나 DLQ를 생성하지 않습니다.

### 발행·재시도 규칙

- 기본 10초의 fixed delay로 폴링합니다. 이전 폴링이 끝난 뒤 다음 주기가 시작됩니다.
- 한 번에 최대 10건만 잠금으로 확보하고 커밋합니다. 한 번의 폴링은 최대 5배치(50건)입니다.
- `FOR UPDATE SKIP LOCKED`로 다른 API 인스턴스가 확보 중인 행을 건너뜁니다.
- 확보할 때 시도 횟수를 증가시키고 새 lease token과 60초 임대를 부여합니다.
- 성공·실패 기록은 행 잠금을 잡은 뒤 status·lease token·임대 만료를 검증합니다.
  이전 임대의 결과는 새 소유자의 상태를 변경할 수 없습니다.
- SQS 응답의 개별 성공/실패 목록을 확인합니다. 응답에 없는 이벤트도 성공으로 처리하지 않습니다.
- 실패는 1초부터 최대 5분까지 지수 백오프와 무작위 지연으로 재시도합니다.
  SDK 내부 재시도 횟수는 Outbox의 attempt_count에 별도로 더하지 않습니다.
- 만료된 임대는 폴링마다 최대 50건씩 회수하고 같은 백오프로 다시 PENDING에 넣습니다.
- SQS 호출 전체 제한은 10초(각 시도 3초), DB 트랜잭션 제한은 5초입니다.
- 미발행 이벤트는 실패 횟수를 이유로 버리지 않습니다. SQS 발행 전 실패는 Outbox에 남고,
  SQS 발행 이후 Worker 처리 실패에 대한 DLQ 정책은 별도입니다.

SQS 전송 성공 직후 서버가 종료되거나 DB 결과 기록에 실패하면 같은 이벤트가 다시 전송될 수 있습니다.
저장된 payload와 eventId를 재사용하며, Worker는 중복·순서 역전을 처리해야 합니다.

### 스케줄러·관측·정리

Relay의 예약 작업은 `searchSyncRelayScheduler`의 단일 전용 스레드를 사용합니다.
이 빈은 `defaultCandidate=false`로 등록하고 각 Relay 작업에서 이름으로 지정합니다.
따라서 기존 CRDT 정리·자정 추첨 및 웹 요청에 쓰이는 기본 스케줄러/실행기를 대체하지 않습니다.
종료 이벤트를 받으면 다음 배치를 확보하지 않고 진행 중인 작업 정리를 최대 20초 기다립니다.
완료하지 못한 작업은 임대 만료 후 복구합니다.

Actuator의 기존 MeterRegistry에 다음 지표를 등록합니다. eventId를 metric label로 사용하지 않습니다.

- `search.sync.relay.events`: claimed / published / retried / recovered / lost_lease / cleaned 건수
- `search.sync.relay.errors`: poll / metrics / cleanup 실행 오류
- `search.sync.relay.backlog`: PENDING / PROCESSING 건수
- `search.sync.relay.oldest.age`: 미발행 이벤트 중 가장 오래된 대기 시간(초)
- `search.sync.relay.sample.age`: 마지막 backlog 조회 이후 경과 시간(초)

Backlog 지표는 30초 간격으로 갱신하며, 최초 조회 전에는 NaN입니다.
지속적인 실패, 가장 오래된 대기 시간, 지표 갱신 중단을 알림 대상으로 사용할 수 있습니다.
발행 후 7일이 지난 PUBLISHED 행만 매시간 최대 1000건씩 정리합니다.
대기·처리 중인 이벤트는 정리하지 않습니다. 삭제 처리량은 배치 상한으로 제한됩니다.

기존 Outbox 마이그레이션을 먼저 적용해야 합니다. 실제 API/DB/SQS 환경에서의 발행 및 복구 확인은
큐와 IAM 설정을 연결한 뒤 수행합니다. 이 구현에서 운영 설정을 활성화하거나 DB에 SQL을 실행하지 않습니다.
