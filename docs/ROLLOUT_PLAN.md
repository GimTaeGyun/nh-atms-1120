# 반입 후 적용 · 개발 준비 · 앞으로 할 일 (2026-11-09)

> 전제: 반입한 것 = `Common-ATMS-main.zip` + `npm-carry.zip`. ws01 은 Gitea·Jenkins 가 돌고 있고, JEUS 새 서버(atms01 :8080 등)는 확인 전.
> 개발서버에 올리는 앱 = `nh-atms-common-main` (NH-ATMS 대체). 점검 근거: [PRE_CARRYIN_CHECK.md](PRE_CARRYIN_CHECK.md).
> **반입 없이 폐쇄망 PC 에서 고쳐 FileZilla 로 올리는 순서 (2026-11-09 이후 변경분 포함): [CLOSED_PC_APPLY_GUIDE.md](CLOSED_PC_APPLY_GUIDE.md)**
> **개발서버에 6개 모듈 + 화면을 띄우고 DB 까지 확인하는 순서: [DEV_SERVER_BRINGUP.md](DEV_SERVER_BRINGUP.md)**
> 명령은 사진 보고 한 줄씩 친다. `sudo`·`su`·`ssh` 는 치지 않는다 (HIWARE 금지 명령).

## ★ JEUS 서버 6개 생성 완료 후 — 실제 진행 순서 (갱신)

> WAS 담당이 서버 6개를 만들었다는 회신을 받은 시점 기준. 배포킷·npm 캐시는 아직 서버에 없다고 보고 처음부터.
> 각 번호의 자세한 명령은 오른쪽 칸의 절·문서. ✋ = 결과를 보고 다음으로 (값이 다르면 멈추고 고칠 곳으로).

| # | 할 일 | 어디서 | 명령·문서 | ✋ 확인 |
|---|---|---|---|---|
| 1 | 반입 파일 해시 | 내부 PC | 아래 A | 두 zip SHA256 = 신청 값 |
| 2 | 배포킷 갱신 | ws01 | 아래 B | `bash -n` 결과 없음 |
| 3 | 배포킷 갱신 | wb01 | 아래 C | `install-wb01.sh` 정상 끝 |
| 4 | Jenkins npm 캐시 합치기 | ws01 | 아래 D | `content-v2`·`index-v5` |
| 5 | **JEUS 구성 확인 (읽기만)** — 포트는 받지 않음 | ws01 | [INFRA_MANUAL.md](INFRA_MANUAL.md) **10-1**, **10-1-1** (`ps … obj_name` 서버 이름, 로그 폴더 읽기) | 아래 "5번 판정표" |
| 6 | 앱 폴더 이름 (옛 `catms-journal` 등이 있을 때만) | ws01 | INFRA_MANUAL **10-2** | `catms cajou cabat cfile cifin cifex` 6개, `jeus` 2750 |
| 7 | `deploy-war.env`: `SERVER_<키>` = 5번의 실제 obj_name, **재기동 메뉴**(받은 번호), `PORT_*` 는 비워 둠 | ws01 | INFRA_MANUAL **10-1-1**·**10-3** (+ DB 준비 전이면 `HEALTH_SKIP="atms-batch"`) | `deploy-war.sh status` 에 6개 `프로세스 실행중` |
| 8 | 로그 폴더 | ws01 | INFRA_MANUAL **10-4** | `/WASlogs/jeusATMSlogs/nbatmsltws01/app` 있음 |
| 9 | 암호화 키 | ws01 | 아래 E (= POST_HANDOVER_EDITS 0-2) | 키 2개 `atmswas jeus 640` |
| 10 | WebtoB `http.m` (5번의 registration-id 반영) | wb01 → WebtoB 담당 | INFRA_MANUAL **11장**, `deploy/webtob/http.m.atms-jsv.txt` | ws01 에서 `ss -tn \| grep 19000` ESTAB, wb01 `curl 127.0.0.1:9000/v3/api-docs` → 200 (앱 배포 후) |
| 11 | 내부 Gitea 를 새 앱으로 교체 | 내부 PC | [PRE_CARRYIN_CHECK.md](PRE_CARRYIN_CHECK.md) **4장** | 두 잡 `build-only` 성공, WAR **6개** |
| 12 | 첫 배포 | Jenkins | 아래 I | Jenkins 콘솔 `기동 확인: <모듈> (로그 Started)`, `status` 에 마지막 기동 로그 시각 (= JEUS Jakarta 지원 확인, INFRA_MANUAL 10-5). 화면 쪽은 9000 으로 |
| 13 | 화면 | Jenkins → PC | 아래 I-3·4 | `http://16.88.122.112:9000` 에 이름 (DB 테이블 준비 후) |

