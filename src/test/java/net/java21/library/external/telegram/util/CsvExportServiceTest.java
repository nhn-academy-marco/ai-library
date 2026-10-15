package net.java21.library.external.telegram.util;

import net.java21.library.external.telegram.dto.FeedbackType;
import net.java21.library.external.telegram.entity.SearchFeedback;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CsvExportService 단위 테스트")
class CsvExportServiceTest {

    private static final Long CHAT_ID = 987654321L;

    private final CsvExportService csvExportService = new CsvExportService();

    @Test
    @DisplayName("CSV에는 원본 chatId 대신 해시된 chatId를 내보낸다")
    void generateCsv_hashesChatId() {
        SearchFeedback feedback = SearchFeedback.of(CHAT_ID, "spring", 1L, FeedbackType.GOOD);
        ReflectionTestUtils.setField(feedback, "id", 1L);
        ReflectionTestUtils.setField(feedback, "createdAt", OffsetDateTime.parse("2026-10-15T10:00:00+09:00"));

        String csv = csvExportService.generateCsv(List.of(feedback));

        assertThat(csv).doesNotContain(CHAT_ID.toString());
        assertThat(csv).contains(PrivacyUtil.hashChatId(CHAT_ID));
        assertThat(csv.lines().findFirst()).hasValue("ID,ChatIdHash,Query,BookId,Type,CreatedAt");
    }
}
