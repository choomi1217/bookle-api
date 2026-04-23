# Word 기능 v1

## 범위

- Reader 에서 탭한 단어를 단어장에 저장한다.
- 단어장 화면에서 책별 필터, 학습 상태 변경, 삭제, 직접 등록을 할 수 있다.
- 플래시카드·4지선다 퀴즈로 복습한다.

## 사용자 흐름

```
[하단 탭: WordBook]
  └─▶ WordBookScreen
         ├─ 책 필터 / 상태 필터
         ├─ 단어 목록
         │    └─ (아이템 탭) 상태 변경 / 삭제
         ├─ "단어 직접 등록" 모달 → POST /wordbook/ai/word → POST /wordbook/words
         └─ "복습 시작" 버튼

[하단 탭: Quiz]
  └─▶ QuizScreen
         ├─ 모드 선택: 플래시카드 | 4지선다
         ├─ 플래시카드: 앞면 영단어 → 탭하면 뒷면(뜻·예문)
         └─ 4지선다: 단어 1개 + 뜻 4개 (정답 1, 오답 3)
```

## 화면

### WordBookScreen
- 상단 필터 바: 책 선택 드롭다운, 상태 칩 (`전체 / NEW / LEARNING / DONE`).
- 목록: `<WordListItem>` — 단어, 한국어 뜻 한 줄, 상태 뱃지.
- 아이템 좌측 스와이프 → 삭제, 우측 스와이프 → 상태 변경 토글 (`NEW → LEARNING → DONE → NEW`).
- 하단 FAB "+" → `AddWordModal`.
- 상단 우측 "복습 시작" → `QuizScreen` 으로 이동.

### AddWordModal
- 입력: 영단어 1개.
- 제출 시:
  1. `POST /wordbook/ai/word` → 뜻·예문 자동 생성
  2. 결과 미리보기 → 사용자 확인
  3. 확인 시 `POST /wordbook/words` 로 저장

### QuizScreen
- 모드 전환 상단 탭. 기본은 플래시카드.
- 세션 대상 단어 = 현재 WordBookScreen 의 필터 결과 (또는 전체).
- 각 화면:

#### FlashCard
- 전면: 영단어 크게.
- 탭 / 스와이프 업 → 뒷면으로 플립 (뜻 + 예문).
- 뒷면에서 "아는 단어" / "모름" 버튼:
  - "아는 단어" → 상태 `LEARNING → DONE` 또는 `NEW → LEARNING` 승격
  - "모름" → 상태 그대로 유지 (필요 시 `DONE → LEARNING` 강등 옵션은 v2)
- 좌우 스와이프로 다음/이전 단어.

#### MultipleChoice (4지선다)
- 상단: 영단어.
- 하단: 뜻 4개 버튼. 정답 1 + 오답 3 (동일 필터 범위에서 랜덤 추출).
- 오답 선택 시 빨강 하이라이트 + 정답 공개, 정답 선택 시 초록 + 바로 다음 문제.
- 5문제 세트 후 결과 요약.

## API 매핑

| 동작 | Endpoint |
|---|---|
| 단어 목록 조회 | `GET /wordbook/words?bookId=&status=` |
| 단어 저장 | `POST /wordbook/words` |
| 단어 상태 변경 | `PATCH /wordbook/words/{id}/status` |
| 단어 삭제 | `DELETE /wordbook/words/{id}` |
| 직접 등록 뜻 생성 | `POST /wordbook/ai/word` |

> 4지선다 오답 생성은 v1 에서 **클라이언트에서 처리** 한다 (`GET /words` 전체 후 셔플). 전용 엔드포인트는 v2 에서 도입.

## 데이터 모델

### Entity: `Word`

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| word | String(100) | 영단어 (원형 보존) |
| meaning | Text | 한국어 뜻 |
| example | Text (nullable) | 영어 예문 |
| status | enum `WordStatus` | `NEW` / `LEARNING` / `DONE` |
| book | Book (nullable, FK) | 출처 책. 직접 등록 시 null |
| createdAt | Instant | 생성 시각 |

### WordStatus 전이 규칙 (v1)

```
NEW  ──"아는 단어"──▶ LEARNING  ──"아는 단어"──▶ DONE
      ◀────────────────────── (수동 상태 변경만 가능, 역방향 자동 전이 없음)
```

- WordBookScreen 스와이프 → 수동 순환 (`NEW → LEARNING → DONE → NEW`)
- Quiz → 정방향 승격만

### DTO

```java
public record WordResponse(
    Long id,
    String word,
    String meaning,
    String example,
    WordStatus status,
    Long bookId,
    Instant createdAt
) {}

public record WordRequest(String word, String meaning, String example, Long bookId) {}
public record WordStatusUpdateRequest(WordStatus status) {}
```

## 중복 단어 처리 (v1)

- 동일 `word` 저장 시 **중복 허용**. BE 에서 unique 제약 없음.
- 같은 단어를 여러 책에서 마주쳐 각각 저장할 수 있도록 열어둠.
- 중복 머지·대소문자 정규화 등은 v2 에서 다룬다.

## v1 에서 의도적으로 뺀 것

- 중복 단어 머지 / 대소문자 정규화
- 단어 발음 (TTS)
- SRS (간격 반복 학습) 알고리즘
- 학습 기록 / 통계
- 태그
- 퀴즈 오답 추적 및 집중 복습 모드
- 전용 퀴즈 엔드포인트

위 항목은 v2+ 에서 별도 파일로 다룬다.
