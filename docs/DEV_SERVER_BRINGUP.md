# 개발서버에 백엔드 6개 + 화면 띄우고 DB 까지 확인하기 (2026-11-10)

> 전제 (2026-11-10 사용자 답): 내부 Gitea 는 **이미 새 앱(nh-atms-common-main)으로 교체**, 개발 DB 에 새 앱의 `TB_` 테이블은 **아직 없음**, 목표 = **6개 모듈 전부 + 화면**.
> 배포킷(ws01)은 [CLOSED_PC_APPLY_GUIDE.md](CLOSED_PC_APPLY_GUIDE.md) 1~3·3-1 까지 적용, 암호화 키 생성 완료 기준.
> 명령은 사진 보고 한 줄씩. `sudo`·`su`·`ssh` 는 치지 않는다. 개인번호·비밀번호·개인키는 사진에서 가린다.

## 전체 순서

| # | 단계 | 누가 | 끝난 표시 |
|---|---|---|---|
| 1 | WAS·WebtoB·DB 담당 요청 (한 번에) | 우리 → 담당 | 아래 1장 표의 회신 |
| 2 | DB: 새 앱 `TB_` 테이블 확인·생성 요청·시드 | 우리 + DB 담당 | `03_seed_status` = 5 / 15 / 5 |
| 3 | DB: NH-ATMS 5개 모듈의 `T_` 테이블·배치 메타 | DB 담당 결정 | `T_BATCH_JOB`·`BATCH_JOB_INSTANCE` 등 있음 |
| 4 | 로컬 PC 에서 DB 연결 먼저 확인 | 우리 | 로컬 `/api/web/me` 에 이름 |
| 5 | 내부 Gitea 소스: 5개 모듈용 공통 보강·로그인 테스트 계정 개인번호(선택) | 우리 | Jenkins `build-only` WAR 6개 |
| 6 | 백엔드 배포 (atms-web → 4개 → batch) | Jenkins | 로그 `Started Atms…Application` 6개 |
| 7 | 화면 배포 | Jenkins | `http://16.88.122.112:9000/` 200 |
| 8 | DB 시험 (API·화면) | 우리 | 아래 8장 표 모두 정상 |

---

## 1. 담당자 요청 (한 번에 묶어서)

| 담당 | 요청 | 왜 | 확인 (ws01, 읽기만) |
|---|---|---|---|
| WAS | `jeusmenu.c` 로 `jeusRstart` **재컴파일** | 지금 실행 파일은 10-22 판(7·8 = WebtoB) → 자동 재기동 불가 | `grep -a -o 'nbatmsltws01_[a-z0-9]*' /JEUS/jeusATMS/jeusMENU/jeusRstart \| sort \| uniq -c` 에 6개 |
| WAS | `nbatmsltws01_abat01` 기동 (DB 준비 후 앱 배포하면 됨) | 2026-11-09 관찰: 프로세스 없음 | `ps -u jeusatms -o args= \| grep -o 'obj_name=[^ ]*' \| sort` 에 6개 |
| WAS | 서버 6개의 **WebtoB registration-id** 회신 | `http.m` `*SERVER` 이름 | — |
| WAS | 데이터소스 **`dsATMS01`** = Oracle 16.88.250.91:1531 NBATMSTP ATMS_APP, **대상 6개 서버**, JEUS 라이브러리에 Oracle JDBC + **orai18n** | 앱은 JNDI 만 씀 (WAR 에 DB 비밀번호 없음) | 앱 로그에 `NameNotFoundException` 없음 |
| WAS | 앱 로그 폴더 `/WASlogs/jeusATMSlogs/nbatmsltws01/app` (jeusatms 쓰기, jeus 그룹 읽기) | 2026-11-09 없음. 배포 스크립트가 이 로그로 기동 확인 | `ls -ld /WASlogs/jeusATMSlogs/nbatmsltws01/app` |
| WAS | 6개 서버 앱 경로 = `/home/atmswas/catms`·`cajou`·`cabat`·`cfile`·`cifin`·`cifex`, 컨텍스트 `/` | WAR 를 풀어 넣는 폴더 | 첫 배포 뒤 로그 |
| WebtoB | registration-id 를 넣은 `deploy/webtob/http.m.atms-jsv.txt` 적용 | 화면·모듈 간 호출이 9000 경유 | 6장 확인 |
| DB | 2·3장 | 테이블 생성 권한은 DB 담당 | — |
| 인프라 | PC → 16.88.122.112:9000 방화벽 (아직이면) | 화면을 PC 브라우저로 봄 | PC 에서 `http://16.88.122.112:9000` |

