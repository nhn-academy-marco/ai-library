package com.nhnacademy.library.core.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

/**
 * PostgreSQL 데이터베이스의 커스텀 함수를 등록하는 컨트리뷰터 클래스입니다.
 * PostgreSQL의 전문 검색 기능을 사용하여 한국어 검색을 지원하기 위한 기능을 추가합니다.
 */
public class PostgreSQLFunctionContributor implements FunctionContributor {
    /**
     * PostgreSQL에 'ts_match_korean' 함수 패턴을 등록합니다.
     * 이 함수는 PostgreSQL의 to_tsvector와 plainto_tsquery를 사용하여
     * 한국어 사전('korean')을 기반으로 전문 검색 기능을 제공합니다.
     *
     * @param functionContributions 커스텀 함수를 등록하는데 사용되는 함수 컨트리뷰션 객체
     */
    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry()
                .registerPattern("ts_match_korean", 
                        "to_tsvector('korean', ?1) @@ plainto_tsquery('korean', ?2)",
                        functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN));

        functionContributions.getFunctionRegistry()
                .registerPattern("vector_cosine_similarity",
                        "(1.0 - (embedding <=> cast(?1 as vector)))",
                        functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.DOUBLE));
    }
}
