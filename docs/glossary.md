# 용어집

> 이 교재에서 자주 쓰는 용어를 한 줄로 정리했습니다. "처음 다루는 문서"는 그 개념을 처음 자세히 설명하거나 직접 구현하는 문서입니다. AI 기본 개념의 전체 지도는 [01. AI 백엔드 개발을 위한 기초 개념](step-0/01.ai-basics.md)에서 먼저 볼 수 있습니다.

영문 용어(A~Z)를 먼저, 한글 용어(가나다순)를 뒤에 둡니다. 한글과 영문으로 함께 쓰는 용어는 교재에서 주로 쓰는 표기 쪽에 둡니다.

## A ~ C

| 용어 | 정의 | 처음 다루는 문서 |
|---|---|---|
| A2A(Agent-to-Agent) | 여러 Agent가 서로 메시지를 주고받으며 협업하는 방식. 이 프로젝트는 구현하지 않고 이론·확장 방향으로만 다룹니다. | [step-7/00.theory.md](step-7/00.theory.md) |
| Advisor | Spring AI `ChatClient` 호출 앞뒤에 끼어들어 대화 기록 추가 같은 공통 처리를 하는 구성 요소(예: `PromptChatMemoryAdvisor`). | [step-7/05.tool-annotation.md](step-7/05.tool-annotation.md) |
| Agent(에이전트) | LLM이 목표를 이루기 위해 판단 → 도구 실행 → 결과 확인을 반복하는 구조. | [step-7/00.theory.md](step-7/00.theory.md) |
| B-Tree 인덱스 | 값의 크기 순서로 정렬해 두는 기본 인덱스. 정확히 일치하거나 앞부분이 같은 검색에 빠르지만 `LIKE '%자바%'` 같은 중간 일치에는 쓰이지 않습니다. | [step-1/04.search-optimization.md](step-1/04.search-optimization.md) |
| BGE-M3 | 이 프로젝트가 쓰는 다국어 임베딩 모델. 텍스트를 1024차원 벡터로 바꿉니다. | [step-2/03.embedding-generation.md](step-2/03.embedding-generation.md) |
| Caffeine | JVM 안에서 동작하는 로컬 캐시 라이브러리. 시맨틱 캐시와 개인화 캐시에 사용합니다. | [step-4/03.semantic-caching.md](step-4/03.semantic-caching.md) |
| CallbackQuery | Telegram에서 사용자가 메시지의 인라인 버튼을 눌렀을 때 Bot에 전달되는 이벤트. 피드백(좋아요/싫어요) 수집에 씁니다. | [step-5/04.feedback-ui-callback-handler.md](step-5/04.feedback-ui-callback-handler.md) |
| ChatClient | Spring AI의 대화용 Fluent API. 시스템 프롬프트·사용자 메시지 조립, 도구 등록, 응답 변환을 담당합니다. | [step-0/01.ai-basics.md](step-0/01.ai-basics.md) |
| ChatMemory | 이전 대화 내용을 저장해 다음 요청에 함께 보내도록 하는 Spring AI 구성 요소. | [step-7/05.tool-annotation.md](step-7/05.tool-annotation.md) |
| ChatModel | Spring AI에서 LLM 호출을 추상화한 인터페이스. Gemini와 Ollama 구현을 같은 방식으로 호출합니다. | [step-3/03.llm-integration.md](step-3/03.llm-integration.md) |

## D ~ L

