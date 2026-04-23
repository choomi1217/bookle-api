# 04. REST API 명세 v1

## 공통 규칙

- **Base URL**: `https://api.youngmi.works/wordbook`
- **Content-Type**: `application/json; charset=utf-8`
- **인증 헤더**: 모든 요청에 `X-API-Key: <WORDBOOK_API_KEY>` 필수.
  - 누락/불일치 → `401 Unauthorized`
- **에러 응답 공통 포맷**:
  ```json
  { "error": "사람이 읽는 메시지", "code": "BAD_REQUEST" }
  ```
- **타임스탬프**: ISO-8601 UTC (`"2026-04-23T10:15:30Z"`).

## 엔드포인트 요약

| Method | Path | 설명 |
|---|---|---|
| GET    | `/books` | 책 목록 조회 |
| GET    | `/books/{id}` | 책 단건 조회 |
| GET    | `/books/{id}/content` | 책 원문 조회 (문장 배열) |
| POST   | `/books` | 책 등록 (초기 seed 용. v1 에서는 운영에서 사용 안 함) |
| GET    | `/words` | 단어 목록 조회 (쿼리: `bookId`, `status`) |
| POST   | `/words` | 단어 저장 |
| PATCH  | `/words/{id}/status` | 학습 상태 변경 |
| DELETE | `/words/{id}` | 단어 삭제 |
| POST   | `/ai/word` | Claude 로 단어 뜻·예문 생성 |
| POST   | `/ai/sentence` | Claude 로 문장 해석 생성 |

## 상세

### `GET /books`

**Response 200**

```json
[
  { "id": 1, "title": "Animal Farm", "author": "George Orwell", "createdAt": "2026-04-23T01:00:00Z" }
]
```

### `GET /books/{id}`

**Response 200**

```json
{ "id": 1, "title": "Animal Farm", "author": "George Orwell", "createdAt": "2026-04-23T01:00:00Z" }
```

**404** — 해당 id 없음

### `GET /books/{id}/content`

책 원문을 문장 배열로 반환. 클라이언트는 각 문장을 `<TappableSentence>` 단위로 렌더하고, 내부에서 단어 단위로 쪼갠다.

**Response 200**

```json
{
  "id": 1,
  "title": "Animal Farm",
  "sentences": [
    "Mr. Jones, of the Manor Farm, had locked the hen-houses for the night.",
    "With the ring of light from his lantern dancing from side to side, he lurched across the yard."
  ]
}
```

> 책 원문은 서버의 `src/main/resources/books/{id}.txt` 에서 로드된다. 문장 분할은 서버에서 단순 정규식 기준 (`[.!?]\s+`) 으로 처리. 정확한 문장 분할 개선은 v2 과제.

### `GET /words?bookId={id}&status={NEW|LEARNING|DONE}`

**Query Parameters** (모두 선택)

| 이름 | 타입 | 설명 |
|---|---|---|
| `bookId` | number | 해당 책에서 저장된 단어만 필터 |
| `status` | string | 학습 상태 필터 |

**Response 200**

```json
[
  {
    "id": 42,
    "word": "lurched",
    "meaning": "비틀거리다, 휘청거리다",
    "example": "He lurched across the yard.",
    "status": "NEW",
    "bookId": 1,
    "createdAt": "2026-04-23T02:11:00Z"
  }
]
```

### `POST /words`

**Request**

```json
{
  "word": "lurched",
  "meaning": "비틀거리다, 휘청거리다",
  "example": "He lurched across the yard.",
  "bookId": 1
}
```

- `meaning`, `example` 은 보통 `POST /ai/word` 의 응답을 그대로 넣는다.
- `bookId` 는 null 허용 (책과 무관한 직접 등록).

**Response 201**

```json
{
  "id": 42,
  "word": "lurched",
  "meaning": "비틀거리다, 휘청거리다",
  "example": "He lurched across the yard.",
  "status": "NEW",
  "bookId": 1,
  "createdAt": "2026-04-23T02:11:00Z"
}
```

### `PATCH /words/{id}/status`

**Request**

```json
{ "status": "LEARNING" }
```

- 허용 값: `NEW`, `LEARNING`, `DONE`.

**Response 200**

```json
{ "id": 42, "status": "LEARNING" }
```

### `DELETE /words/{id}`

**Response 204** — 본문 없음

### `POST /ai/word`

**Request**

```json
{ "word": "lurched" }
```

**Response 200**

```json
{ "meaning": "비틀거리다, 휘청거리다", "example": "He lurched across the yard." }
```

**502** — Claude 응답이 JSON 형식을 위반한 경우
**503** — Claude 호출 실패/타임아웃

### `POST /ai/sentence`

**Request**

```json
{ "sentence": "He lurched across the yard." }
```

**Response 200**

```json
{ "translation": "그는 마당을 비틀거리며 건너갔다." }
```

## 향후 확장 (v2+ 후보)

- `GET /wordbook/quiz/next` — 정답 1 + 오답 3 번들 응답 (클라이언트 랜덤 뽑기 대체)
- `POST /wordbook/ai/sentences/batch` — 문장 배열 일괄 해석 (토큰 효율·응답 속도)
- `GET /wordbook/books/{id}/pages/{n}` — 페이지 단위 원문 조회
