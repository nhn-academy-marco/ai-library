package com.nhnacademy.library.core.book.service.search;

import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RRF (Reciprocal Rank Fusion) 알고리즘을 사용하여 검색 결과를 병합하는 서비스입니다.
 * RRF는 여러 검색 엔진의 결과 순위를 통합하기 위해 사용되며, 수식은 다음과 같습니다:
 * RRFscore(d) = Σ (1 / (k + rank(d, r)))
 * 여기서 k는 상수(기본값 60)이며, rank(d, r)은 검색 엔진 r에서의 도서 d의 순위입니다.
 */
@Slf4j
@Service
public class RrfService {

    /** RRF 알고리즘의 랭킹 평활화를 위한 상수 k */
    private static final int RRF_K = 60;

    /**
     * 키워드 검색 결과와 벡터 검색 결과를 RRF 알고리즘으로 병합합니다.
     * 1. 각 도서별로 키워드 검색 순위와 벡터 검색 순위를 기반으로 RRF 점수를 누적 계산합니다.
     * 2. 병합 과정에서 벡터 검색의 유사도(Similarity) 정보를 도서 정보에 업데이트합니다.
     * 3. 최종 계산된 RRF 점수를 기준으로 내림차순 정렬하여 반환합니다.
     *
     * @param keywordResults 키워드 검색 결과 리스트
     * @param vectorResults  벡터 검색 결과 리스트
     * @return RRF 점수가 적용된 병합된 도서 리스트 (점수 내림차순 정렬)
     */
    public List<BookSearchResponse> fuse(List<BookSearchResponse> keywordResults, List<BookSearchResponse> vectorResults) {
        // 도서 ID별 누적 RRF 점수를 저장하는 맵
        Map<Long, Double> rrfScores = new HashMap<>();
        // 도서 ID별 상세 정보를 저장하는 맵 (최종 반환용)
        Map<Long, BookSearchResponse> bookMap = new HashMap<>();

        // 1. 키워드 검색 결과 처리 및 점수 계산
        for (int i = 0; i < keywordResults.size(); i++) {
            BookSearchResponse b = keywordResults.get(i);
            // 1 / (k + rank) 점수 누적
            rrfScores.put(b.getId(), rrfScores.getOrDefault(b.getId(), 0.0) + 1.0 / (RRF_K + i + 1));
            bookMap.put(b.getId(), b);
        }

        // 2. 벡터 검색 결과 처리 및 점수 계산
        for (int i = 0; i < vectorResults.size(); i++) {
            BookSearchResponse b = vectorResults.get(i);
            // 1 / (k + rank) 점수 누적
            rrfScores.put(b.getId(), rrfScores.getOrDefault(b.getId(), 0.0) + 1.0 / (RRF_K + i + 1));
            if (!bookMap.containsKey(b.getId())) {
                bookMap.put(b.getId(), b);
            } else {
                // 키워드 결과에 이미 존재하는 경우, 벡터 검색에서 추출된 유사도(Similarity) 정보를 보존하여 업데이트합니다.
                BookSearchResponse existing = bookMap.get(b.getId());
                bookMap.put(b.getId(), new BookSearchResponse(
                        existing.getId(), existing.getIsbn(), existing.getTitle(), existing.getVolumeTitle(),
                        existing.getAuthorName(), existing.getPublisherName(), existing.getPrice(),
                        existing.getEditionPublishDate(), existing.getImageUrl(), existing.getBookContent(), b.getSimilarity()
                ));
            }
        }

        // 3. 점수 기준 정렬 및 최종 응답 DTO 생성
        return rrfScores.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // 점수 내림차순 정렬
                .map(entry -> {
                    Long id = entry.getKey();
                    BookSearchResponse original = bookMap.get(id);
                    Double rrfScore = entry.getValue();
                    // 최종 필드에 RRF 점수를 포함하여 반환
                    return new BookSearchResponse(
                            original.getId(), original.getIsbn(), original.getTitle(), original.getVolumeTitle(),
                            original.getAuthorName(), original.getPublisherName(), original.getPrice(),
                            original.getEditionPublishDate(), original.getImageUrl(), original.getBookContent(), original.getSimilarity(),
                            rrfScore
                    );
                })
                .toList();
    }
}