## 2. DB — 새 앱 `TB_` 테이블 (atms-web 이 씀)

새 앱 소스의 `db/` 폴더 (`db/README.md`, `db/seed/README.md`). **은행 DB 도구**(SQL Developer 등)로 앱 계정(ATMS_APP)에 접속해 돌린다.

| 순서 | 파일 | 하는 일 |
|---|---|---|
| 2-1 | `db/check/01_exists.sql` | 15개 테이블 존재 확인 (읽기만) |
| 2-2 | `db/check/02_columns.sql` | 컬럼 비교 (읽기만) |
| 2-3 | 없는 테이블 → DB 담당에 생성 요청 | 참고 DDL `db/ddl/TB_*.sql` 을 함께 전달. **은행 마스터(TB_INSA·TB_OFC_REG_MGT·TB_MCN_INFO·TB_CO_MODEL 등)는 이관 데이터가 있어야** 화면에 값이 나온다 — 빈 테이블만 만들면 사용자·기기가 안 나옴 |
| 2-4 | `db/seed/01_menu.sql` → `02_auth_menu.sql` | 견본 메뉴 5 · 권한-메뉴 15 행 |
| 2-5 | `db/seed/03_fixed_user_auth.sql` (틀) | 로그인할 테스트 계정 개인번호에 권한 행이 없을 때만 (파일 안 `CHANGE_ME` → 개인번호, **커밋하지 않음**) |
| 2-6 | `db/seed/10_sample_notice.sql` (선택) | 견본 공지 5건 |
| 2-7 | `db/check/03_seed_status.sql` | 5 / 15 / 5 |

- 최소 조건(2026-11-11 로그인 필수): `TB_INSA` 에 로그인할 개인번호 행(없으면 로그인 `LGN_001`), `TB_SCR_AUTH` 에 그 개인번호의 개인권한(없으면 부서권한 `PER_NO='1'`) 승인(`APV_STS='Y'`)·미만료 행 → 없으면 빈 메뉴
- `T_API_MESSAGE`·`T_USER_ACTION_LOG` 는 없어도 된다 (백엔드가 건너뜀)

## 3. DB — NH-ATMS 5개 모듈의 `T_` 테이블 (journal·batch·file·ifin·ifex 가 씀)

5개 모듈은 업체 `T_` 테이블(`T_ATM`, `T_JOURNAL`, `T_BATCH_JOB`, `T_IF_QUEUE`, `T_GW_INTERFACE`, `T_MSG_ASYNC` … 약 50개)을 쓴다. **TO-BE 정본(`TB_`)과 다른 업체 스키마**라 개발 DB 에 만들지 여부는 DB 담당·현업 결정이다.

| 선택 | 결과 |
|---|---|
| A. 만든다 (권장: 개발 확인용) | 업체 스크립트 `NH-ATMS/Document/2.개발문서/sql/V1.0.0__init_schema.sql` ~ `V1.5.2__…sql` 을 **번호 순서대로**, 이어서 모듈 폴더의 `src/main/resources/sql/*.sql` (batch: `spring-batch-meta-oracle.sql`·`batch-job-seed-oracle.sql`·`stat-tables-oracle.sql`·`daily-report-tables-oracle.sql`·`table-management-oracle.sql`, ifex `ddl_if_external_20260710.sql`, ifin `alter_t_gw_interface_eai.sql`, journal `V1.2.8__ap_deploy_schema.sql`) — DB 담당이 검토 후 적용. **`00_drop_all.sql`·`00_create_user.sql`·`00_rename_to_standard.sql` 은 절대 돌리지 않는다** (업체 도커 DB 용: 전부 삭제·사용자 생성) |
| B. 아직 안 만든다 | journal·file·ifin·ifex 는 대개 **기동은 된다** (조회할 때 `ORA-00942`). **batch 는 Spring Batch 메타 테이블(`BATCH_JOB_INSTANCE` 등)이 없으면 기동 실패** → batch 는 배포하지 않고 `HEALTH_SKIP="atms-batch"` 유지 |