| 용어 | 정의 | 처음 다루는 문서 |
|---|---|---|
| DLQ(Dead Letter Queue) | 여러 번 재시도해도 처리하지 못한 메시지를 따로 모아 두는 큐. 이 프로젝트는 3회 시도 후 DLQ로 보냅니다. | [step-4/04.review-summarization.md](step-4/04.review-summarization.md) |
| EmbeddingModel | Spring AI에서 임베딩 생성을 추상화한 인터페이스. `embed(텍스트)`로 `float[]`를 받습니다. | [step-2/03.embedding-generation.md](step-2/03.embedding-generation.md) |
| EXPLAIN ANALYZE | PostgreSQL이 쿼리를 어떻게 실행했는지(인덱스 사용 여부, 소요 시간)를 보여 주는 명령. | [step-1/04.search-optimization.md](step-1/04.search-optimization.md) |
| FTS(Full Text Search, 전문 검색) | 문서를 단어 단위로 분해해 색인하고 검색하는 방식. PostgreSQL은 `tsvector`와 GIN 인덱스로 지원합니다. | [step-1/04.search-optimization.md](step-1/04.search-optimization.md) |
| FunctionToolCallback | Spring AI에서 함수를 코드로 직접 정의해 도구로 등록하는 방식. 이후 `@Tool` 애노테이션 방식으로 전환합니다. | [step-7/02.single-function.md](step-7/02.single-function.md) |
| Gemini | Google의 LLM. 이 프로젝트는 `gemini-2.0-flash`를 사용합니다. | [step-0/02.environment-setup.md](step-0/02.environment-setup.md) |
| GIN 인덱스 | 하나의 값에 여러 키(단어 등)가 들어 있는 데이터를 위한 역색인. 전문 검색 속도를 높입니다. | [step-1/04.search-optimization.md](step-1/04.search-optimization.md) |
| HNSW | 벡터를 그래프로 연결해 가까운 벡터를 근사적으로 빠르게 찾는 인덱스 기법. pgvector가 지원합니다. | [step-2/01.search-quality-limitations.md](step-2/01.search-quality-limitations.md) |
| Inline Keyboard | Telegram 메시지 아래에 붙는 버튼 묶음. 누르면 CallbackQuery가 발생합니다. | [step-5/01.telegram-bot-setup.md](step-5/01.telegram-bot-setup.md) |
| LLM(Large Language Model) | 방대한 텍스트로 학습해 입력 다음에 올 내용을 토큰 단위로 생성하는 대규모 언어 모델. | [step-0/01.ai-basics.md](step-0/01.ai-basics.md) |
| Long Polling | Bot이 Telegram 서버에 "새 메시지가 있으면 알려 달라"고 요청을 걸어 두고 기다리는 수신 방식. Webhook과 대비됩니다. | [step-5/01.telegram-bot-setup.md](step-5/01.telegram-bot-setup.md) |

## M ~ R

| 용어 | 정의 | 처음 다루는 문서 |
|---|---|---|
| Map-Reduce 요약 | 긴 자료를 조각(청크)으로 나눠 각각 요약(Map)한 뒤, 부분 요약을 다시 합쳐 최종 요약(Reduce)하는 방식. 리뷰 요약에 씁니다. | [step-4/04.review-summarization.md](step-4/04.review-summarization.md) |
| MCP(Model Context Protocol) | 도구와 데이터를 여러 AI 클라이언트가 같은 방식으로 쓸 수 있게 정한 공개 통신 규약. Tool Calling을 외부와 공유하는 표준입니다. | [step-7/00.theory.md](step-7/00.theory.md) |
| n8n | 노드를 연결해 워크플로우를 만드는 자동화 도구. Spring AI API를 Slack 등 외부 서비스와 연결합니다. | [step-8/01.n8n-integration.md](step-8/01.n8n-integration.md) |
| Ollama | 내 컴퓨터나 서버에서 오픈 LLM을 실행하는 도구. 이 프로젝트는 `qwen2.5` 모델을 기본으로 사용합니다. | [step-0/02.environment-setup.md](step-0/02.environment-setup.md) |
| OpenAI 호환 엔드포인트 | OpenAI API와 같은 요청·응답 형식을 제공하는 서버. BGE-M3 임베딩 서버를 Spring AI OpenAI 스타터로 호출할 수 있는 이유입니다. | [step-2/03.embedding-generation.md](step-2/03.embedding-generation.md) |
| pgvector | PostgreSQL에 `vector` 타입과 거리 연산자(`<=>` 등)를 추가하는 확장. | [step-2/02.pgvector-setup.md](step-2/02.pgvector-setup.md) |
| QueryDSL | Java 코드로 타입 안전하게 SQL·JPQL을 작성하는 라이브러리. 조건이 바뀌는 동적 쿼리에 씁니다. | [step-1/02.basic-search-implementation.md](step-1/02.basic-search-implementation.md) |
| RabbitMQ | 메시지 큐 서버. 리뷰 요약 요청을 비동기로 처리하는 데 씁니다. | [step-4/04.review-summarization.md](step-4/04.review-summarization.md) |
| RAG(Retrieval-Augmented Generation, 검색 증강 생성) | 관련 자료를 먼저 검색해 프롬프트의 컨텍스트로 넣고 LLM이 그 자료를 근거로 답하게 하는 방식. | [step-3/01.understanding-rag.md](step-3/01.understanding-rag.md) |
| RRF(Reciprocal Rank Fusion) | 여러 검색 결과의 순위를 `1 / (k + 순위)` 점수로 합쳐 하나의 순위로 만드는 방법. 이 프로젝트는 k=60을 씁니다. | [step-2/05.hybrid-search.md](step-2/05.hybrid-search.md) |

