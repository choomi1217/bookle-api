# Book 기능 v1

## 범위

- 고정된 5권(CLAUDE.md §2)의 영문 원서를 읽을 수 있다.
- 읽는 도중 단어를 탭하거나 문장을 길게 눌러 Claude 로 해석을 받는다.
- 단어는 단어장에 저장 가능, 문장은 저장 없이 즉석 해석만.
- 책 등록/수정/삭제 UI 는 v1 범위 밖 (seed 데이터로 고정).

## 사용자 흐름

```
[앱 실행]
  └─▶ BookListScreen : 책 5권 그리드
         └─▶ (책 탭) ReaderScreen
                ├─ 단어 탭      → WordMeaningModal
                │    └─ "단어장에 추가" → POST /wordbook/words
                ├─ 문장 long-press → SentenceTranslationBubble (문장 하단 인라인)
                └─ "전체 해석" 버튼 → 현재 페이지 전 문장 일괄 해석
```

## 화면

### BookListScreen
- 5권을 2열 그리드로. 썸네일은 v1 에서 없이 타이틀/작가만.
- 탭 시 `ReaderScreen` 으로 네비게이트, `bookId` 파라미터 전달.

### ReaderScreen
- 상단 헤더: 책 제목, "전체 해석" 토글 버튼.
- 본문: `GET /wordbook/books/{id}/content` 응답의 `sentences[]` 를 순서대로 렌더.
- 문장 한 개당 `<TappableSentence>` 하나. 내부는 `tokenize(sentence)` 로 단어 분할 후 `<TappableWord>` 의 배열.
- 페이지네이션은 v1 에서 **스크롤 뷰 하나**로 단순 처리 (FlatList 최적화는 v2).

### WordMeaningModal
- 단어 탭 시 표시.
- 로딩 상태: 스피너 + "뜻을 가져오는 중…".
- 완료 상태: 영단어 / 한국어 뜻 / 예문 / "단어장에 추가" 버튼.
- 저장 완료 시 토스트 "단어장에 저장됨".

### SentenceTranslationBubble
- 문장 long-press 시 해당 문장 아래에 말풍선.
- 로딩·완료 상태 동일하게 처리.
- 한 번 표시된 해석은 해당 세션 동안 문장 아래 유지 (재탭 시 재호출 없음).

## API 매핑

| 동작 | Endpoint | 참고 |
|---|---|---|
| 책 목록 표시 | `GET /wordbook/books` | 책 5권 고정이지만 API 호출 |
| 책 원문 로드 | `GET /wordbook/books/{id}/content` | 앱 진입 시 1회 |
| 단어 해석 | `POST /wordbook/ai/word` | 단어 탭마다 호출 |
| 문장 해석 | `POST /wordbook/ai/sentence` | 문장 long-press 또는 전체 해석 |
| 단어 저장 | `POST /wordbook/words` | 모달 "단어장에 추가" |

## 데이터 모델

### Entity: `Book`

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| title | String | 책 제목 |
| author | String | 작가 |
| createdAt | Instant | 생성 시각 |

원문은 DB 가 아닌 `src/main/resources/books/{id}.txt` 에 보관 (v1 단순화). 서버 시작 시 `BookContentLoader` 가 메모리에 로드 → `GET /books/{id}/content` 응답 시 미리 로드된 캐시 사용.

### DTO

```java
public record BookResponse(Long id, String title, String author, Instant createdAt) {}
public record BookContentResponse(Long id, String title, List<String> sentences) {}
```

## 문장 분할 규칙 (v1)

서버에서 정규식 기반 단순 분할:

```
pattern = (?<=[.!?])\s+(?=[A-Z])
```

- 인용부 / 약어(Mr., Mrs.) 에서 오분할 가능. v1 수용 범위.
- 개선은 `Book 기능 v2` 에서 다룬다.

## 단어 분할 규칙 (v1)

클라이언트에서 처리:

```ts
function tokenize(sentence: string): Token[] {
  // /([A-Za-z']+)|(\s+)|([^A-Za-z'\s]+)/ 매칭
  // word / whitespace / punctuation 세 종류의 토큰으로 분리
}
```

- `word` 토큰만 `<TappableWord>` 로 렌더. 공백·구두점은 텍스트 그대로.
- 하이픈 포함 단어(`self-evident`)는 하나로 유지하지 않음 (v1 한계). v2 에서 개선.

## v1 에서 의도적으로 뺀 것

- 책 추가/삭제 UI
- 책 썸네일, 커버 이미지
- 읽기 진행률 저장 (북마크)
- 페이지 개념 / 페이지 단위 로딩
- 문장 해석 캐싱 (동일 문장 재요청 방지)
- 오프라인 지원

위 항목은 v2+ 에서 별도 파일로 다룬다.
