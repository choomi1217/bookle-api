# 01. Backend 설계

Spring Boot 3.x + Java 21 (LTS) 기반 REST API 서버.

## 프로젝트 배치

> **TODO**: 기존 `api.youngmi.works` 를 서빙하는 Spring Boot 앱이 있다면 **모듈(`wordbook` 패키지)로 추가**한다. 없다면 `bookle/` 경로에서 **독립 Spring Boot 앱**을 새로 만든다.

두 경우 모두 아래 패키지 구조는 동일하다.

## 패키지 구조

```
com.bookle.wordbook/
├── config/
│   ├── SecurityConfig.java         # X-API-Key 필터 등록, CORS
│   └── RestClientConfig.java       # Claude API 용 RestClient Bean
├── controller/
│   ├── BookController.java
│   ├── WordController.java
│   └── AiController.java
├── domain/
│   ├── Book.java
│   ├── Word.java
│   └── WordStatus.java             # enum: NEW, LEARNING, DONE
├── dto/
│   ├── BookResponse.java
│   ├── WordRequest.java
│   ├── WordResponse.java
│   ├── WordStatusUpdateRequest.java
│   ├── AiWordRequest.java
│   ├── AiWordResponse.java
│   ├── AiSentenceRequest.java
│   └── AiSentenceResponse.java
├── repository/
│   ├── BookRepository.java
│   └── WordRepository.java
└── service/
    ├── BookService.java
    ├── WordService.java
    └── ClaudeService.java
```

DTO는 Java `record` 로 선언해 간결하게 유지한다.

## DB 스키마

기존 RDS PostgreSQL에 아래 테이블 2개를 추가한다. 기존 테이블은 건드리지 않는다.

### `books`

```sql
CREATE TABLE books (
    id         BIGSERIAL    PRIMARY KEY,
    title      VARCHAR(200) NOT NULL,
    author     VARCHAR(100),
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

초기 투입 데이터는 `CLAUDE.md` §2의 5권 (Animal Farm / Despereaux / Hunger Games / Great Gatsby / Down and Out).

### `words`

```sql
CREATE TABLE words (
    id         BIGSERIAL    PRIMARY KEY,
    word       VARCHAR(100) NOT NULL,
    meaning    TEXT         NOT NULL,
    example    TEXT,
    status     VARCHAR(20)  NOT NULL DEFAULT 'NEW',  -- NEW / LEARNING / DONE
    book_id    BIGINT       REFERENCES books(id) ON DELETE SET NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_words_book_id ON words(book_id);
CREATE INDEX idx_words_status  ON words(status);
```

### 스키마 관리 방식

- 초기 개발: `src/main/resources/schema.sql` + `spring.jpa.hibernate.ddl-auto: validate`
- 운영 안정화 이후: Flyway 도입 고려 (v2 이상)

## API 경로 설계

모든 엔드포인트는 `/wordbook/*` prefix 를 사용한다. CloudFront/ALB 는 해당 prefix 를 이 서비스로 라우팅한다.

전체 endpoint 목록은 [04-api.md](./04-api.md) 참고.

## 설정 파일 (`application.yml`)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
    open-in-view: false

anthropic:
  api-key: ${ANTHROPIC_API_KEY}
  model: claude-sonnet-4-5          # 모델 버전은 00-overview.md 확인 필요 항목 참조
  max-tokens: 500

wordbook:
  api-key: ${WORDBOOK_API_KEY}      # 모바일 앱이 X-API-Key 로 전송
  cors-allowed-origins: "*"         # 앱만 호출 → 단순화
```

## 인증: 정적 API Key

개인 단일 사용자이지만 공개 인터넷에 노출되므로 **필터 레벨 정적 키 검증** 을 둔다.

- 요청 헤더 `X-API-Key` 값이 `wordbook.api-key` 와 일치하지 않으면 `401`.
- 검증 대상: `/wordbook/**` 전체.
- 키는 EC2 환경변수(`WORDBOOK_API_KEY`)로 주입. 앱은 빌드 시 `.env` → `expo-constants` 로 주입 후 `expo-secure-store` 에 저장.
- 키 노출 시 환경변수만 교체 후 앱 재빌드하면 무효화 가능.

> 개인용이라도 `meaning` / `example` 필드를 Claude API 호출로 생성하므로, 외부 남용 시 **청구 비용 폭증**이 실제 위협이다.

## Claude API 연동

### `ClaudeService` 핵심 동작

```java
public record AiWord(String meaning, String example) {}
public record AiSentence(String translation) {}

@Service
public class ClaudeService {
    private final RestClient restClient;      // Spring 6.1+ RestClient
    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public AiWord generateWordMeaning(String word) {
        String sys = "단어를 받으면 JSON으로만 응답하세요: " +
                     "{\"meaning\":\"한국어 뜻\",\"example\":\"영어 예문\"}";
        return callClaude(sys, word, AiWord.class);
    }

    public AiSentence generateSentenceTranslation(String sentence) {
        String sys = "영어 문장을 받으면 JSON으로만 응답하세요: " +
                     "{\"translation\":\"한국어 해석\"}";
        return callClaude(sys, sentence, AiSentence.class);
    }
    // callClaude: POST https://api.anthropic.com/v1/messages
    //   headers: x-api-key, anthropic-version: 2023-06-01
    //   body: { model, max_tokens, system, messages: [{role:user, content}] }
    //   content[0].text → JSON 파싱
}
```

### 선택 사항

- 시스템 프롬프트 `cache_control: ephemeral` 지정으로 **프롬프트 캐싱** 적용 시 비용 절감.
- `RestTemplate` 대신 Spring 6.1 의 `RestClient` 사용 (Spring Boot 3.2+ 표준).
- 타임아웃: connect 3초 / read 30초. Claude 응답이 길어질 수 있어 read 는 넉넉히.

### 에러 처리 정책

- Claude 가 JSON 파싱 실패한 응답을 주면 → `502 Bad Gateway` + 로그 WARN.
- Claude API 4xx → 원인 로그 + `503 Service Unavailable` 로 래핑.
- Claude API 5xx / 타임아웃 → 1회 재시도, 이후 실패 시 `503`.

## CORS

RN 앱만 호출하면 CORS 자체는 불필요(모바일 네이티브 HTTP 클라이언트는 CORS 영향 없음). 다만 향후 웹에서 호출 가능성 대비해 `wordbook.cors-allowed-origins` 설정 기반으로 Spring 의 `CorsConfigurationSource` 를 구성한다. 초기값 `*` 유지.

## 로깅

- Claude 호출 시 `word` 또는 `sentence` 입력, 응답 소요 시간(ms), 토큰 수(응답 `usage` 필드) 를 INFO 로 남긴다. 비용 추적 및 이상 패턴 감지용.
- `logback-spring.xml` 은 Spring Boot 기본값 사용. 로테이션 설정은 v2 이상에서 추가.