### 5번 판정표 — WAS 회신과 실제가 같은지

| 확인 (10-1 출력) | 기대값 | 다르면 고칠 곳 (서버에서, 소스 재반입 없이) |
|---|---|---|
| 서버 이름 | `atms01`·`ajou01`·`abat01`·`file01`·`ifin01`·`ifex01` | `deploy-war.env` `SERVER_*` (표시용), `http.m` `*SERVER` — POST_HANDOVER_EDITS **B·F** |
| 포트 | **받지 않음** (2026-11-09) — `deploy-war.sh` 는 프로세스 + 앱 로그로 재기동 확인 | 화면·모듈 간 호출은 9000 경유라 무관. 에이전트 포트는 L4 연결 단계에서 |
| 서버 이름(obj_name) | `ps -u jeusatms -o args= \| grep -o 'obj_name=[^ ]*'` 6개 | `deploy-war.env` `SERVER_<키>` 를 글자까지 같게 (정지 확인에 씀) |
| 앱 경로·컨텍스트 | `/home/atmswas/catms`… 6개, `/` | 담당에 수정 요청 — POST_HANDOVER_EDITS **E** |
| registration-id | 서버마다 1개 | `http.m` 의 `*SERVER` 이름 — POST_HANDOVER_EDITS **F** |
| `dsATMS01` | Oracle 16.88.250.91:1531 NBATMSTP ATMS_APP, 대상 6개, JEUS 라이브러리에 Oracle JDBC + orai18n | 이름이 다르면 내부 Gitea 사본 `atms-common` 의 `jeus-common.yml` `jndi-name` (6개 모듈 공용 한 곳) — POST_HANDOVER_EDITS **G**. Tibero·대상 누락이면 담당 (T63·T64) |
| atms01 JVM 옵션 | 새 앱용 JVM 옵션은 **필요 없음** (2026-11-11 로그인 필수 — 예전 `-Datms.fixed-user.per-no` 는 넣지 않음, 남아 있어도 무해). 로그인 전 `/api/web/me` 401 `ME_002` 는 정상 | 테스트 계정 개인번호는 내부 Gitea 사본 `testAccounts.ts` — POST_HANDOVER_EDITS **K** |
| jeusRstart 메뉴 | 6개 서버의 boot·down 번호 | 7번에 기입. 없으면 재기동은 담당이 (배포 UNSTABLE, T29) |
| `curl` 6줄 | 앱 배포 전 `404` | `000` = 그 서버가 안 떠 있음 → 담당 |

- 서버 이름·포트·폴더가 회신과 다르면 **소스는 다시 반입하지 않는다** — 위 "고칠 곳"은 모두 서버 파일 또는 내부 Gitea 사본이다.
- 첫 배포 권장 순서: `atms-web` → `atms-journal`·`atms-file`·`atms-if-internal`·`atms-if-external` → (DB 의 `T_` 테이블·Spring Batch 메타 테이블 준비 후) `atms-batch`. Jenkins `MODULE` 로 하나씩.

## 0. 전체 흐름

```
[내부 PC]  반입 파일 해시 확인 ─┬─▶ [ws01] 배포킷 갱신 ─▶ Jenkins npm 캐시 합치기 ─▶ 암호화 키 ─▶ JEUS 상태 확인(읽기만)
                               ├─▶ [wb01] 배포킷(deploy) 갱신
                               ├─▶ [팀 PC] npm 캐시 합치기 ─▶ 로컬 실행 준비
                               └─▶ [내부 PC] Gitea 를 새 앱으로 교체 ─▶ Jenkins build-only 2개
[WAS·WebtoB 담당] atms01:8080·JNDI·개인번호 JVM 옵션·커넥터·재기동 메뉴 / http.m
                                                    ─▶ 백엔드 build-deploy ─▶ 화면 build-deploy ─▶ 화면 확인
```

