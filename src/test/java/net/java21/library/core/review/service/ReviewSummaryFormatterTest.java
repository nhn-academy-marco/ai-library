package net.java21.library.core.review.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReviewSummaryFormatter 단위 테스트")
class ReviewSummaryFormatterTest {

    /** 실제 화면에 노출됐던 LLM 출력 (Markdown, 인사말, 제목 줄 포함) */
    private static final String RAW = """
            물론입니다! 기존 요약과 새로운 리뷰 요약본을 통합하여 최종 요약을 작성해 드리겠습니다.

            **최종 요약**
            이 책은 스프링 프레임워크의 최신 기능과 기술을 상세히 설명합니다.

            **[장점]**: 이 책은 스프링 3의 최신 기능과 기술을 상세히 설명합니다.

            **[단점]**: 초보자는 이해하기 어려울 수 있습니다.

            **[추천대상]**: 스프링 프레임워크를 사용하는 개발자에게 추천됩니다.
            """;

    @Test
    @DisplayName("Markdown 기호, 인사말, 제목 줄을 제거한다")
    void cleanRemovesMarkdownAndPreamble() {
        String cleaned = ReviewSummaryFormatter.clean(RAW);

        assertThat(cleaned)
                .doesNotContain("**")
                .doesNotContain("물론입니다")
                .doesNotContain("최종 요약")
                .startsWith("이 책은 스프링 프레임워크의");
    }

    @Test
    @DisplayName("항목 머리말을 인식해 정해진 순서로 나눈다")
    void parseSplitsIntoSections() {
        List<ReviewSummaryFormatter.Section> sections = ReviewSummaryFormatter.parse(RAW);

        assertThat(sections).extracting(ReviewSummaryFormatter.Section::label)
                .containsExactly("요약", "장점", "단점", "추천 대상");
        assertThat(sections.get(1).text()).isEqualTo("이 책은 스프링 3의 최신 기능과 기술을 상세히 설명합니다.");
        assertThat(sections.get(3).text()).isEqualTo("스프링 프레임워크를 사용하는 개발자에게 추천됩니다.");
    }

    @Test
    @DisplayName("프롬프트 출력 형식 그대로인 요약을 나눈다")
    void parseExpectedFormat() {
        String formatted = """
                [장점] 예제가 풍부합니다.
                [단점] 분량이 많습니다.
                [추천 대상] 입문자에게 권합니다.
                [총평] 실무에 바로 쓸 수 있는 책입니다.
                """;

        assertThat(ReviewSummaryFormatter.parse(formatted))
                .extracting(ReviewSummaryFormatter.Section::label)
                .containsExactly("장점", "단점", "추천 대상", "총평");
    }

    @Test
    @DisplayName("항목 머리말이 없으면 빈 목록을 반환한다")
    void parseWithoutSectionsReturnsEmpty() {
        assertThat(ReviewSummaryFormatter.parse("아직 등록된 리뷰가 없습니다.")).isEmpty();
        assertThat(ReviewSummaryFormatter.parse(null)).isEmpty();
        assertThat(ReviewSummaryFormatter.clean(null)).isNull();
    }
}
