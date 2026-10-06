package com.notevault.searchworker;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Container readiness is published after GraalJS verification, schema checks and consumer startup. */
@Component
public class WorkerReadiness {
    public static final Path FILE = Path.of("/tmp/note-vault-worker-ready");

    @EventListener(ApplicationReadyEvent.class)
    public void ready() throws IOException {
        Files.writeString(FILE, "ready\n");
    }

    @EventListener(ContextClosedEvent.class)
    public void closed() throws IOException {
        Files.deleteIfExists(FILE);
    }
}