- 배치 시드(`batch-job-seed-oracle.sql`)의 `HOST_NAME` 은 `nbatmsltws01` 이어야 그 서버에서 돈다 (2호기 이중 실행 방지, INFRA_MANUAL 14-8)

## 4. 로컬 PC 에서 DB 연결 먼저 확인 (서버 배포 전, 권장)

서버는 JEUS 데이터소스가 끼어 원인 구분이 어렵다. 같은 DB 를 PC 에서 먼저 본다 ([ROLLOUT_PLAN.md](ROLLOUT_PLAN.md) L-1~L-3).

1. `D:\local-db.yml` 에 DB 계정·비밀번호 (새 앱 `docs/01_실행_배포.md` 1-1 — 개인번호는 넣지 않음, 화면에서 로그인)
2. IntelliJ Gradle 창 → `atms-web` → `bootRun` (또는 `gradlew.bat :atms-web:bootRun --offline`)
3. 브라우저 `http://localhost:8080/api/web/me` → 로그인 전이라 401 `ME_002` 가 정상(백엔드는 뜸)
4. 화면: `Frontend/nh-atms-web` 에서 `npm ci --offline` → `npm run dev` → `http://localhost:3100` → 로그인 화면에서 개인번호 + 공통 비밀번호로 로그인 → 견본 메뉴. 로그인 실패 `LGN_001` = `TB_INSA` 에 개인번호 없음, `ORA-00942` = 테이블 없음

## 5. 내부 Gitea 소스 (내부 PC)

이미 새 앱으로 교체됐으므로 clone 폴더에서 [CLOSED_PC_APPLY_GUIDE.md](CLOSED_PC_APPLY_GUIDE.md) **6-1 ~ 6-4** 그대로:

| # | 할 일 |
|---|---|
| 5-1 | (사용자가 이미 넣은 5개 모듈은 그대로 `':atms-common'`) `atms-common` 에 `util/GlobalIdGenerator.java` 복사 + `jeus-common.yml` 에 `webtob-url`·`url.*`·`agent-entry.*` 추가 (CLOSED_PC 6-1 ①②) |
| 5-2 | (선택) `Frontend/nh-atms-web/src/screens/login/testAccounts.ts` 의 `CHANGE_ME_1`~`6` → 테스트 계정 개인번호 (2026-11-11 로그인 필수 — 예전 `application-jeus.yml` 개인번호·JVM 옵션은 없앰) |
| 5-3 | PC 에서 `gradlew.bat bootWar -Pjeus --offline` → WAR 6개 |
| 5-4 | commit → push → 화면 잡은 `build-deploy`, 백엔드 잡은 2026-11-10부터 **`build-deploy`(빌드 + 6개 배포 + 재기동)** 로 돔. 백엔드 잡 보관 파일 **WAR 6개** 확인 |

## 6. 백엔드 배포 (Jenkins `nh-atms-backend` → Build with Parameters)

한 번에 하나씩. 각 단계 확인 뒤 다음으로.

| 순서 | MODE | MODULE | RESTART | 확인 |
|---|---|---|---|---|
| 6-1 | `build-deploy` | `atms-web` | ✔ | 콘솔 `교체 완료: atms-web` → `기동 확인: atms-web` (SUCCESS). `jeusRstart 재컴파일 전` 으로 UNSTABLE 이면 WAS 담당에 atms01 재기동 요청 |
| 6-2 | `build-deploy` | `atms-journal` | ✔ | 같은 방식 |
| 6-3 | `build-deploy` | `atms-file` | ✔ | |
| 6-4 | `build-deploy` | `atms-if-internal` | ✔ | EAI 는 기본 `mock` |
| 6-5 | `build-deploy` | `atms-if-external` | ✔ | |
| 6-6 | `build-deploy` | `atms-batch` | ✔ | **3장 A 를 끝낸 뒤**. 먼저 ws01 `~/atms/deploy/jeus/deploy-war.env` 의 `HEALTH_SKIP="atms-batch"` 줄 삭제 |

각 단계 뒤 ws01 확인:

```bash
~/atms/deploy/jeus/deploy-war.sh status
```

```bash
grep -h -E 'Started Atms|APPLICATION FAILED|NameNotFound|ORA-' /WASlogs/jeusATMSlogs/nbatmsltws01/app/*.log | tail -20
```

