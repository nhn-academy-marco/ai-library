package com.nhnacademy.library;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.ai.selected-model=ollama",
    "rabbitmq.queue.review-summary=nhnacademy-library-review"
})
class NhnAcademyAiLibraryApplicationTests {

    @Test
    void contextLoads() {
    }

}
