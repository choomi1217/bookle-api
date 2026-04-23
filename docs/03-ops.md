# 03. 운영 / 배포

## 현재 인프라 현황

| 구성요소 | 상태 |
|---|---|
| AWS EC2 | 기존 운영 중 |
| AWS RDS (PostgreSQL) | 기존 운영 중 |
| CloudFront | 기존 운영 중 |
| 도메인 `api.youngmi.works` | 기존 운영 중 (CloudFront 연결) |
| Apple Developer Program | 가입 여부 확인 필요 |
| Google Play Developer 계정 | 가입 여부 확인 필요 |

## API 도메인 배치

기존 `api.youngmi.works` 에 **경로 prefix `/wordbook/*`** 을 추가한다. 서브도메인 신설 없이 운용.

```
https://api.youngmi.works/wordbook/books
https://api.youngmi.works/wordbook/words
https://api.youngmi.works/wordbook/ai/word
https://api.youngmi.works/wordbook/ai/sentence
```

### 라우팅 설정

> **TODO**: 아래는 구성 형태에 따라 선택한다. 사용자 확인 필요.
>
> - (A) 기존 EC2 Spring Boot 앱에 `wordbook` 패키지를 모듈로 추가 → 별도 라우팅 없이 같은 프로세스에서 처리.
> - (B) 별도 Spring Boot 프로세스로 띄우는 경우 → nginx / ALB 에서 `/wordbook/*` 를 해당 포트로 리버스 프록시.
> - CloudFront Behavior 에서 `/wordbook/*` 경로에 대해 **캐싱 비활성** 지정 필수 (POST / 동적 응답).

### CloudFront 주의사항

- 기본 behavior 가 GET 만 허용하는 경우 → `/wordbook/*` behavior 에서 `POST`, `PATCH`, `DELETE`, `OPTIONS` 메서드 허용.
- `X-API-Key` 헤더가 origin 까지 전달되도록 **Origin Request Policy** 에서 해당 헤더 포함.
- 응답 캐시 TTL = 0.

## 환경변수 (EC2)

`/etc/environment` 또는 systemd `Environment=` 로 주입.

| 이름 | 설명 |
|---|---|
| `ANTHROPIC_API_KEY` | Claude API 호출 키 |
| `WORDBOOK_API_KEY` | 모바일 앱 → 서버 호출 시 `X-API-Key` 헤더 값 |
| `DB_HOST` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | RDS 접속 정보 |

> 두 API Key 는 저장소 커밋 금지. EC2 직접 주입.

## DB 초기화

1. 기존 RDS 접속.
2. `CREATE TABLE books ...`, `CREATE TABLE words ...` (DDL 은 [01-backend.md](./01-backend.md) 참고).
3. 책 5권 seed insert.

```sql
INSERT INTO books (title, author) VALUES
  ('Animal Farm', 'George Orwell'),
  ('The Tale of Despereaux', 'Kate DiCamillo'),
  ('The Hunger Games', 'Suzanne Collins'),
  ('The Great Gatsby', 'F. Scott Fitzgerald'),
  ('Down and Out in Paris and London', 'George Orwell');
```

책 원문 `.txt` 는 BE 의 `src/main/resources/books/{id}.txt` 에 투입. 서버 시작 시 메모리에 로드.

## 배포 스크립트 (BE)

`scripts/deploy.sh` (로컬 실행):

```bash
#!/usr/bin/env bash
set -euo pipefail

EC2_HOST="${EC2_HOST:?}"          # ubuntu@xxx.xxx.xxx.xxx
JAR_NAME="bookle.jar"             # 실제 빌드 결과물 이름에 맞춤

./gradlew clean bootJar
scp "build/libs/${JAR_NAME}" "${EC2_HOST}:~/app/${JAR_NAME}"
ssh "${EC2_HOST}" "sudo systemctl restart bookle"
```

systemd 유닛 파일은 아래와 같이 구성한다. (`/etc/systemd/system/bookle.service`)

```ini
[Unit]
Description=WordBook Spring Boot App
After=network.target

[Service]
EnvironmentFile=/etc/bookle.env
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod /home/ubuntu/app/bookle.jar
Restart=on-failure
User=ubuntu

[Install]
WantedBy=multi-user.target
```

> CLAUDE.md 는 `pkill -f java` 방식을 예시로 들었으나, 재시작 실패 시 다운타임·좀비 프로세스 위험이 있어 **systemd 권장**. 단, 기존 앱이 이미 `pkill` 기반으로 운영 중이면 기존 방식 유지.

## 모바일 빌드·배포

### Apple 쪽
- **Apple Developer Program** $99/년 가입 필수 (TestFlight 배포 시점).
- App Store Connect 에서 앱 레코드 생성 후 `eas.json` 의 `ascAppId` 기입.
- `eas build --profile preview --platform ios` → `eas submit --platform ios` → TestFlight.
- 내부 테스터로 본인 Apple ID 등록 → iPhone TestFlight 앱에서 수신.

### Google 쪽
- **Google Play Developer Account** $25 1회 가입.
- Play Console 에서 앱 생성 → 내부 테스트 트랙 생성.
- 서비스 계정 JSON 키를 `play-service-account.json` 으로 저장 (커밋 금지, EAS Secret 권장).
- `eas build --profile preview --platform android` → `eas submit --platform android` → Play Internal Testing.

## 배포 체크리스트

- [ ] EC2 `/etc/bookle.env` 에 `ANTHROPIC_API_KEY`, `WORDBOOK_API_KEY`, DB 접속 정보 투입
- [ ] RDS 에 `books`, `words` 테이블 생성 + seed insert
- [ ] Spring Boot 앱 빌드·배포 + `systemctl status bookle` 확인
- [ ] `curl -H "X-API-Key: ..." https://api.youngmi.works/wordbook/books` 200 확인
- [ ] CloudFront behavior `/wordbook/*` 메서드·헤더·TTL 설정 확인
- [ ] Apple / Google 개발자 계정 가입
- [ ] EAS Secret 등록 (`WORDBOOK_API_KEY`, Play 서비스 계정 키)
- [ ] Expo 프로젝트 생성 + `eas build --profile development` 로 Dev Client 설치
- [ ] `eas build --profile preview` 로 TestFlight / Play Internal 첫 업로드 성공
