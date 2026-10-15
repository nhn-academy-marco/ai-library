package net.java21.library.front.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 화면에 AI 추천·임베딩 모델 이름을 {@code aiModels}로 제공합니다.
 *
 * <p>값은 설정에서 읽으며 모델 이름 외의 설정(API 키, base-url)은 노출하지 않습니다.</p>
 */
@ControllerAdvice(annotations = Controller.class)
public class AiModelInfoAdvice {

    private final AiModelInfo aiModelInfo;

    public AiModelInfoAdvice(@Value("${spring.ai.selected-model:gemini}") String selectedModel,
                             @Value("${spring.ai.ollama.chat.options.model:}") String ollamaModel,
                             @Value("${spring.ai.google.genai.chat.options.model:}") String geminiModel,
                             @Value("${spring.ai.openai.embedding.options.model:}") String embeddingModel) {
        this.aiModelInfo = AiModelInfo.of(selectedModel, ollamaModel, geminiModel, embeddingModel);
    }

    @ModelAttribute("aiModels")
    public AiModelInfo aiModels() {
        return aiModelInfo;
    }
}
