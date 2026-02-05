package com.nhnacademy.library.core.book.service.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public float[] getEmbedding(String text) {
        float[] result = embeddingModel.embed(text);
        if (result == null) {
            return new float[0];
        }
        return result;
    }

    public List<float[]> getEmbeddings(List<String> texts) {
        return embeddingModel.embed(texts);
    }
}