## S ~ Z

| 용어 | 정의 | 처음 다루는 문서 |
|---|---|---|
| Spring AI | 여러 LLM·임베딩 모델을 같은 인터페이스로 다루게 해 주는 Spring 프로젝트. 이 교재는 1.1.2를 사용합니다. | [step-0/01.ai-basics.md](step-0/01.ai-basics.md) |
| Telegram Bot | Telegram 메신저에서 메시지를 주고받는 프로그램. 대화형 검색 UI와 피드백 수집에 씁니다. | [step-5/01.telegram-bot-setup.md](step-5/01.telegram-bot-setup.md) |
| Temperature | LLM이 다음 토큰을 얼마나 무작위로 고를지 정하는 값. 낮을수록 일관되고, 높을수록 다양하지만 환각 위험이 큽니다. 이 프로젝트는 0.3입니다. | [step-0/01.ai-basics.md](step-0/01.ai-basics.md) |
| Tool Calling(Function Calling) | LLM에 사용할 수 있는 도구 목록을 알려 주고, LLM이 필요한 도구 호출을 요청하면 애플리케이션이 실행해 결과를 돌려주는 방식. | [step-7/00.theory.md](step-7/00.theory.md) |
| Tool 기반 오케스트레이션 | 하나의 LLM이 등록된 여러 Tool을 스스로 골라 조합해 복잡한 요청을 처리하는 이 프로젝트의 방식(`ToolBasedAiOrchestrationService`). | [step-7/01.overview.md](step-7/01.overview.md) |
| `@Tool` / `@ToolParam` | 메서드와 파라미터에 설명을 붙여 Spring AI 도구로 등록하는 애노테이션. LLM은 이 설명을 보고 도구를 고릅니다. | [step-7/05.tool-annotation.md](step-7/05.tool-annotation.md) |
| Top-K | 검색 결과 중 상위 K건만 사용하는 것. RAG에서는 컨텍스트에 넣을 문서 수를 정하며 비용과 품질에 직접 영향을 줍니다. | [step-4/02.topk-optimization.md](step-4/02.topk-optimization.md) |
| TTL(Time To Live) | 캐시 항목이 유효한 시간. 시맨틱 캐시는 30분입니다. | [step-4/03.semantic-caching.md](step-4/03.semantic-caching.md) |
| tsvector | PostgreSQL 전문 검색용 데이터 타입. 문서를 검색 가능한 단어(토큰) 목록으로 바꿔 저장합니다. | [step-1/04.search-optimization.md](step-1/04.search-optimization.md) |
| Webhook | 이벤트가 생기면 외부 서비스가 우리 서버 주소로 HTTP 요청을 보내 알려 주는 방식. Long Polling과 대비됩니다. | [step-5/01.telegram-bot-setup.md](step-5/01.telegram-bot-setup.md) |

## ㄱ ~ ㅂ