| 단계 | 어디서 · 누가 | 끝난 표시 |
|---|---|---|
| A | 내부 PC · 우리 | 두 zip 의 SHA256 이 신청 값과 같음 |
| B | ws01 · atmswas | `bash -n` 결과 없음, `ls deploy/jeus` 에 3개 스크립트 |
| C | wb01 · atmswas | `install-wb01.sh` 정상 끝 |
| D | ws01 · atmswas | Jenkins 캐시에 `content-v2`·`index-v5` |
| E | ws01 · atmswas | 키 2개 `-rw-r----- atmswas jeus` |
| F | ws01 · atmswas (읽기만) → 담당 | atms01 이 8080 LISTEN, `dsATMS01` Oracle, 개인번호 JVM 옵션 |
| G | 내부 PC · 우리 | Gitea 맨 위에 `Backend`·`Frontend`, 두 잡 `build-only` 성공 |
| H | WebtoB 담당 | `127.0.0.1:9000/v3/api-docs` → 200 |
| I | Jenkins · 우리 | 백엔드·화면 `build-deploy` 성공, 화면에 사용자 이름 |
| L | 팀 PC · 각자 | 로컬 백엔드 `Started`, 화면 3100 |

---

## A. 반입 파일 확인 (내부 PC, PowerShell)

```powershell
Get-FileHash .\Common-ATMS-main.zip -Algorithm SHA256
```

```powershell
Get-FileHash .\npm-carry.zip -Algorithm SHA256
```

