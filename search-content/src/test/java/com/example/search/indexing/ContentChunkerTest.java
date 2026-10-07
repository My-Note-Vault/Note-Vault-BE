package com.example.search.indexing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("검색 본문 청킹 단위 테스트")
class ContentChunkerTest {
    private final ContentChunker chunker = new ContentChunker();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t"})
    @DisplayName("비어 있는 본문은 임베딩할 청크를 생성하지 않는다")
    void blankContentHasNoChunks(String content) {
        assertThat(chunker.chunk(content)).isEmpty();
    }

    @Test
    @DisplayName("긴 본문은 유니코드 문자를 깨뜨리지 않고 1800자 단위로 나눈다")
    void splitsByCodePoint() {
        String content = "😀".repeat(3601);
        var chunks = chunker.chunk(content);

        assertThat(chunks).extracting(ChunkDraft::index).containsExactly(0, 1, 2);
        assertThat(chunks).extracting(ChunkDraft::content)
                .containsExactly("😀".repeat(1800), "😀".repeat(1800), "😀");
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.hash()).isEqualTo(ContentChunker.sha256(chunk.content())));
    }

    @Test
    @DisplayName("충분히 긴 Markdown 섹션은 다음 제목 경계에서 분리한다")
    void splitsAtHeadingBoundary() {
        String first = "# 첫 번째\n" + "가".repeat(700);
        String second = "## 두 번째\n짧은 내용";
        assertThat(chunker.chunk(first + "\n" + second)).extracting(ChunkDraft::content)
                .containsExactly(first, second);
    }

    @ParameterizedTest
    @ValueSource(strings = {"```", "~~~"})
    @DisplayName("코드 블록 내부의 제목 기호는 섹션 경계로 취급하지 않는다")
    void preservesFencedCode(String fence) {
        String content = "# 제목\n" + "가".repeat(700) + "\n" + fence + "\n# 코드 주석\n" + fence;
        assertThat(chunker.chunk(content)).extracting(ChunkDraft::content).containsExactly(content);
    }

    @Test
    @DisplayName("줄바꿈 형식이 달라도 동일한 청크와 해시를 생성한다")
    void normalizesLineEndings() {
        assertThat(chunker.chunk("# 제목\r\n본문\r\n")).isEqualTo(chunker.chunk("# 제목\n본문\n"));
        assertThat(ContentChunker.sha256("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