| 용어 | 정의 | 처음 다루는 문서 |
|---|---|---|
| 개인화 랭킹 | 사용자의 선호 벡터와 도서 벡터의 유사도를 반영해 검색 결과 순서를 사용자마다 다르게 정하는 것. | [step-6/03.personalized-ranking.md](step-6/03.personalized-ranking.md) |
| 내적(Dot Product) | 두 벡터의 같은 위치 값을 곱해 모두 더한 값. 코사인 유사도 계산의 기초입니다. | [step-2/02.pgvector-setup.md](step-2/02.pgvector-setup.md) |
| 벡터(Vector) | 숫자를 순서대로 나열한 배열. 임베딩 결과로 얻으며, 숫자의 개수를 차원이라고 합니다. | [step-0/01.ai-basics.md](step-0/01.ai-basics.md) |
| 벡터 검색 | 질문 벡터와 가까운 문서 벡터를 찾는 검색. 의미 검색을 구현하는 방법입니다. | [step-2/04.natural-language-search.md](step-2/04.natural-language-search.md) |
| 벡터 데이터베이스(Vector Database) | 벡터를 저장하고 유사한 벡터를 빠르게 찾는 저장소. 이 프로젝트는 PostgreSQL + pgvector를 씁니다. | [step-0/01.ai-basics.md](step-0/01.ai-basics.md) |
| 비동기 웜업(Warm-up) | 캐시 미스일 때 먼저 빠른 결과를 응답하고, LLM 추천은 이벤트로 백그라운드에서 실행해 다음 요청을 위해 캐시를 미리 채우는 것. | [step-4/03.semantic-caching.md](step-4/03.semantic-caching.md) |

## ㅅ ~ ㅇ

| 용어 | 정의 | 처음 다루는 문서 |
|---|---|---|
| 사용자 선호 벡터 | 사용자가 좋아요를 누른 도서들의 임베딩을 평균 내 만든, 사용자 취향을 나타내는 벡터. | [step-6/02.user-preference-vector.md](step-6/02.user-preference-vector.md) |
| 시맨틱 캐시(Semantic Cache) | 질문 문자열이 아니라 질문 임베딩의 코사인 유사도로 비슷한 질문을 판별해 저장된 답을 재사용하는 캐시. 이 프로젝트는 유사도 0.98 이상을 적중으로 봅니다. | [step-4/03.semantic-caching.md](step-4/03.semantic-caching.md) |
| 시스템 프롬프트(System Prompt) | LLM에 역할, 규칙, 출력 형식을 알려 주는 고정 지시문. 사용자 메시지와 구분해 보냅니다. | [step-0/01.ai-basics.md](step-0/01.ai-basics.md) |
| 의미 검색(Semantic Search) | 글자가 아니라 뜻이 비슷한지를 기준으로 찾는 검색. 임베딩과 벡터 검색으로 구현합니다. | [step-2/01.search-quality-limitations.md](step-2/01.search-quality-limitations.md) |
| 이벤트 기반 아키텍처(EDA) | 구성 요소가 직접 호출하지 않고 이벤트를 발행·구독해 느슨하게 연결되는 구조. CSV 적재와 캐시 웜업에 씁니다. | [step-1/01.csv-batch-load.md](step-1/01.csv-batch-load.md) |
| 임계값(Threshold) | 결과를 채택할지 정하는 기준값. 예: RRF 점수 0.02 이상만 RAG 후보로 사용, 코사인 유사도 0.98 이상만 캐시 적중. | [step-4/02.topk-optimization.md](step-4/02.topk-optimization.md) |
| 임베딩(Embedding) | 텍스트를 의미를 담은 숫자 배열(벡터)로 바꾸는 일 또는 그 결과. 뜻이 비슷한 글은 가까운 벡터가 됩니다. | [step-0/01.ai-basics.md](step-0/01.ai-basics.md) |

## ㅈ ~ ㅎ

