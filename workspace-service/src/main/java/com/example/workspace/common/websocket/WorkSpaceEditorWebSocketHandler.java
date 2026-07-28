package com.example.workspace.common.websocket;

import com.example.workspace.common.redis.RedisPublisher;
import com.example.workspace.document.command.application.DocumentCommandService;
import com.example.workspace.document.command.domain.DocumentDelta;
import com.example.workspace.document.command.domain.DocumentType;
import com.example.workspace.document.query.DocumentQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
@Component
public class WorkSpaceEditorWebSocketHandler extends BinaryWebSocketHandler {

    private static final byte[] BOOTSTRAP_COMPLETE_MESSAGE = {2};
    private static final int MESSAGE_DOCUMENT_UPDATE = 3;
    private static final int MESSAGE_UPDATE_ACK = 4;
    private static final int MESSAGE_SEARCH_PROJECTION = 5;
    private static final int MESSAGE_CRDT_SYNC_REQUEST = 6;
    private static final int MESSAGE_CRDT_SNAPSHOT = 7;

    private final SessionRegistry sessionRegistry;
    private final RedisPublisher redisPublisher;
    private final DocumentCommandService documentCommandService;
    private final DocumentQueryService documentQueryService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long persistenceDocumentId = documentId(session);
        if ("space".equals(documentType(session))) {
            persistenceDocumentId = documentCommandService.ensureWorkspaceHomeDocument(
                    memberId(session),
                    workSpaceId(session)
            );
        }
        session.getAttributes().put(
                CollaborationSessionAttributes.PERSISTENCE_DOCUMENT_ID,
                persistenceDocumentId
        );

