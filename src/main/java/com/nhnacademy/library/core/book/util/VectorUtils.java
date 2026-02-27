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
     * <p>코사인 유사도는 두 벡터 간의 각도의 코사인 값으로,
     * -1.0 (완전히 반대) ~ 1.0 (완전히 같은 방향) 범위를 가집니다.</p>
     *
     * @param vectorA 첫 번째 벡터
     * @param vectorB 두 번째 벡터
     * @return 코사인 유사도 (-1.0 ~ 1.0)
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

    /**
     * 벡터들의 평균 벡터를 계산합니다.
     *
     * <p>사용자 선호도를 계산할 때, 여러 도서의 임베딩 벡터를 평균내어
     * 사용자의 취향을 대표하는 벡터를 생성하는 데 사용됩니다.</p>
     *
     * @param vectors 벡터 배열
     * @return 평균 벡터
     * @throws IllegalArgumentException 벡터 배열이 null이거나 비어있는 경우, 또는 벡터 차원이 다른 경우
     */
    public static float[] averageVector(float[][] vectors) {
        if (vectors == null || vectors.length == 0) {
            throw new IllegalArgumentException("Vectors cannot be null or empty");
        }

        int dimension = vectors[0].length;
        float[] result = new float[dimension];

        for (float[] vector : vectors) {
            if (vector.length != dimension) {
                throw new IllegalArgumentException("All vectors must have the same dimension");
            }
            for (int i = 0; i < dimension; i++) {
                result[i] += vector[i];
            }
        }

        // 평균 계산
        for (int i = 0; i < dimension; i++) {
            result[i] /= vectors.length;
        }

        return result;
    }
}
