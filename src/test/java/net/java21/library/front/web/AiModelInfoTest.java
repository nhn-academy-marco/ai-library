package net.java21.library.front.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AiModelInfo: 설정값으로 화면 안내용 모델 이름 결정")
class AiModelInfoTest {

    @Test
    @DisplayName("selected-model=ollama이면 Ollama 채팅 모델을 쓴다")
    void ollama() {
        AiModelInfo info = AiModelInfo.of("ollama", "qwen2.5:latest", "gemini-2.0-flash", "bge-m3");

        assertThat(info).isEqualTo(new AiModelInfo("Ollama", "qwen2.5:latest", "bge-m3"));
    }

    @Test
    @DisplayName("selected-model이 비어 있으면 AiConfig 기본값과 같이 Gemini 채팅 모델을 쓴다")
    void defaultGemini() {
        AiModelInfo info = AiModelInfo.of("", "qwen2.5:latest", "gemini-2.0-flash", "bge-m3");

        assertThat(info).isEqualTo(new AiModelInfo("Gemini", "gemini-2.0-flash", "bge-m3"));
    }

    @Test
    @DisplayName("모델 설정이 없으면 '기본 모델'로 표시한다")
    void missingModel() {
        AiModelInfo info = AiModelInfo.of("gemini", "", null, "");

        assertThat(info.chatModel()).isEqualTo("기본 모델");
        assertThat(info.embeddingModel()).isEqualTo("기본 모델");
    }
}