| 로그에 보이는 것 | 원인 → 할 일 |
|---|---|
| `Started Atms…Application` | 정상 |
| 로그 파일이 아예 안 생기고 주소 모두 404 | JEUS 가 Jakarta(Spring Boot 3) 를 못 올림 (INFRA_MANUAL 10-5) → WAS 담당 |
| `NameNotFoundException … dsATMS01` | 데이터소스 이름·대상 서버 (T63) → WAS 담당 |
| `ORA-17056` / 한글 깨짐 | JEUS 라이브러리에 orai18n 없음 (T22) → WAS 담당 |
| `[field-crypto] … 키 파일` | `/home/atmswas/keys` 권한 (`namei -l`) |
| `ORA-00942` | 테이블 없음 → 2·3장 |
| batch `APPLICATION FAILED TO START` + `BATCH_JOB_INSTANCE` | Spring Batch 메타 테이블 없음 → 3장 |

WebtoB 적용 확인 (ws01 → wb01:9000):

```bash
ss -tn | grep ':19000'
```

```bash
for u in /v3/api-docs /api/web/me /api/batch/jobs; do echo "$u $(curl -s -o /dev/null -w '%{http_code}' --max-time 5 http://16.88.122.112:9000$u)"; done
```

- `ss` 에 `16.88.122.112:19000` ESTAB 줄들, `/v3/api-docs` 200 → WebtoB ↔ atms01 연결
- `302`(→ `/error/error.html`) 면 `http.m` 미적용·이름 불일치, `503` 이면 그 서버 미연결 → WebtoB·WAS 담당

### 6-B. Jenkins 결과 읽기 (2026-11-10~)

| 색 | 뜻 | 원인 보는 곳 |
|---|---|---|
| 초록 SUCCESS | WAR 교체 + 서버 재기동 + 로그에 새 `Started Atms…Application` 까지 확인 | — |
| 노랑 UNSTABLE | 서버에 **반영 안 됨**: 재기동 메뉴 없음 / `jeusRstart` 재컴파일 전 / `RESTART` 꺼짐 | 콘솔의 "WAS 담당 재기동 필요"·"교체만 함" 줄, 콘솔 끝 `===== ws01 상태` |
| 빨강 FAILURE | 빌드 실패, 또는 재기동·기동 확인 실패 (실패한 모듈은 직전 판으로 자동 되돌림) | 빌드면 Gradle 오류 줄. 기동 실패면 콘솔의 `----- …/<모듈>.log (끝 40줄)`·`----- …/JeusServer.log (끝 40줄)`, 그리고 콘솔 끝 `===== ws01 상태` |

- 콘솔 전체: 빌드 번호 → **Console Output** (긴 로그는 "Full Log" 를 누른다)
- 서버에서 같은 내용: `tail -n 100 ~/deploy/war-deploy.log` (결과 한 줄씩), 6-A 의 로그 명령

### 6-A. 로그 보기 (ws01, atmswas — 읽기만)

| 로그 | 위치 | 언제 |
|---|---|---|
| 앱 로그 (Spring) | `/WASlogs/jeusATMSlogs/nbatmsltws01/app/<모듈>.log` (`atms-web.log` 등) | 기동 성공·실패, 요청 오류, DB 오류 |
| JEUS 서버 로그 | `/JEUS/jeusATMS/logs/JeusSystem/nbatmsltws01_<서버>/` | 앱 로그가 아예 안 생길 때 (WAR 배포·Jakarta·JNDI 문제) |
| 배포 기록 | `~/deploy/war-deploy.log` | Jenkins 배포·재기동 결과 |

```bash
tail -f /WASlogs/jeusATMSlogs/nbatmsltws01/app/atms-web.log
```

```bash
tail -n 200 /WASlogs/jeusATMSlogs/nbatmsltws01/app/atms-web.log
```

```bash
grep -n -E 'ERROR|Exception|APPLICATION FAILED|Started Atms' /WASlogs/jeusATMSlogs/nbatmsltws01/app/atms-web.log | tail -30
```

```bash
ls -lt /JEUS/jeusATMS/logs/JeusSystem/nbatmsltws01_atms01/ | head
```

```bash
tail -n 100 ~/deploy/war-deploy.log
```

