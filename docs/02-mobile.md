# 02. Mobile 앱 설계

React Native + Expo + TypeScript 기반 iOS/Android 앱.

## 왜 Expo managed workflow 인가

- **iOS + Android 단일 코드베이스** — 개인 프로젝트 규모에서 네이티브 두 번 작성은 비현실적.
- **EAS Build** — 로컬에 Xcode/Android Studio 설치·관리 없이 클라우드 빌드. macOS 없어도 iOS 빌드 가능.
- **Dev Client** — `expo-dev-client` 설치판을 실기기에 1회 설치하면, 이후 JS 변경은 Metro 번들러에서 **핫 리로드**. 매 변경마다 재빌드 불필요.
- **TestFlight 배포 경로** — `eas build --profile preview --platform ios` → `eas submit` → TestFlight. Android는 동일 플로우로 Play Internal Testing.
- 한국 기업 채택 사례 — 당근마켓, 쿠팡이츠, 번개장터, 직방, 야놀자 일부.

## 프로젝트 구조

```
mobile/
├── app.config.ts                  # Expo 설정 (앱명, 번들ID, 아이콘, env)
├── eas.json                       # EAS Build/Submit 프로파일
├── package.json
├── tsconfig.json
├── index.ts                       # 엔트리포인트
└── src/
    ├── App.tsx                    # 루트 컴포넌트, Provider 조립
    ├── navigation/
    │   ├── RootNavigator.tsx      # Bottom Tab: Reader / WordBook / Quiz
    │   └── ReaderStack.tsx        # 책 목록 → 리더
    ├── api/
    │   ├── client.ts              # axios 인스턴스 + X-API-Key 인터셉터
    │   ├── books.ts               # 책 관련 API 호출
    │   ├── words.ts               # 단어 관련 API 호출
    │   └── ai.ts                  # Claude AI 관련 API 호출
    ├── hooks/
    │   ├── useBooks.ts            # TanStack Query 래퍼
    │   ├── useWords.ts
    │   └── useAi.ts
    ├── screens/
    │   ├── BookListScreen.tsx
    │   ├── ReaderScreen.tsx
    │   ├── WordBookScreen.tsx
    │   └── QuizScreen.tsx
    ├── components/
    │   ├── reader/
    │   │   ├── TappableWord.tsx   # 단어 1개 (onPress)
    │   │   ├── TappableSentence.tsx # 문장 1개 (onLongPress)
    │   │   ├── WordMeaningModal.tsx
    │   │   └── SentenceTranslationBubble.tsx
    │   ├── wordbook/
    │   │   └── WordListItem.tsx
    │   └── quiz/
    │       ├── FlashCard.tsx
    │       └── MultipleChoice.tsx
    ├── store/
    │   └── settingsStore.ts       # Zustand: API base URL, 키 존재 여부
    ├── theme/
    │   └── tokens.ts              # 색상·간격·타이포 토큰
    └── utils/
        ├── tokenize.ts            # 문장/단어 분리
        └── storage.ts             # expo-secure-store 래퍼
```

## 라이브러리 확정 목록

| 용도 | 라이브러리 | 버전 지정 기준 |
|---|---|---|
| 프로젝트 | `expo` | 최신 SDK (프로젝트 생성 시점 기준) |
| 네비게이션 | `@react-navigation/native`, `@react-navigation/native-stack`, `@react-navigation/bottom-tabs` | 7.x |
| 서버 상태 | `@tanstack/react-query` | 5.x |
| 클라 상태 | `zustand` | 4.x |
| HTTP | `axios` | 1.x |
| 안전 저장소 | `expo-secure-store` | Expo SDK 매칭 |
| 개발 빌드 | `expo-dev-client` | Expo SDK 매칭 |
| 환경변수 | `expo-constants` | Expo SDK 매칭 |

## 주요 상호작용 구현 메모

### 단어 탭
- 책 원문을 문장 → 단어로 `tokenize` 해서 `<TappableWord>` 로 감싼다.
- `onPress` → `POST /wordbook/ai/word` 호출 → 결과 모달에 표시.
- 모달 하단 "단어장에 추가" → `POST /wordbook/words`.

### 문장 Long-press
- RN 에서는 브라우저 `selection` API 가 없다. **문장 단위 `<Pressable onLongPress>`** 로 대체.
- 사용자는 "문장 전체" 를 길게 누르는 형태로 조작.
- 500ms 이상 누르면 `POST /wordbook/ai/sentence` → 해석을 문장 하단에 인라인 표시.

### 전체 해석 버튼
- 상단 toolbar 버튼 → 현재 보이는 문장 N개에 대해 `/wordbook/ai/sentence` 를 **동시성 제한 3** 으로 병렬 호출.
- 각 문장별 로딩 인디케이터 + 완료 시 아래 말풍선 렌더.
- 토큰 비용 큼 → 페이지 단위로만 호출 (전체 책 번역은 불가).

### 플래시카드
- `react-native-reanimated` 의 3D rotate 또는 단순 opacity toggle 로 시작.
- 슬라이드 넘기기 → 카드 스택 방식.

### 4지선다 퀴즈
- 정답 단어 1개 + 같은 책 (또는 전체)에서 **랜덤 오답 3개** 를 BE 에서 받아온다.
- 클라이언트 단 랜덤 추출 대신 BE 에 `GET /wordbook/quiz/next` 같은 전용 엔드포인트 **v2 에서 추가 고려**.
- v1: `GET /wordbook/words` 전체 조회 후 클라이언트에서 정답 + 랜덤 3개 섞기.

## 환경변수 / 설정

`app.config.ts` 에서 `extra` 로 아래 주입:

```ts
extra: {
  apiBaseUrl: "https://api.youngmi.works/wordbook",
  wordbookApiKey: process.env.WORDBOOK_API_KEY, // EAS Secret
}
```

- `WORDBOOK_API_KEY` 는 EAS Secret (`eas secret:create`) 으로 관리. 저장소 커밋 금지.
- 런타임에는 `Constants.expoConfig.extra.wordbookApiKey` 로 읽고, `expo-secure-store` 에 캐싱.

## 빌드 프로파일 (`eas.json`)

```json
{
  "cli": { "version": ">= 5.0.0" },
  "build": {
    "development": {
      "developmentClient": true,
      "distribution": "internal"
    },
    "preview": {
      "distribution": "internal",
      "ios":     { "simulator": false },
      "android": { "buildType": "apk" }
    },
    "production": {
      "autoIncrement": true
    }
  },
  "submit": {
    "production": {
      "ios": { "ascAppId": "TODO" },
      "android": { "serviceAccountKeyPath": "./play-service-account.json" }
    }
  }
}
```

- `development` → Dev Client 설치용 (최초 1회)
- `preview` → TestFlight / Play Internal 용 (개발 중 실기 테스트)
- `production` → 스토어 제출용

## 개발 이터레이션 플로우

1. `eas build --profile development --platform ios` 1회 실행 → iPhone 에 Dev Client 설치.
2. 로컬에서 `npx expo start --dev-client` → iPhone에서 핫 리로드로 개발.
3. 실기에서 확인이 필요한 시점 (타인에게 공유, JS 외 네이티브 변경 등)에 `eas build --profile preview` → TestFlight 업로드.
4. 스토어 제출 시점에만 `production` 프로파일 사용.

## 접근성·기본 UX 가드레일

- 폰트 크기 시스템 설정 반영 (`allowFontScaling`).
- 다크모드 대응 (`useColorScheme`).
- Safe area 적용 (`react-native-safe-area-context`).
- 네트워크 오류 시 토스트 + 재시도 (TanStack Query `retry` 기본값 3).
