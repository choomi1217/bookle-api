# WordBook App
### Spring Boot + PostgreSQL (기존 RDS) + AWS EC2 프로젝트 설계 문서 v1.2

---

## 1. 프로젝트 개요

영어 원서를 읽으면서 모르는 단어/문장을 저장하고, 플래시카드·퀴즈로 복습하는 개인 영단어 학습 웹앱입니다.
**기존 AWS EC2 + RDS PostgreSQL 환경에 신규 모듈로 추가합니다.**

---

## 2. 지원 도서 목록

> 모두 [Project Gutenberg Australia](https://gutenberg.net.au) 에서 무료로 제공되는 퍼블릭 도메인 영문 원서입니다.

| 책 제목 | 작가 | 난이도 | 비고 |
|---|---|---|---|
| Animal Farm | George Orwell | ★★★☆☆ | 우화·풍자, 1945년작 |
| The Tale of Despereaux | Kate DiCamillo | ★★☆☆☆ | 입문용, 오디오북 있음 |
| The Hunger Games | Suzanne Collins | ★★★☆☆ | 현대 문체, 1인칭 |
| The Great Gatsby | F. Scott Fitzgerald | ★★★★☆ | 미국 문학 고전, 1925년작 |
| Down and Out in Paris and London | George Orwell | ★★★☆☆ | 오웰 논픽션, Animal Farm과 문체 유사 |

> 추후 books 테이블에 `gutenberg_url` 컬럼을 추가하면 원문 출처 링크도 관리할 수 있습니다.

---

## 3. 핵심 기능

### 📚 책 기능

| 기능 | 설명 |
|---|---|
| 단어 탭 → 뜻·예문 자동 생성 | 책 읽다가 모르는 단어 탭 → Claude API가 뜻·예문 즉시 생성, 단어장에 등록 가능 |
| 문장 길게 드래그 → 문장 해석 | 문장 선택 후 길게 누르면 Claude API가 한국어 해석 생성 |
| 전체 해석 보기 | 버튼 클릭 시 각 문장 하단에 해석을 일괄 생성하여 표시 |

### 📝 단어 기능

| 기능 | 설명 |
|---|---|
| 단어 직접 등록 | 단어 입력 → Claude API가 뜻·예문 자동 생성 후 저장 |
| 플래시카드 복습 | 앞면 영단어 → 탭하면 뒤집어서 뜻·예문 확인 |
| 4지선다 퀴즈 | 랜덤 단어를 제시하고 뜻 4지선다로 맞추기 |

---

## 4. 기술 스택

| 레이어 | 기술 | 비고 |
|---|---|---|
| Backend | Spring Boot 3.x | 기존 프로젝트에 모듈 추가 |
| Language | Java 17 | 기존과 동일 |
| ORM | Spring Data JPA | 기존과 동일 |
| DB | PostgreSQL (기존 RDS) | 기존 RDS에 테이블 추가 |
| AI | Claude API (claude-sonnet-4-20250514) | 신규 추가 |
| Frontend | Thymeleaf + Vanilla JS | 기존 방식 유지 |
| Infra | 기존 AWS EC2 + RDS | 신규 인프라 없음 |
| Build | Gradle | 기존과 동일 |

---

## 5. AWS 배포 전략
> 기존 EC2, RDS를 그대로 사용합니다. 추가 인프라 비용 없음.
