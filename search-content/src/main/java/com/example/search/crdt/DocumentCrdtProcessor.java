package com.example.search.crdt;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.Semaphore;

/** One restoration produces both the durable snapshot and the body used by indexing. */
@Service
@Transactional(propagation = Propagation.NEVER)
public class DocumentCrdtProcessor {
    private final DocumentCrdtTransactions transactions;
    private final YjsDocumentConverter converter;
    private final Semaphore permits;

    public DocumentCrdtProcessor(DocumentCrdtTransactions transactions, YjsDocumentConverter converter,
                                 ProjectionProperties properties) {
        this.transactions = transactions;
        this.converter = converter;
        this.permits = new Semaphore(properties.concurrency(), true);
    }

    public void refreshDocument(long documentId, long requestedRevision) {
        acquire();
        try {
            var input = transactions.load(documentId, requestedRevision);
            if (input.isEmpty()) return;
            var captured = input.get();
            var projection = converter.project(captured.state(), captured.updates());
            transactions.saveProjection(captured, projection);
        } finally {
            permits.release();
        }
    }

    private void acquire() {
        try {
            // Wait without a DB connection. The consumer keeps the SQS visibility lease alive.
            permits.acquire();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("CRDT delivery interrupted");
        }
    }
}