| 용어 | 정의 | 처음 다루는 문서 |
|---|---|---|
| 재현율(Recall) / 정밀도(Precision) | 재현율은 찾아야 할 결과 중 실제로 찾은 비율, 정밀도는 찾은 결과 중 맞는 결과의 비율. 검색 품질을 재는 지표입니다. | [step-2/01.search-quality-limitations.md](step-2/01.search-quality-limitations.md) |
| 적중률(Hit Rate) | 캐시 요청 중 저장된 값을 바로 돌려준 비율. 높을수록 LLM 호출 비용이 줄어듭니다. | [step-4/03.semantic-caching.md](step-4/03.semantic-caching.md) |
| 전략 패턴(Strategy Pattern) | 바뀔 수 있는 알고리즘을 인터페이스 뒤의 구현체로 분리하는 설계. 검색 방식(Keyword/Vector/Hybrid/RAG)을 `SearchStrategy`로 나눕니다. | [step-2/04.natural-language-search.md](step-2/04.natural-language-search.md) |
| 차원(Dimension) | 벡터를 이루는 숫자의 개수. BGE-M3 임베딩은 1024차원이며 DB 컬럼은 `vector(1024)`입니다. | [step-2/03.embedding-generation.md](step-2/03.embedding-generation.md) |
| 청크(Chunk) | 큰 데이터를 처리하기 좋게 나눈 조각. 배치 적재 단위나 Map-Reduce 요약의 입력 단위로 씁니다. | [step-4/04.review-summarization.md](step-4/04.review-summarization.md) |
| 컨텍스트(Context) | 프롬프트에 함께 넣어 LLM이 답의 근거로 삼게 하는 자료. RAG에서는 검색된 도서 정보입니다. | [step-3/02.context-construction.md](step-3/02.context-construction.md) |
| 컨텍스트 윈도우(Context Window) | LLM이 한 번에 받아들일 수 있는 최대 토큰 수. | [step-0/01.ai-basics.md](step-0/01.ai-basics.md) |
| 코사인 유사도(Cosine Similarity) | 두 벡터가 같은 방향을 가리키는 정도. 1에 가까울수록 의미가 비슷합니다. pgvector에서는 `1 - (a <=> b)`로 구합니다. | [step-0/01.ai-basics.md](step-0/01.ai-basics.md) |
| 콜드 스타트(Cold Start) | 피드백이 거의 없는 신규 사용자처럼 개인화에 쓸 데이터가 부족한 상황. 이 프로젝트는 좋아요 피드백이 3건 미만이면 선호 벡터를 만들지 않습니다. | [step-6/04.cold-start-and-optimization.md](step-6/04.cold-start-and-optimization.md) |
| 토큰(Token) | LLM이 텍스트를 읽고 쓰는 최소 단위 조각. 비용, 속도, 길이 제한이 토큰 수로 정해집니다. | [step-0/01.ai-basics.md](step-0/01.ai-basics.md) |
| 프롬프트(Prompt) | LLM에 보내는 입력 전체. 시스템 프롬프트와 사용자 메시지로 구성합니다. | [step-0/01.ai-basics.md](step-0/01.ai-basics.md) |
| 프롬프트 엔지니어링(Prompt Engineering) | 원하는 형식과 품질의 답을 얻도록 프롬프트의 지시, 예시, 출력 형식을 설계하는 일. | [step-3/02.context-construction.md](step-3/02.context-construction.md) |
| 피드백 루프(Feedback Loop) | 사용자의 반응(좋아요/싫어요)을 수집해 다시 검색·추천 품질 개선에 반영하는 순환 구조. | [step-5/05.feedback-search-integration.md](step-5/05.feedback-search-integration.md) |
| 하이브리드 검색(Hybrid Search) | 키워드 검색과 벡터 검색 결과를 RRF로 합쳐 서로의 약점을 보완하는 검색. | [step-2/05.hybrid-search.md](step-2/05.hybrid-search.md) |
| 환각(Hallucination) | LLM이 사실이 아닌 내용을 그럴듯하게 생성하는 현상. RAG와 프롬프트 규칙으로 줄입니다. | [step-0/01.ai-basics.md](step-0/01.ai-basics.md) |
