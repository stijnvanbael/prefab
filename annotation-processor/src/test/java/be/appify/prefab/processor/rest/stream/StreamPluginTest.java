package be.appify.prefab.processor.rest.stream;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.Compilation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Unit tests for {@link StreamPlugin}.
 *
 * <p>Verifies that the processor generates correct SSE controller endpoints and service methods
 * for both the pull model ({@code Stream<T>} return type) and the push model
 * ({@code @EventHandler @ByReference @Stream}).
 */
class StreamPluginTest {

    public static final Compilation pullSessionCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("stream/pull/source/Session.java"));
    public static final Compilation pushCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(
                    sourceOf("stream/push/source/TokenEmitted.java"),
                    sourceOf("stream/push/source/ChatSession.java"));

    @Test
    @DisplayName("Pull model: @Stream generates SSE controller endpoint with correct path")
    void pullModel_generatesControllerEndpoint() {
        assertThat(pullSessionCompilation).succeeded();
        assertThat(pullSessionCompilation)
                .generatedSourceFile("stream.pull.infrastructure.http.SessionController")
                .contentsAsUtf8String()
                .contains("/{id}/stream");
    }

    @Test
    @DisplayName("Pull model: generated SSE endpoint produces text/event-stream content type")
    void pullModel_generatesTextEventStreamProduces() {
        assertThat(pullSessionCompilation).succeeded();
        assertThat(pullSessionCompilation)
                .generatedSourceFile("stream.pull.infrastructure.http.SessionController")
                .contentsAsUtf8String()
                .contains("TEXT_EVENT_STREAM_VALUE");
    }

    @Test
    @DisplayName("Pull model: generated SSE endpoint uses virtual thread")
    void pullModel_generatesVirtualThreadUsage() {
        assertThat(pullSessionCompilation).succeeded();
        assertThat(pullSessionCompilation)
                .generatedSourceFile("stream.pull.infrastructure.http.SessionController")
                .contentsAsUtf8String()
                .contains("ofVirtual");
    }

    @Test
    @DisplayName("Pull model: generated SSE endpoint sends correct event name")
    void pullModel_generatesCorrectEventName() {
        assertThat(pullSessionCompilation).succeeded();
        assertThat(pullSessionCompilation)
                .generatedSourceFile("stream.pull.infrastructure.http.SessionController")
                .contentsAsUtf8String()
                .contains("\"token\"");
    }

    @Test
    @DisplayName("Pull model: generated SSE endpoint includes heartbeat")
    void pullModel_generatesHeartbeat() {
        assertThat(pullSessionCompilation).succeeded();
        assertThat(pullSessionCompilation)
                .generatedSourceFile("stream.pull.infrastructure.http.SessionController")
                .contentsAsUtf8String()
                .contains("scheduleHeartbeat");
    }

    @Test
    @DisplayName("Push model: SSE connect endpoint is generated in controller")
    void pushModel_generatesSseConnectEndpoint() {
        assertThat(pushCompilation).succeeded();
        assertThat(pushCompilation)
                .generatedSourceFile("stream.push.infrastructure.http.ChatSessionController")
                .contentsAsUtf8String()
                .contains("SseEmitter stream");
    }

    @Test
    @DisplayName("Push model: SseRegistry class is generated")
    void pushModel_generatesSseRegistry() {
        assertThat(pushCompilation).succeeded();
        assertThat(pushCompilation)
                .generatedSourceFile("stream.push.infrastructure.http.ChatSessionSseRegistry")
                .isNotNull();
    }

    @Test
    @DisplayName("Push model: service method includes SSE push logic")
    void pushModel_serviceMethodIncludesSsePush() {
        assertThat(pushCompilation).succeeded();
        assertThat(pushCompilation)
                .generatedSourceFile("stream.push.application.ChatSessionService")
                .contentsAsUtf8String()
                .contains("ChatSessionSseRegistry");
    }

    @Test
    @DisplayName("Push model: terminal=done generates emitter.complete() call")
    void pushModel_terminalFieldGeneratesCompleteCall() {
        assertThat(pushCompilation).succeeded();
        assertThat(pushCompilation)
                .generatedSourceFile("stream.push.application.ChatSessionService")
                .contentsAsUtf8String()
                .contains("emitter.complete()");
    }

    @Test
    @DisplayName("Pull model: @Stream on Stream<MyRecord> generates correct JSON SSE data serialisation")
    void pullModel_streamOfRecord_generatesJsonDataSend() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("stream/pull/source/TokenItem.java"),
                        sourceOf("stream/pull/source/RecordSession.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("stream.pull.infrastructure.http.RecordSessionController")
                .contentsAsUtf8String()
                .contains(".data(item)");
    }

    @Test
    @DisplayName("Pull model: @Stream on Flux<T> generates subscribe call")
    void pullModel_flux_generatesSubscribeCall() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("stream/pull/source/FluxSession.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("stream.pull.infrastructure.http.FluxSessionController")
                .contentsAsUtf8String()
                .contains(".subscribe(");
    }
}

