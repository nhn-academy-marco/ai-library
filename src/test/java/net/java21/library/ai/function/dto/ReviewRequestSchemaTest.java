package net.java21.library.ai.function.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReviewRequest Tool 입력 스키마 테스트")
class ReviewRequestSchemaTest {

    @Test
    @DisplayName("GetReviews Tool 입력 스키마는 bookId 속성을 가진 object여야 한다")
    void schemaIsObjectWithBookId() {
        String schema = JsonSchemaGenerator.generateForType(ReviewRequest.class);

        assertThat(schema).contains("\"type\" : \"object\"").contains("\"bookId\"");
    }
}