- 신청서에 적은 값과 같아야 한다 (대소문자 무관). 다르면 쓰지 않고 다시 반입.
- `Common-ATMS-main.zip` 은 7-Zip 으로 푼다. 이후 경로는 풀린 폴더 `Common-ATMS-main\` 기준.

## B. ws01 배포킷 갱신 (atmswas)

FileZilla(바이너리)로 `Common-ATMS-main\` **안의** `bin`, `conf`, `deploy`, `docs`, `README.md` 만 `/home/atmswas/atms/` 에 덮어쓴다.
`NH-ATMS`·`nh-atms-common-main` 은 올리지 않는다 (소스는 G 단계에서 Gitea 로). 실제 설정(`conf/*.env`)·`infra/`·`images/`·`release/` 는 zip 에 없어 그대로 남는다.

```bash
cd ~/atms
```

```bash
find bin deploy -name "*.sh" -exec sed -i 's/\r$//' {} +
```

```bash
find bin deploy -name "*.sh" -exec chmod 700 {} +
```

```bash
for f in bin/*.sh deploy/*/*.sh; do bash -n "$f" || echo "FAIL $f"; done
```

```bash
ls deploy/jeus
```

- `bash -n` 줄은 아무것도 안 나와야 한다. `ls` 에 `deploy-war.sh install-deploy-war.sh swap-deploy.sh` 가 보여야 한다.
- Gitea 백업 서비스 확인은 [DEV_SERVER_SETUP.md](DEV_SERVER_SETUP.md) "배포킷 갱신" ③ 둘째 블록 그대로.
- Gitea·Jenkins 는 다시 만들지 않는다. 배포 키(`authorized_keys`)도 그대로 — 경로가 같아 새 `deploy-war.sh` 가 바로 쓰인다.
- **Jenkins 잡 스크립트도 반입본으로 다시 붙여 넣는다** (백엔드·화면 2개, 절차 [DEV_SERVER_SETUP.md](DEV_SERVER_SETUP.md) "배포킷 갱신" ⑤). 2026-11-10 사례: 배포킷은 새 판인데 잡은 10-23 판이라 안내와 맞지 않았다

## C. wb01 배포킷 갱신 (atmswas)

FileZilla 로 `deploy` 폴더만 `/home/atmswas/deploy/kit/` 에 덮어쓴다 (**`~/docs` 아래에는 절대 두지 않는다**).

```bash
find ~/deploy/kit/deploy -name "*.sh" -exec sed -i 's/\r$//' {} +
```

```bash
bash ~/deploy/kit/deploy/webtob/install-wb01.sh
```

- 공개키 인자 없이 실행하면 스크립트만 새로 깔고 기존 Jenkins 키 줄은 그대로 둔다 ([FRONTEND_CICD_PLAN.md](FRONTEND_CICD_PLAN.md) 4-1).

## D. Jenkins npm 캐시 합치기 (ws01, atmswas)

FileZilla(바이너리)로 `npm-carry.zip` 을 `/home/atmswas/atms/inbox/` 에 올린다.

```bash
cd ~/atms/inbox
```

```bash
python3 -m zipfile -e npm-carry.zip npm-carry
```

```bash
ls npm-carry/_cacache
```

```bash
podman unshare mkdir -p ~/atms/infra/jenkins/cache/npm/_cacache
```

```bash
podman unshare cp -rn ~/atms/inbox/npm-carry/_cacache/. ~/atms/infra/jenkins/cache/npm/_cacache/
```

```bash
podman unshare chown -R 1000:1000 ~/atms/infra/jenkins/cache/npm/_cacache
```

```bash
rm -rf ~/atms/inbox/npm-carry
```

- `ls` 에 `content-v2`·`index-v5` 가 보여야 한다. Jenkins 재기동 불필요. 자세히: [REQUIRED_FILES.md](REQUIRED_FILES.md) 3-5 ④.

## E. 필드 암호화 키 (ws01, atmswas — 없으면 atms01 기동 실패)

[POST_HANDOVER_EDITS.md](POST_HANDOVER_EDITS.md) **0-2** 의 명령 그대로 (`/home/atmswas/keys/field-k202611.key.pem`·`.pub.pem`, 폴더 `jeus` 2750, 파일 `jeus` 640).
이미 만들어 둔 키가 있으면 다시 만들지 않는다 (`ls -l /home/atmswas/keys`).

## F. JEUS 상태 확인 (ws01, 읽기만) → WAS 담당 요청

[INFRA_MANUAL.md](INFRA_MANUAL.md) **10-1** 의 조회 명령을 그대로 친다 (`domain.xml` 의 서버 이름·포트·`dsATMS01`, `ss -ltn`, `curl`).

| 확인 | 기대 | 아니면 WAS 담당에 |
|---|---|---|
| 서버 `atms01` 포트 | 8080, 16.88.122.113 LISTEN | 생성·포트 변경 ([WAS_REQUEST_MANAGED_SERVERS.md](WAS_REQUEST_MANAGED_SERVERS.md)) |
| 앱 경로 | `/home/atmswas/catms`, 컨텍스트 `/` | 등록 |
| `dsATMS01` | Oracle 16.88.250.91:1531 NBATMSTP ATMS_APP, 대상 atms01, JEUS 라이브러리에 Oracle JDBC + **orai18n** | 데이터소스 수정 |
| WebtoB 커넥터 | atms01 → wb01:19000 (registration-id) | 커넥터 |
| 재기동 메뉴 | jeusRstart 에 atms01 boot·down | 없으면 배포 후 재기동은 담당이 (Jenkins UNSTABLE) |

- **서버 6개 모두 필요** (2026-11-09 결정: NH-ATMS 업무 모듈 5개도 함께 배포 — atms01 = 새 앱, ajou01·abat01·file01·ifin01·ifex01 = NH-ATMS 모듈). `dsATMS01` 대상도 6개.
- 5개 모듈은 업체 `T_` 테이블·Spring Batch 메타 테이블을 쓴다 → DB 담당과 확인. batch 는 DB 없으면 기동 실패 → 준비 전 `~/deploy/deploy-war.env` 에 `HEALTH_SKIP="atms-batch"`.
- 재기동 메뉴를 받으면 `~/deploy/deploy-war.env` 에 `STOP_MENU_web`·`BOOT_MENU_web` ([INFRA_MANUAL.md](INFRA_MANUAL.md) 10-3).

## G. 내부 Gitea 를 새 앱으로 교체 (내부 PC, Git Bash)

[PRE_CARRYIN_CHECK.md](PRE_CARRYIN_CHECK.md) **4장** 그대로: `NH-ATMS/nh-atms` 를 clone → `git rm -r -q .` → `nh-atms-common-main\` 안의 모든 것(숨김 `.gitignore`, `.env.*` 포함) 복사 → `git add -A` → commit → push.
웹훅으로 화면·백엔드 잡이 `build-only` 로 돈다.

| 잡 | 성공 표시 | 실패하면 |
|---|---|---|
| `nh-atms-frontend` | `npm ci --offline` → `added … packages`, `npm run build` 성공 | `ENOTCACHED` → D 단계 확인 (INFRA_MANUAL T67) |
| `nh-atms-backend` | `BUILD SUCCESSFUL`, 보관 파일 `atms-{web,journal,batch,file,if-internal,if-external}-0.0.1-<날짜>-<번호>.war` 6개 | `No cached version …` → Gradle 캐시 (DEV_SERVER_SETUP 9-5) |

## H. WebtoB `http.m` (WebtoB 담당)

`deploy/webtob/http.m.atms-jsv.txt` 를 전달 ([INFRA_MANUAL.md](INFRA_MANUAL.md) 11장). 새 앱에 필요한 것은 `/api/` 와 `/v3/api-docs` → atms01 두 줄이고, 나머지 줄은 있어도 무해하다.

## I. 첫 배포 · 확인

1. 백엔드 잡 `MODE=build-deploy` — 먼저 `MODULE=atms-web`, 확인 뒤 `MODULE=all`(WAR 6개) → 교체 → 재기동(메뉴 없으면 UNSTABLE → 담당에 해당 서버 재기동 요청). 다른 서버는 `curl http://16.88.122.113:<8081~8085>/v3/api-docs` 로 확인
2. ws01:

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://16.88.122.113:8080/v3/api-docs
```

```bash
curl -s http://16.88.122.113:8080/api/web/me
```

```bash
tail -n 50 /WASlogs/jeusATMSlogs/nbatmsltws01/app/atms-web.log
```

- `200`, `/api/web/me` 는 로그인 전이라 401 `ME_002` 면 정상(2026-11-11 로그인 필수 — 이름은 화면에서 로그인한 뒤 확인, 로그인 뒤 `ME_001`·`LGN_001` 이면 그 개인번호가 `TB_INSA` 에 있는지), `Started` 가 로그에 없고 모든 주소 404 면 JEUS Jakarta 미지원 의심 (INFRA_MANUAL 10-5), `[field-crypto] … 키 파일이 없습니다` 면 E.
3. 화면 잡 `MODE=build-deploy` → PC 브라우저 `http://16.88.122.112:9000` (PC → 9000 방화벽이 열려 있어야 함)
4. 확인: 첫 화면에 사용자 이름, 견본 화면(공지사항 등) 조회 — DB 오류면 L-2 의 테이블 확인, `/login` 에서 "비밀번호를 암호화해서 보냅니다 (키 k202611)"

