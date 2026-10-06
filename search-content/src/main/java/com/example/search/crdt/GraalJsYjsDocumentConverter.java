package com.example.search.crdt;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.IOAccess;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.example.search.crdt.YjsProjectionException.Reason.*;

/** Runs a bundled, synchronous Yjs program; no Node process or request-thread execution. */
public final class GraalJsYjsDocumentConverter
        implements YjsDocumentConverter, HealthIndicator, AutoCloseable {
    private static final String PRELUDE = """
            for (const name of ['setTimeout', 'setInterval', 'clearTimeout', 'clearInterval']) {
              globalThis[name] = () => { throw new Error('Timers are unsupported in the Yjs projector'); };
            }
            """;

    private final ProjectionProperties properties;
    private final ObjectMapper mapper;
    private final Source bundle;
    private final Source prelude;
    private final Engine engine;
    private final YjsHostBridge host = new YjsHostBridge();
    private final ScheduledThreadPoolExecutor watchdog;
    private final Set<Context> activeContexts = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean();

    public GraalJsYjsDocumentConverter(ProjectionProperties properties, ObjectMapper mapper)
            throws IOException {
        this.properties = properties;
        this.mapper = mapper.copy();
        int maxString = (int) Math.min(Integer.MAX_VALUE,
                Math.max(encodedLimit(), properties.maxContentCharacters()));
        this.mapper.getFactory().setStreamReadConstraints(StreamReadConstraints.builder()
                .maxStringLength(maxString).build());
        bundle = Source.newBuilder("js", readResource("crdt/yjs-projector.js"), "yjs-projector.js")
                .cached(true).build();
        prelude = Source.newBuilder("js", PRELUDE, "projector-runtime.js").cached(true).build();
        engine = Engine.create();
        watchdog = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "yjs-projection-timeout");
            thread.setDaemon(true);
            return thread;
        });
        watchdog.setRemoveOnCancelPolicy(true);
        try {
            verifyStartup();
        } catch (Exception exception) {
            close();
            throw new IllegalStateException("Yjs projection startup verification failed", exception);
        }
    }

    @Override
    public Projection project(byte[] baseState, List<byte[]> updates) {
        if (closed.get()) throw new IllegalStateException("Yjs projector is closed");
        if (updates.size() > properties.maxUpdates()) throw new YjsProjectionException(INPUT_LIMIT);
        long bytes = baseState == null ? 0 : baseState.length;
        for (byte[] update : updates) {
            if (update == null || update.length == 0) throw new YjsProjectionException(INVALID_RESULT);
            bytes += update.length;
        }
        if (bytes > properties.maxInputBytes()) throw new YjsProjectionException(INPUT_LIMIT);
        Base64.Encoder encoder = Base64.getEncoder();
        Request request = new Request(
                baseState == null || baseState.length == 0 ? null : encoder.encodeToString(baseState),
                updates.stream().map(encoder::encodeToString).toList(),
                properties.maxContentCharacters(), properties.maxStateBytes());
        return execute(request, properties.executionTimeout());
    }

    private Projection execute(Request request, Duration timeout) {
        AtomicBoolean timedOut = new AtomicBoolean();
        try (Context context = Context.newBuilder("js").engine(engine)
                .allowHostAccess(HostAccess.EXPLICIT)
                .allowHostClassLookup(name -> false)
                .allowIO(IOAccess.NONE).allowCreateThread(false).build()) {
            activeContexts.add(context);
            if (closed.get()) {
                activeContexts.remove(context);
                throw new IllegalStateException("Yjs projector is closed");
            }
            long deadline = System.nanoTime() + timeout.toNanos();
            ScheduledFuture<?> cancellation = null;
            try {
                cancellation = watchdog.schedule(() -> {
                    timedOut.set(true);
                    context.close(true);
                }, timeout.toMillis(), TimeUnit.MILLISECONDS);
                context.getBindings("js").putMember("__yjsHost", host);
                context.eval(prelude);
                context.eval(bundle);
                String json = context.getBindings("js").getMember("projectDocument")
                        .execute(mapper.writeValueAsString(request)).asString();
                if (timedOut.get() || System.nanoTime() > deadline) {
                    throw new YjsProjectionException(TIMEOUT);
                }
                long maxJson = encodedLimit() + 6L * properties.maxContentCharacters() + 1024;
                if (json.length() > maxJson) throw new YjsProjectionException(OUTPUT_LIMIT);
                Response result = mapper.readValue(json, Response.class);
                if ("WAITING_DEPENDENCIES".equals(result.status())) {
                    throw new YjsProjectionException(WAITING_DEPENDENCIES);
                }
                if ("OUTPUT_LIMIT".equals(result.status())) {
                    throw new YjsProjectionException(OUTPUT_LIMIT);
                }
                if (!"READY".equals(result.status())) throw new YjsProjectionException(INVALID_RESULT);
                if (result.content() == null || result.state() == null || result.state().isEmpty()) {
                    throw new YjsProjectionException(INVALID_RESULT);
                }
                if (result.content().length() > properties.maxContentCharacters()) {
                    throw new YjsProjectionException(OUTPUT_LIMIT);
                }
                if (result.state().length() > encodedLimit()) throw new YjsProjectionException(OUTPUT_LIMIT);
                byte[] state = Base64.getDecoder().decode(result.state());
                if (state.length == 0 || state.length > properties.maxStateBytes()) {
                    throw new YjsProjectionException(OUTPUT_LIMIT);
                }
                return new Projection(result.content(), state);
            } finally {
                if (cancellation != null) cancellation.cancel(false);
                activeContexts.remove(context);
            }
        } catch (YjsProjectionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new YjsProjectionException(timedOut.get() ? TIMEOUT : RUNTIME, exception);
        }
    }

    private void verifyStartup() throws IOException {
        StartupInput input = mapper.readValue(readResource("crdt/startup.json"), StartupInput.class);
        Projection result = execute(new Request(input.baseState(), input.updates(),
                properties.maxContentCharacters(), properties.maxStateBytes()), properties.startupTimeout());
        Projection restored = execute(new Request(
                Base64.getEncoder().encodeToString(result.crdtState()), List.of(),
                properties.maxContentCharacters(), properties.maxStateBytes()), properties.startupTimeout());
        if (!input.expectedContent().equals(result.content()) || !result.content().equals(restored.content())) {
            throw new IllegalStateException("Yjs startup content/snapshot verification failed");
        }
    }

    private long encodedLimit() {
        return 4L * ((properties.maxStateBytes() + 2L) / 3L);
    }

    private static String readResource(String name) throws IOException {
        try (var stream = new ClassPathResource(name).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Override
    public Health health() {
        return closed.get() ? Health.down().build() : Health.up().build();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        watchdog.shutdownNow();
        try {
            activeContexts.forEach(context -> context.close(true));
        } finally {
            engine.close(true);
        }
    }

    private record Request(String baseState, List<String> updates,
                           int maxContentCharacters, int maxStateBytes) { }
    private record Response(String status, String content, String state) { }
    private record StartupInput(String baseState, List<String> updates, String expectedContent) { }
}
