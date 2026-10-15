package net.java21.library.front.web;

/**
 * 화면에 안내하는 AI 모델 정보 (모델 이름만 담고 API 키·서버 주소는 담지 않습니다)
 *
 * @param chatProvider   LLM 제공자 표시 이름 (Ollama | Gemini)
 * @param chatModel      AI 추천(RAG)에 쓰는 LLM(채팅) 모델 이름
 * @param embeddingModel 의미·하이브리드·RAG 검색에 쓰는 임베딩 모델 이름
 */
public record AiModelInfo(String chatProvider, String chatModel, String embeddingModel) {

    static final String OLLAMA = "ollama";
    private static final String OLLAMA_LABEL = "Ollama";
    private static final String GEMINI_LABEL = "Gemini";
    private static final String UNKNOWN_MODEL = "기본 모델";

    /**
     * {@code spring.ai.selected-model} 값으로 사용하는 채팅 모델을 고릅니다.
     * {@code AiConfig}와 같이 ollama가 아니면(미지정 포함) Gemini로 봅니다.
     *
     * @param selectedModel  spring.ai.selected-model 값
     * @param ollamaModel    spring.ai.ollama.chat.options.model 값
     * @param geminiModel    spring.ai.google.genai.chat.options.model 값
     * @param embeddingModel spring.ai.openai.embedding.options.model 값
     * @return 화면 안내용 모델 정보
     */
    public static AiModelInfo of(String selectedModel, String ollamaModel, String geminiModel, String embeddingModel) {
        boolean ollama = OLLAMA.equalsIgnoreCase(selectedModel == null ? "" : selectedModel.trim());
        return new AiModelInfo(
                ollama ? OLLAMA_LABEL : GEMINI_LABEL,
                orDefault(ollama ? ollamaModel : geminiModel),
                orDefault(embeddingModel));
    }

    private static String orDefault(String value) {
        return value == null || value.isBlank() ? UNKNOWN_MODEL : value.trim();
    }
}
