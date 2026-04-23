# 00. 전체 개요

## 프로젝트 목표

영어 원서를 읽으며 모르는 단어·문장을 Claude API로 즉시 해석하고, 저장한 단어를 플래시카드·퀴즈로 복습하는 **개인용 영어 학습 모바일 앱**.

- **사용 주체**: 단일 사용자 (인증·멀티테넌시 불필요. 다만 공개 인터넷 노출이므로 정적 API Key로 최소 보호)
- **유통 채널**: iOS (TestFlight → App Store) + Android (Google Play Internal Testing → Play Store)
- **기존 인프라 재사용**: AWS EC2 + RDS PostgreSQL + CloudFront + 기존 도메인 `api.youngmi.works`

## 기술 스택 결정 사항

### Backend

| 레이어 | 기술 | 선정 이유 |
|---|---|---|
| Framework | Spring Boot 3.x | 국내 IT 기업 주력 서버 스택 (네이버·카카오·쿠팡·토스·배민·당근 등) |
| Language | Java 21 (LTS) | 현행 LTS. 토스·쿠팡 등 국내 기업도 이동 중. Spring Boot 3.x 완벽 지원 |
| ORM | Spring Data JPA | 러닝커브 낮고 Spring 기본 조합 |
| DB | PostgreSQL (기존 RDS) | 기존 인프라 재사용 |
| AI | Claude API (Anthropic) | 필수 요구사항 |
| Build | Gradle | Spring 프로젝트 표준 |

### Mobile

| 레이어 | 기술 | 선정 이유 |
|---|---|---|
| Framework | React Native (Expo managed workflow) | 당근마켓·쿠팡이츠·번개장터·직방·야놀자 등 국내 채택. iOS + Android 동시 개발에 현실적 |
| Language | TypeScript | 국내 프런트엔드 채용 사실상 필수 |
| Navigation | React Navigation | RN 사실상 표준 |
| Server State | TanStack Query (React Query v5) | 캐싱·재시도·로딩 상태 관리 표준 |
| Client State | Zustand | 가볍고 러닝커브 낮음 |
| HTTP | axios | 인터셉터로 API Key 헤더 주입 간편 |
| Secure Storage | expo-secure-store | API Key, 설정값 저장 |

### 스택 선택에서 고려한 대안

- **네이티브(Swift + Kotlin)**: 네이버·카카오·토스 플래그십 앱이 채택. 성능·UX 최상이지만 **iOS + Android 두 번 개발**이라 개인 프로젝트 규모에서는 비현실적.
- **Flutter**: 국내에서 네이버웹툰·배민 일부 채택. RN 대비 국내 채용·사례가 적어 제외.
- **Expo bare workflow**: 커스텀 네이티브 모듈 필요 시 선택. 현 요구사항에서는 managed로 충분.

## 아키텍처 개요

```
┌────────────────────────────────┐
│  React Native (iOS / Android)  │
│  - Reader / WordBook / Quiz    │
└───────────────┬────────────────┘
                │ HTTPS + X-API-Key
                ▼
┌────────────────────────────────┐
│  CloudFront                    │
│  api.youngmi.works/wordbook/*  │
└───────────────┬────────────────┘
                ▼
┌────────────────────────────────┐
│  EC2 : Spring Boot             │
│  (WordBook 모듈)               │
└──────┬──────────────┬──────────┘
       ▼              ▼
┌────────────┐  ┌──────────────┐
│  RDS       │  │ Claude API   │
│ PostgreSQL │  │ (Anthropic)  │
└────────────┘  └──────────────┘
```

## 개발 로드맵

| 단계 | 목표 | 완료 기준 |
|---|---|---|
| 1 | 문서화 | `docs/*` 초안 리뷰 완료 |
| 2 | BE 뼈대 | Spring Boot 프로젝트 + `books`·`words` DDL + Book/Word CRUD 로컬 동작 |
| 3 | Claude API 연동 | `ClaudeService` + `/wordbook/ai/word`·`/wordbook/ai/sentence` 동작 |
| 4 | BE 배포 검증 | `https://api.youngmi.works/wordbook/...` 외부에서 200 응답 확인 |
| 5 | 모바일 뼈대 | Expo 프로젝트 + Dev Client 실기 설치 + 배포 빌드로 TestFlight 업로드 1회 성공 |
| 6 | Reader 화면 | 단어 탭 → 뜻·예문 모달, 문장 long-press → 해석, 전체 해석 버튼 동작 |
| 7 | WordBook 화면 | 책별 필터, 단어 목록, 상태 변경, 삭제, 직접 등록 |
| 8 | Quiz 화면 | 플래시카드 플립, 4지선다 동작 |
| 9 | 안정화 | 책 원문 5권 투입, 버그 수정, UI 다듬기, App Store/Play Store 제출 준비 |

## 확인 필요 항목 (미확정)

> **TODO**: 아래 항목은 사용자 확인 후 관련 문서에 반영.
>
> 1. **기존 `api.youngmi.works` 운용 중인 앱**: 같은 EC2 Spring Boot 앱에 모듈로 추가할지, 별도 프로세스/포트로 띄울지.
> 2. **기존 앱의 경로 prefix**: `/wordbook/*` 가 기존 라우트와 충돌하지 않는지.
> 3. **기존 RDS 접근**: 같은 DB 스키마에 `books`·`words` 테이블 추가 가능한지, 별도 스키마가 필요한지.
> 4. **Claude 모델 버전**: CLAUDE.md에는 `claude-sonnet-4-20250514` 로 적혀 있으나, 현재 최신 권장은 `claude-sonnet-4-5`. 비용·성능 확인 후 확정.
> 5. **Apple Developer Program / Google Play Developer 계정**: 가입 여부. 미가입이면 5단계 진입 전에 가입 필요 (각각 $99/년, $25 1회).
