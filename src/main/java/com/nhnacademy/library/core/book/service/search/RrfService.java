package com.nhnacademy.library.core.book.service.search;

import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RRF (Reciprocal Rank Fusion) 서비스
 */
@Slf4j
@Service
public class RrfService {

    private static final int RRF_K = 60;

    /**
     * 키워드 검색 결과와 벡터 검색 결과를 RRF 알고리즘으로 병합합니다.
     *
     * @param keywordResults 키워드 검색 결과 리스트
     * @param vectorResults  벡터 검색 결과 리스트
     * @return RRF 점수가 적용된 병합된 도서 리스트 (점수 내림차순 정렬)
     */
    public List<BookSearchResponse> fuse(List<BookSearchResponse> keywordResults, List<BookSearchResponse> vectorResults) {
        Map<Long, Double> rrfScores = new HashMap<>();
        Map<Long, BookSearchResponse> bookMap = new HashMap<>();

        // 1. 키워드 결과 처리
        for (int i = 0; i < keywordResults.size(); i++) {
            BookSearchResponse b = keywordResults.get(i);
            rrfScores.put(b.getId(), rrfScores.getOrDefault(b.getId(), 0.0) + 1.0 / (RRF_K + i + 1));
            bookMap.put(b.getId(), b);
        }

        // 2. 벡터 결과 처리
        for (int i = 0; i < vectorResults.size(); i++) {
            BookSearchResponse b = vectorResults.get(i);
            rrfScores.put(b.getId(), rrfScores.getOrDefault(b.getId(), 0.0) + 1.0 / (RRF_K + i + 1));
            if (!bookMap.containsKey(b.getId())) {
                bookMap.put(b.getId(), b);
            } else {
                // 키워드 결과에 이미 있는 경우 유사도 정보 업데이트
                BookSearchResponse existing = bookMap.get(b.getId());
                bookMap.put(b.getId(), new BookSearchResponse(
                        existing.getId(), existing.getIsbn(), existing.getTitle(), existing.getVolumeTitle(),
                        existing.getAuthorName(), existing.getPublisherName(), existing.getPrice(),
                        existing.getEditionPublishDate(), existing.getImageUrl(), existing.getBookContent(), b.getSimilarity()
                ));
            }
        }

        // 3. 점수 기준 정렬 및 DTO 생성
        return rrfScores.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .map(entry -> {
                    Long id = entry.getKey();
                    BookSearchResponse original = bookMap.get(id);
                    Double rrfScore = entry.getValue();
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