---

## L. 로컬(팀 PC) 개발 환경

### L-1. 한 번만

| 할 일 | 방법 |
|---|---|
| Node 22.13 이상, JDK 21, Git | 각 PC 설치본 확인 (`node -v`, `java -version`) |
| npm 캐시 합치기 | `npm-carry.zip` → [REQUIRED_FILES.md](REQUIRED_FILES.md) 3-5 ②의 `Expand-Archive` + `robocopy` |
| Gradle 캐시 | 기존 NH-ATMS 개발에 쓰던 캐시 그대로 (새 앱은 새 라이브러리 없음) |
| 소스 | 내부 Gitea `NH-ATMS/nh-atms` clone (G 이후 새 앱) — **개발의 정본은 내부 Gitea** |
| `D:\local-db.yml` | DB 계정·비밀번호·내 개인번호 (스페이스 들여쓰기) — 새 앱 `docs/01_실행_배포.md` 1-1 |

### L-2. DB 확인 (한 번, 누군가 한 명)

새 앱 `db/check/01_exists.sql` → `02_columns.sql` → 필요하면 `db/seed/01·02(·03)` → `db/check/03_seed_status.sql`. 결과는 `db/README.md` 의 확인표에 적는다.

### L-3. 매일 실행

```bash
cd Backend/nh-atms-api
```

```bash
sh gradlew :atms-web:bootRun --offline
```

새 Git Bash 창:

```bash
cd Frontend/nh-atms-web
```

```bash
npm ci --offline
```

```bash
npm run dev
```

- 브라우저 `http://localhost:3100`. 백엔드 없이 화면만 보려면 `npm run dev:mock`.
- 커밋 전 검사: 화면 `npm run check`, 백엔드 `sh gradlew build --offline` (새 앱 `CLAUDE.md` "커밋 규칙").

### L-4. 개발 준비 — 새 화면 만드는 법

- 견본 셋 중 모양이 가장 비슷한 것을 복사한다 (새 앱 `CLAUDE.md` "새 화면을 만드는 순서"):

| 화면 모양 | 견본 |
|---|---|
| 목록 + 팝업으로 등록·수정 | 공지사항 `sample.notice` |
| 목록 / 편집 탭 | 기기정보 `sample.mcnInfo` |
| 표 직접 편집 + 일괄 저장 | 기기모델코드 `sample.mcnModel` |