        sessionRegistry.add(sessionKey(session), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info(
                "CRDT WebSocket closed: sessionId={}, documentType={}, documentId={}, status={}",
                session.getId(),
                documentType(session),
                documentId(session),
                status
        );
        sessionRegistry.remove(sessionKey(session), session);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        Long workSpaceId = workSpaceId(session);
        Long memberId = memberId(session);
        String documentType = documentType(session);
        Long documentId = persistenceDocumentId(session);

        ByteBuffer readOnlyBuffer = message.getPayload().asReadOnlyBuffer();
        byte[] payload = new byte[readOnlyBuffer.remaining()];
        readOnlyBuffer.get(payload);

        int messageType = readMessageType(payload);
        if (messageType == MESSAGE_DOCUMENT_UPDATE) {
            ClientCrdtUpdate update = decodeDocumentUpdate(payload);
            DocumentCommandService.CommittedCrdtUpdate committed =
                    documentCommandService.appendDocumentDelta(
                            memberId,
                            workSpaceId,
                            documentId,
                            persistenceDocumentType(documentType),
                            update.clientUpdateId(),
                            update.crdtUpdate()
                    );
            sessionRegistry.sendTo(
                    sessionKey(session),
                    session.getId(),
                    encodeUpdateAck(committed)
            );
            redisPublisher.publish(
                    workSpaceId,
                    documentType,
                    documentId(session),
                    session.getId(),
                    encodeRevisionedUpdate(committed.revision(), committed.crdtUpdate())
            );
            return;
        }

        if (messageType == MESSAGE_CRDT_SYNC_REQUEST) {
            long lastAppliedRevision = decodeCrdtSyncRequest(payload);
            sendMissingCrdtUpdates(
                    session,
                    memberId,
                    workSpaceId,
                    documentId,
                    persistenceDocumentType(documentType),
                    lastAppliedRevision
            );
            return;
        }

        if (messageType == MESSAGE_SEARCH_PROJECTION) {
            SearchProjection projection = decodeSearchProjection(payload);
            documentCommandService.updateSearchProjection(
                    memberId,
                    workSpaceId,
                    documentId,
                    persistenceDocumentType(documentType),
                    projection.revision(),
                    projection.content(),
                    projection.crdtState()
            );
            return;
        }

        redisPublisher.publish(
                workSpaceId,
                documentType,
                documentId(session),
                session.getId(),
                payload
        );
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error(
                "CRDT WebSocket transport error: sessionId={}, documentType={}, documentId={}",
                session.getId(),
                documentType(session),
                documentId(session),
                exception
        );
        sessionRegistry.remove(sessionKey(session), session);

        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private SessionKey sessionKey(final WebSocketSession session) {
        return new SessionKey(documentType(session), documentId(session));
    }

    private Long workSpaceId(final WebSocketSession session) {
        return (Long) session.getAttributes().get(CollaborationSessionAttributes.WORKSPACE_ID);
    }

    private Long memberId(final WebSocketSession session) {
        return (Long) session.getAttributes().get(CollaborationSessionAttributes.MEMBER_ID);
    }

    private String documentType(final WebSocketSession session) {
        return (String) session.getAttributes().get(CollaborationSessionAttributes.DOCUMENT_TYPE);
    }

    private Long documentId(final WebSocketSession session) {
        return (Long) session.getAttributes().get(CollaborationSessionAttributes.DOCUMENT_ID);
    }

    private Long persistenceDocumentId(final WebSocketSession session) {
        return (Long) session.getAttributes().get(
                CollaborationSessionAttributes.PERSISTENCE_DOCUMENT_ID
        );
    }

    private DocumentType persistenceDocumentType(final String documentType) {
        return "space".equals(documentType)
                ? DocumentType.WORKSPACE_HOME
                : DocumentType.valueOf(documentType.toUpperCase());
    }

    private void sendMissingCrdtUpdates(
            final WebSocketSession session,
            final Long memberId,
            final Long workSpaceId,
            final Long documentId,
            final DocumentType documentType,
            final long lastAppliedRevision
    ) {
        DocumentQueryService.CollaborationHistory history =
                documentQueryService.findCollaborationHistory(
                        memberId,
                        workSpaceId,
                        documentId,
                        documentType,
                        lastAppliedRevision
                );

        if (history.crdtState() != null && history.crdtState().length > 0) {
            sessionRegistry.sendTo(
                    sessionKey(session),
                    session.getId(),
                    encodeCrdtSnapshot(history.snapshotRevision(), history.crdtState())
            );
        }
        for (DocumentDelta delta : history.deltas()) {
            sessionRegistry.sendTo(
                    sessionKey(session),
                    session.getId(),
                    encodeRevisionedUpdate(delta.getRevision(), delta.getCrdtUpdate())
            );
        }
        sessionRegistry.sendTo(
                sessionKey(session),
                session.getId(),
                encodeBootstrapComplete(history.latestRevision())
        );
    }

    private byte[] encodeCrdtSnapshot(final long revision, final byte[] update) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeVarUint(output, MESSAGE_CRDT_SNAPSHOT);
        writeVarLong(output, revision);
        writeVarUint(output, update.length);
        output.writeBytes(update);
        return output.toByteArray();
    }

