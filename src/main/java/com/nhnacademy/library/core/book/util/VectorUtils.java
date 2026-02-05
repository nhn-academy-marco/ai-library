package com.nhnacademy.library.core.book.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 벡터 연산 유틸리티 클래스
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VectorUtils {

    /**
     * 두 벡터 간의 코사인 유사도를 계산합니다.
     *
     * @param vectorA 벡터 A
     * @param vectorB 벡터 B
     * @return 코사인 유사도 (0.0 ~ 1.0)
     */
    public static double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
