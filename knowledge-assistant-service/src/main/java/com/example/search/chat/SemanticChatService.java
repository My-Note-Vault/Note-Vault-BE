package com.example.search.chat;

import com.example.search.chat.policy.ChatPolicy;
import com.example.search.retrieval.SearchEvidence;
import com.example.search.retrieval.HybridSearch;
import com.example.search.retrieval.RetrievalTrace;
import com.example.search.infrastructure.OpenAiSearchClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class SemanticChatService {
    private final HybridSearch search;
    private final OpenAiSearchClient openAi;
    private final KeywordExtractor keywordExtractor;

    @Value("${openai.chat.top-k:6}")
    private int topK;

    public ChatResult chat(Long memberId, String question) {
        ChatPreparation preparation = prepare(memberId, question);
        if (!preparation.hasContext()) {
            return new ChatResult("NO_CONTEXT", "관련 문서 내용을 찾지 못했습니다.", List.of());
        }
        return new ChatResult("ANSWERED",
                openAi.answer(preparation.question(), preparation.context()), preparation.sources());
    }

    public ChatPreparation prepare(Long memberId, String question) {
        String normalizedQuestion = ChatPolicy.normalizeQuestion(question);
        return toPreparation(normalizedQuestion, search.search(memberId, normalizedQuestion,
                keywordExtractor.extract(normalizedQuestion), openAi.embeddingModel(),
                openAi.embedQuestion(normalizedQuestion), topK));
    }

    public EvaluationPreparation prepareForEvaluation(Long memberId, String question) {
        long started = System.nanoTime();
        String normalizedQuestion = ChatPolicy.normalizeQuestion(question);
        KeywordExtractor.Extraction extracted = keywordExtractor.extractDetailed(normalizedQuestion);
        long embeddingStarted = System.nanoTime();
        String embedding = openAi.embedQuestion(normalizedQuestion);
        long embeddingMs = (System.nanoTime() - embeddingStarted) / 1_000_000;
        HybridSearch.SearchResult result = search.searchWithTrace(memberId, normalizedQuestion, extracted.keywords(),
                openAi.embeddingModel(), embedding, topK);
        SearchDiagnostics trace = new SearchDiagnostics(extracted,
                new EmbeddingInfo(openAi.embeddingModel(), (int) embedding.chars().filter(c -> c == ',').count() + 1,
                        embeddingMs), result.trace(), (System.nanoTime() - started) / 1_000_000);
        return new EvaluationPreparation(toPreparation(normalizedQuestion, result.evidence()), trace);
    }

    private ChatPreparation toPreparation(String normalizedQuestion, List<SearchEvidence> found) {
        if (found.isEmpty()) {
            return new ChatPreparation(normalizedQuestion, "", List.of());
        }

        StringBuilder context = new StringBuilder();
        List<Source> sources = new ArrayList<>();
        for (int index = 0; index < found.size(); index++) {
            SearchEvidence evidence = found.get(index);
            int number = index + 1;
            context.append('[').append(number).append("]\n문서: ")
                    .append(evidence.source().title()).append("\n내용:\n")
                    .append(evidence.content()).append("\n\n");
            sources.add(new Source(number, evidence.chunkId(), evidence.source().sourceType().name(),
                    evidence.resourceId(), evidence.resourceType(), evidence.source().title(),
                    evidence.similarity(), excerpt(evidence.content())));
        }
        return new ChatPreparation(normalizedQuestion, context.toString(), sources);
    }

    public void streamAnswer(ChatPreparation preparation, java.util.function.Consumer<String> onDelta) {
        openAi.streamAnswer(preparation.question(), preparation.context(), onDelta);
    }

    private String excerpt(String content) {
        return content.length() <= 300 ? content : content.substring(0, 300) + "…";
    }

    public record Source(int number, Long chunkId, String sourceType, Long resourceId,
                         String resourceType, String title, Double similarity, String excerpt) {
    }

    public record ChatResult(String status, String answer, List<Source> sources) {
    }

    public record EmbeddingInfo(String model, int dimensions, long durationMs) { }
    public record SearchDiagnostics(KeywordExtractor.Extraction keywords, EmbeddingInfo embedding,
                                    RetrievalTrace retrieval, long preparationDurationMs) { }
    public record EvaluationPreparation(ChatPreparation preparation, SearchDiagnostics diagnostics) { }

    public record ChatPreparation(String question, String context, List<Source> sources) {
        public boolean hasContext() {
            return !sources.isEmpty();
        }
    }
}
