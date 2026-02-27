package com.nhnacademy.library.core.book.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * VectorUtils 단위 테스트
 *
 * <p>코사인 유사도와 평균 벡터 계산을 검증합니다.</p>
 */
@DisplayName("VectorUtils 단위 테스트")
class VectorUtilsTest {

    @Test
    @DisplayName("같은 벡터의 코사인 유사도는 1.0이어야 한다")
    void sameVectorCosineSimilarity() {
        // Given
        float[] vector = {1.0f, 2.0f, 3.0f};

        // When
        double similarity = VectorUtils.calculateCosineSimilarity(vector, vector);

        // Then
        assertThat(similarity).isEqualTo(1.0);
    }

    @Test
    @DisplayName("반대 벡터의 코사인 유사도는 -1.0이어야 한다")
    void oppositeVectorCosineSimilarity() {
        // Given
        float[] vector1 = {1.0f, 2.0f, 3.0f};
        float[] vector2 = {-1.0f, -2.0f, -3.0f};

        // When
        double similarity = VectorUtils.calculateCosineSimilarity(vector1, vector2);

        // Then
        assertThat(similarity).isEqualTo(-1.0);
    }

    @Test
    @DisplayName("직교 벡터의 코사인 유사도는 0.0이어야 한다")
    void orthogonalVectorCosineSimilarity() {
        // Given
        float[] vector1 = {1.0f, 0.0f, 0.0f};
        float[] vector2 = {0.0f, 1.0f, 0.0f};

        // When
        double similarity = VectorUtils.calculateCosineSimilarity(vector1, vector2);

        // Then
        assertThat(similarity).isEqualTo(0.0);
    }

    @Test
    @DisplayName("영 벡터와의 코사인 유사도는 0.0이어야 한다")
    void zeroVectorCosineSimilarity() {
        // Given
        float[] vector1 = {1.0f, 2.0f, 3.0f};
        float[] vector2 = {0.0f, 0.0f, 0.0f};

        // When
        double similarity = VectorUtils.calculateCosineSimilarity(vector1, vector2);

        // Then
        assertThat(similarity).isEqualTo(0.0);
    }

    @ParameterizedTest
    @MethodSource("nullVectorProvider")
    @DisplayName("null 벡터 입력 시 0.0을 반환해야 한다")
    void nullVectorReturnsZero(float[] vector1, float[] vector2) {
        // When
        double similarity = VectorUtils.calculateCosineSimilarity(vector1, vector2);

        // Then
        assertThat(similarity).isEqualTo(0.0);
    }

    static Stream<Object[]> nullVectorProvider() {
        return Stream.of(
            new Object[]{null, new float[]{1.0f, 2.0f}},
            new Object[]{new float[]{1.0f, 2.0f}, null},
            new Object[]{null, null}
        );
    }

    @Test
    @DisplayName("차원이 다른 벡터 간 코사인 유사도 계산 시 0.0을 반환해야 한다")
    void differentDimensionsReturnsZero() {
        // Given
        float[] vector1 = {1.0f, 2.0f, 3.0f};
        float[] vector2 = {1.0f, 2.0f};

        // When
        double similarity = VectorUtils.calculateCosineSimilarity(vector1, vector2);

        // Then
        assertThat(similarity).isEqualTo(0.0);
    }

    @Test
    @DisplayName("평균 벡터를 올바르게 계산해야 한다")
    void averageVectorCalculation() {
        // Given
        float[][] vectors = {
            {1.0f, 2.0f, 3.0f},
            {3.0f, 4.0f, 5.0f},
            {5.0f, 6.0f, 7.0f}
        };

        // When
        float[] average = VectorUtils.averageVector(vectors);

        // Then (1+3+5)/3=3, (2+4+6)/3=4, (3+5+7)/3=5
        assertThat(average).isEqualTo(new float[]{3.0f, 4.0f, 5.0f});
    }

    @Test
    @DisplayName("단일 벡터의 평균은 자기 자신이어야 한다")
    void singleVectorAverage() {
        // Given
        float[][] vectors = {{1.0f, 2.0f, 3.0f}};

        // When
        float[] average = VectorUtils.averageVector(vectors);

        // Then
        assertThat(average).isEqualTo(new float[]{1.0f, 2.0f, 3.0f});
    }

    @ParameterizedTest
    @MethodSource("nullVectorsProvider")
    @DisplayName("null 또는 빈 벡터 배열 입력 시 예외가 발생해야 한다")
    void nullOrEmptyVectorsThrowsException(float[][] vectors) {
        // When & Then
        assertThatThrownBy(() -> VectorUtils.averageVector(vectors))
            .isInstanceOf(IllegalArgumentException.class);
    }

    static Stream<Object[]> nullVectorsProvider() {
        return Stream.of(
            new Object[]{null},
            new Object[]{new float[0][]}
        );
    }

    @Test
    @DisplayName("차원이 다른 벡터들의 평균 계산 시 예외가 발생해야 한다")
    void differentDimensionsAverageThrowsException() {
        // Given
        float[][] vectors = {
            {1.0f, 2.0f, 3.0f},
            {1.0f, 2.0f},
            {1.0f}
        };

        // When & Then
        assertThatThrownBy(() -> VectorUtils.averageVector(vectors))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("실제 임베딩 벡터 간 코사인 유사도를 계산할 수 있다")
    void realEmbeddingVectors() {
        // Given (실제 1024차원 임베딩의 일부라고 가정)
        float[] embedding1 = new float[1024];
        float[] embedding2 = new float[1024];

        // 첫 10차원만 설정 (나머지는 0)
        for (int i = 0; i < 10; i++) {
            embedding1[i] = 0.1f * i;
            embedding2[i] = 0.1f * (i + 1);
        }

        // When
        double similarity = VectorUtils.calculateCosineSimilarity(embedding1, embedding2);

        // Then (유사한 벡터이므로 높은 유사도)
        assertThat(similarity).isGreaterThan(0.9);
    }
}
