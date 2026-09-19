package com.example.search.chat;

import com.example.search.retrieval.IndexedChunk;
import com.example.search.retrieval.HybridSearch;
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
        if (question == null || question.isBlank() || question.length() > 4000) {
            throw new IllegalArgumentException("질문은 1자 이상 4000자 이하여야 합니다.");
        }

        String normalizedQuestion = question.trim();
        List<HybridSearch.Result> found = search.search(memberId, normalizedQuestion,
                keywordExtractor.extract(normalizedQuestion), openAi.embeddingModel(),
                openAi.embedQuestion(normalizedQuestion), topK);
        if (found.isEmpty()) {
            return new ChatPreparation(question.trim(), "", List.of());
        }

        StringBuilder context = new StringBuilder();
        List<Source> sources = new ArrayList<>();
        for (int index = 0; index < found.size(); index++) {
            HybridSearch.Result result = found.get(index);
            IndexedChunk chunk = result.chunk();
            int number = index + 1;
            context.append('[').append(number).append("]\n문서: ")
                    .append(chunk.sourceTitle()).append("\n내용:\n")
                    .append(chunk.content()).append("\n\n");
            sources.add(new Source(number, chunk.id(), chunk.sourceType(),
                    chunk.resourceId(), chunk.resourceType(), chunk.sourceTitle(),
                    result.semantic(), excerpt(chunk.content())));
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
                         String resourceType, String title, double similarity, String excerpt) {
    }

    public record ChatResult(String status, String answer, List<Source> sources) {
    }

    public record ChatPreparation(String question, String context, List<Source> sources) {
        public boolean hasContext() {
            return !sources.isEmpty();
        }
    }
}