- 순서: API 계약(`docs/07_API_계약.md`)에 절 추가 → 프론트 4종(domain·endpoints·mocks·Page) → 백엔드(controller·service·mapper·dto + 테스트) → 공유 파일 연결 → 검사.
- 부품 카탈로그: 화면 "공통 모듈"(`sample.commonModules`) — 탭마다 [코드 보기].
- 테이블은 TO-BE 정본대로 ([DB_TOBE_TABLES.md](DB_TOBE_TABLES.md), 새 앱 `docs/04_DB_규칙.md`). 근거 없는 규칙은 `잠정 Q-xx` 로 한 곳에.
- 여러 명·에이전트로 나눌 때: 새 앱 `agent-briefs/README.md` (파일 소유 규칙).

---

## 앞으로 할 일

### 지금 (첫 배포까지)

| # | 할 일 | 누가 | 단계 |
|---|---|---|---|
| 1 | 반입 파일 해시 확인, ws01·wb01 배포킷 갱신 | 우리 | A·B·C |
| 2 | Jenkins·팀 PC npm 캐시 합치기 | 우리 | D·L-1 |
| 3 | 암호화 키 생성 | 우리 | E |
| 4 | JEUS 상태 조회 → atms01:8080·JNDI·커넥터·**개인번호 JVM 옵션**·재기동 메뉴 요청 | 우리 → WAS 담당 | F |
| 5 | WebtoB `http.m` 요청 | 우리 → WebtoB 담당 | H |
| 6 | DB 테이블 확인·시드 | 우리 (+ DB 담당) | L-2 |
| 7 | Gitea 교체 → `build-only` 2개 성공 | 우리 | G |
| 8 | 첫 `build-deploy` (백엔드 → 화면) → 화면 확인 | 우리 | I |
| 9 | 방화벽: PC → 16.88.122.112:9000 (화면), 필요하면 PC → 113:8080 (직접 확인) | 우리 → 인프라 | — |

### 다음 (개발 시작 후)

| # | 할 일 | 근거 |
|---|---|---|
| 10 | 업무 화면 개발 시작 — 견본 복사 방식, API 계약 먼저 | L-4 |
| 11 | 현업 질문 답 받기 — 새 앱 `docs/90_현업질문_후보.md` Q-01~Q-17 (특히 Q-17 로그인 비밀번호 원천, Q-09 관리자, Q-11 권한 채택 규칙) | 답이 오면 `잠정` 메서드 한 곳만 고침 |
| 12 | 로그인 방식 확정 — 지금은 세션 로그인 + 공통 테스트 비밀번호(2026-11-11 로그인 필수, 잠정 Q-17, 세션 쿠키 확인 필요) → 실제 비밀번호 원천 / NH SSO 로 바꿀지 | 새 앱 `docs/07_API_계약.md` §7 |
| 13 | 화면 배포 확인 뒤 `crypto.field.required=true` (평문 거부) | 같은 곳 |
| 14 | GitHub(Common-ATMS) ↔ 내부 Gitea 동기화 규칙 정하기 — 폐쇄망에서 바뀐 소스를 밖으로 가져올지, 배포킷만 밖에서 만들지 | 지금은 소스 정본 = 내부 Gitea, 배포킷 정본 = GitHub |

### 나중에 (결정 대기)

| # | 할 일 | 근거 |
|---|---|---|
| 15 | ~~NH-ATMS 나머지 5개 모듈을 올릴지~~ → **함께 배포로 결정(2026-11-09)**. 남은 일: 그 화면 API 를 새 앱 화면에서 쓰려면 인증 연결(NH-ATMS JWT ↔ 새 앱), 원격제어 콜백 `/api/remote-exec/put-msg-resp` 를 새 앱에 둘지 | [COMMON_ATMS_INTEGRATION.md](COMMON_ATMS_INTEGRATION.md) 6-8 |
| 16 | 에이전트 연결 — L4 VIP, ATM망 방화벽, `/api/agent/ping` 미구현 결함 | [INFRA_MANUAL.md](INFRA_MANUAL.md) 13-5 |
| 17 | 2호기(ws02·wb02) — IP·접속 경로가 오면 | INFRA_MANUAL 14장 |
| 18 | HTTPS — 운영 전 재검토 | CLAUDE.md |