    private byte[] encodeRevisionedUpdate(final Long revision, final byte[] update) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeVarUint(output, MESSAGE_DOCUMENT_UPDATE);
        writeVarLong(output, revision);
        writeVarUint(output, update.length);
        output.writeBytes(update);
        return output.toByteArray();
    }

    private byte[] encodeUpdateAck(final DocumentCommandService.CommittedCrdtUpdate committed) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeVarUint(output, MESSAGE_UPDATE_ACK);
        writeVarString(output, committed.clientUpdateId());
        writeVarLong(output, committed.revision());
        writeVarUint(output, committed.crdtUpdate().length);
        output.writeBytes(committed.crdtUpdate());
        return output.toByteArray();
    }

    private byte[] encodeBootstrapComplete(final long latestRevision) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.writeBytes(BOOTSTRAP_COMPLETE_MESSAGE);
        writeVarLong(output, latestRevision);
        return output.toByteArray();
    }

    private int readMessageType(final byte[] payload) {
        try {
            return readVarUint(payload, new int[]{0});
        } catch (IllegalArgumentException exception) {
            return -1;
        }
    }

    private ClientCrdtUpdate decodeDocumentUpdate(final byte[] payload) {
        int[] cursor = {0};
        if (readVarUint(payload, cursor) != MESSAGE_DOCUMENT_UPDATE) {
            throw new IllegalArgumentException("Invalid document update message");
        }
        String clientUpdateId = readVarString(payload, cursor);
        int length = readVarUint(payload, cursor);
        if (length < 0 || cursor[0] + length != payload.length) {
            throw new IllegalArgumentException("Invalid document update length");
        }
        byte[] crdtUpdate = Arrays.copyOfRange(payload, cursor[0], cursor[0] + length);
        return new ClientCrdtUpdate(clientUpdateId, crdtUpdate);
    }

    private long decodeCrdtSyncRequest(final byte[] payload) {
        int[] cursor = {0};
        if (readVarUint(payload, cursor) != MESSAGE_CRDT_SYNC_REQUEST) {
            throw new IllegalArgumentException("Invalid CRDT sync request");
        }
        long revision = readVarLong(payload, cursor);
        if (cursor[0] != payload.length) {
            throw new IllegalArgumentException("Invalid CRDT sync request length");
        }
        return revision;
    }

    private SearchProjection decodeSearchProjection(final byte[] payload) {
        int[] cursor = {0};
        if (readVarUint(payload, cursor) != MESSAGE_SEARCH_PROJECTION) {
            throw new IllegalArgumentException("Invalid search projection message");
        }
        long revision = readVarLong(payload, cursor);
        int length = readVarUint(payload, cursor);
        if (length < 0 || cursor[0] + length > payload.length) {
            throw new IllegalArgumentException("Invalid search projection length");
        }
        String content = new String(payload, cursor[0], length, StandardCharsets.UTF_8);
        cursor[0] += length;
        int stateLength = readVarUint(payload, cursor);
        if (stateLength < 0 || cursor[0] + stateLength != payload.length) {
            throw new IllegalArgumentException("Invalid CRDT state length");
        }
        byte[] crdtState = Arrays.copyOfRange(
                payload,
                cursor[0],
                cursor[0] + stateLength
        );
        return new SearchProjection(revision, content, crdtState);
    }

    private int readVarUint(final byte[] payload, final int[] cursor) {
        int value = 0;
        int shift = 0;
        while (cursor[0] < payload.length && shift <= 28) {
            int next = payload[cursor[0]++] & 0xff;
            value |= (next & 0x7f) << shift;
            if ((next & 0x80) == 0) {
                return value;
            }
            shift += 7;
        }
        throw new IllegalArgumentException("Invalid Yjs varUint");
    }

    private long readVarLong(final byte[] payload, final int[] cursor) {
        long value = 0;
        int shift = 0;
        while (cursor[0] < payload.length && shift <= 63) {
            int next = payload[cursor[0]++] & 0xff;
            value |= (long) (next & 0x7f) << shift;
            if ((next & 0x80) == 0) {
                return value;
            }
            shift += 7;
        }
        throw new IllegalArgumentException("Invalid Yjs varLong");
    }

    private String readVarString(final byte[] payload, final int[] cursor) {
        int length = readVarUint(payload, cursor);
        if (length < 0 || cursor[0] + length > payload.length) {
            throw new IllegalArgumentException("Invalid string length");
        }
        String value = new String(payload, cursor[0], length, StandardCharsets.UTF_8);
        cursor[0] += length;
        return value;
    }

    private void writeVarUint(final ByteArrayOutputStream output, int value) {
        while (value > 127) {
            output.write((value & 127) | 128);
            value >>>= 7;
        }
        output.write(value & 127);
    }

    private void writeVarLong(final ByteArrayOutputStream output, long value) {
        while (value > 127) {
            output.write(((int) value & 127) | 128);
            value >>>= 7;
        }
        output.write((int) value & 127);
    }

    private void writeVarString(final ByteArrayOutputStream output, final String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarUint(output, bytes.length);
        output.writeBytes(bytes);
    }

    private record ClientCrdtUpdate(String clientUpdateId, byte[] crdtUpdate) {
    }

    private record SearchProjection(long revision, String content, byte[] crdtState) {
    }
}