- `tail -f` 는 실시간으로 계속 보여 준다 — 끝낼 때 `Ctrl+C`
- 다른 모듈은 파일 이름만 바꾼다: `atms-journal.log`, `atms-batch.log`, `atms-file.log`, `atms-if-internal.log`, `atms-if-external.log` / JEUS 폴더는 `nbatmsltws01_ajou01` 등
- 앱 로그 폴더가 없으면(2026-11-09 관찰) 앱이 처음 뜰 때 만들어지거나, WAS 담당에 생성 요청 (1장). JEUS 서버 로그 폴더가 안 읽히면(`Permission denied`) WAS 담당에 확인
- 로그에 개인번호·이름이 찍힐 수 있으니 사진은 가려서

## 7. 화면 배포 (Jenkins `nh-atms-frontend`)

`MODE=build-deploy` → 콘솔 끝에 wb01 `deploy-front.sh` 성공 (`/` 200, `index.html` 일치).

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://16.88.122.112:9000/
```

→ `200`. `ENOTCACHED` 로 빌드 실패면 Jenkins npm 캐시 합치기 ([ROLLOUT_PLAN.md](ROLLOUT_PLAN.md) D).

## 8. DB 시험

### 8-1. ws01 에서 API (9000 경유 — PC 방화벽과 무관)

```bash
curl -s http://16.88.122.112:9000/actuator/health
```

```bash
curl -s http://16.88.122.112:9000/api/web/me
```

```bash
curl -s http://16.88.122.112:9000/api/web/menu/my
```

```bash
curl -s http://16.88.122.112:9000/api/web/notice
```

```bash
curl -s http://16.88.122.112:9000/api/batch/jobs
```

| 호출 | 정상 | 아니면 |
|---|---|---|
| `/actuator/health` | `{"status":"UP"}` (DB 연결 포함) | `DOWN` → 6장 로그의 데이터소스 오류 |
| `/api/web/me` | 로그인 전 401 `ME_002` = 정상 (curl 은 세션이 없음) | 500/`ORA-00942` → 2장. 이름·권한은 화면에서 로그인한 뒤 확인 |
| `/api/web/menu/my` | curl 은 401 `ME_002`(로그인 필요). 화면에서 로그인하면 `SMP000`·`SMP001~004` | 로그인 뒤 빈 메뉴 → `TB_SCR_AUTH`·시드 02 |
| `/api/web/notice` | curl 은 401 `ME_002`. 화면에서 로그인하면 `[견본]` 5건 (시드 10 을 넣었으면) | 빈 목록 = 정상(시드 안 넣음) |
| `/api/batch/jobs` | 배치 작업 목록 (3장 A 후) | 503/302 → abat01·`http.m`, `ORA-00942` → 3장 |

- 응답에 개인번호·이름이 나오므로 사진은 가려서

### 8-2. PC 브라우저 (PC → 9000 방화벽이 열린 뒤)

`http://16.88.122.112:9000` →

| 화면 | 확인 | DB |
|---|---|---|
| 첫 화면 | 오른쪽 위 사용자 이름 | TB_INSA, TB_SCR_AUTH |
| 견본 > 공지사항 | 조회 → 등록 → 수정 → 삭제 (`[견본]` 글로) | TB_NOTICE 쓰기 |
| 견본 > 기기정보 | 조회 (이관 데이터가 있으면 목록) | TB_MCN_INFO |
| 견본 > 기기모델코드 | 조회 | TB_CO_MODEL |
| `/login` (견본, 기본 꺼짐) | "비밀번호를 암호화해서 보냅니다 (키 k202611)" | 키 파일 |

- 공지 등록이 되면 **쓰기 권한까지 확인** 끝. 시험 데이터는 `db/seed/99_cleanup_samples.sql` 로 지움

### 8-3. 끝난 뒤

- `~/atms/deploy/jeus/deploy-war.sh status` 사진 + 8-1 결과를 기록 ([WORK_LOG.md](WORK_LOG.md))
- 남은 일: L4·에이전트 연결 (INFRA_MANUAL 13-5), 2호기, `crypto.field.required=true`, 현업 질문 Q-01~Q-17 ([ROLLOUT_PLAN.md](ROLLOUT_PLAN.md) "앞으로 할 일")
