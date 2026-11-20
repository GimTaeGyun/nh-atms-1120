> 사용기록조회 화면(`sample.useLog`, 2026-11-17) + **사용기록 자동 기록**(`TB_SCR_CUD_HIST`) — 예전 행위이력(`T_USER_ACTION_LOG`)을 대체.
> - **반입은 파일 단위**: 부록 A 의 새 파일은 통째로, diff 파일은 이 부 관련 줄만 바뀐 것(전체본을 덮어써도 됨), 삭제 파일은 지운다. 부록 B = SHA-512.
> - 제9부(코드관리)까지 넣은 뒤 넣습니다.
> - 경로 줄임: src/ = Frontend/nh-atms-web/src/, web/ = Backend/nh-atms-api/atms-web/src/main/java/kr/co/nh/atms/web/, mapper/ = Backend/nh-atms-api/atms-web/src/main/resources/mapper/

## 0. 한눈에 보기

- 사용자 결정(2026-11-17, 받은 문서 "사용기록(CRUD 감사로그) 재구현 가이드" 기준)
  1. **문서대로 다시 만듦** — 예전 행위이력 Aspect·테이블은 지우고 새 사용기록으로 대체.
  2. **메서드 이름 = 기록 정책**: 컨트롤러 메서드가 `select`·`insert`·`update`·`delete` 로 시작하면 기록(수행작업 R·C·U·D). 기존 컨트롤러 6개는 **이름만** 바꿈(주소·동작 그대로).
  3. 테이블 = 정본 `TB_SCR_CUD_HIST`(엑셀 사진). 주석의 C·U·D 에 **R(조회)·E(엑셀)을 잠정으로 더 씀**(Q-30).
  4. 컬럼이 없는 **IP·권한은 파라미터(PARAMS) 머리**에 `ip=… auth=…` 로, 500바이트에서 표시 없이 자름(Q-32).
  5. 실행 SQL = **모든 조회(+등록·수정·삭제)**, 실제 값을 끼운 글, 자르지 않음(CLOB). 비밀번호·토큰 값만 지우고 나머지 원문.
  6. 이력번호 = 시각 14 + 서버번호 1 + 초당 일련 4(Q-31). 실패도 기록(N + 오류코드).
  7. 기록 제외: 공통 콤보·내 정보·내 메뉴·로그인·대시보드. 엑셀은 **모든 화면 공통 엑셀 버튼이 서버에 알림(사유 없음)**. 보관기간·자동 삭제 없음(Q-33).
  8. 화면: 견본 › 견본 화면 › 사용기록조회, 메뉴 권한(조회)으로 통제, **관리자 = 전체 · 그 밖 = 내 사무소 기록만**. 기간 필수·최대 31일·서버 페이징 20건. 엑셀 있음(사유 없이). 행 두 번 클릭 = 상세 팝업.

```text
화면(모든 탭) ─ API 요청(X-Screen-Key) ─▶ CrudLogAspect(이름 접두어?) ─▶ 컨트롤러 실행 ─┬▶ 성공 Y / 실패 N+오류코드
                                              │ SQL 모으기 시작         ExecutedSqlCapture ┘   (원래 예외 그대로)
                                              └──────────────────────────────▶ CrudLogWriter(다른 트랜잭션) ─▶ TB_SCR_CUD_HIST
엑셀 버튼 ─ downloadExcel ─ POST /use-log/excel ─▶ E 기록(실패해도 내려받기 계속)

사용기록조회(sample.useLog)
 조회조건: 기간(필수·31일) · 수행작업 · 수행결과 · 사무소(관리자만 고름) · 사용자 · 화면 · 사용자 IP
 표: 일시 · 사용자 · 사무소 · 권한 · 화면 · 수행작업 · 결과 · IP   [엑셀]
 두 번 클릭 → 상세 팝업: + URI · 파라미터 원문 · 실행 SQL [복사]
```

| PARAMS 예 | 뜻 |
|---|---|
| `ip=10.1.2.3 auth=A {"query":{"fromDate":"20261117","toDate":"20261117"}}` | 관리자가 조회 |
| `ip=10.1.2.3 auth=L err=NTC_002 {"body":{"subject":"…"}}` | 남의 공지 수정 시도(403) |
| `ip=10.1.2.3 auth=L {"body":{"fileName":"기기정보","rowCount":20}}` | 엑셀(E) 20행 |

## 1. 준비 — DB

- `TB_SCR_CUD_HIST` 는 **판독본 참고 DDL**(`db/ddl/TB_SCR_CUD_HIST.sql`, 컬럼 14) — 개발 DB 에 있는지 `db/check/01_exists.sql`·`02_columns.sql` 로 먼저 확인. 없으면 기록만 안 남고 업무는 그대로(서버 로그 `[사용기록] 저장 실패`).
- 메뉴: `db/change/use_log_menu.sql` 을 `code_mgt_menu.sql` 다음에 한 문장씩, 마지막 COMMIT(메뉴관리 화면에서 등록해도 같음). 권한은 권한관리 → 메뉴 권한 탭에서 '조회'만(관리자 A 는 없어도 됨).
- 2호기(ws02)는 JVM 옵션 `-Datms.uselog.node=2`(이력번호가 겹치지 않게). 1호기는 기본 1.

## 2. 만들 파일 — 빨간줄 없이 가는 순서

| # | 구분 | 파일 | 왜 이 순서 |
|---|---|---|---|
| B1~B2 | diff | `api-messages.tsv` · `application.yml` | 오류 코드 ULG_001, 설정 `atms.uselog.*`(예전 `actionlog.*` 지움) |
| B3~B12 | **삭제** | `web/actionlog/**` 8개 · `mapper/actionlog/UserActionLogMapper.xml` · `actionlog/UserActionLogSupportTest` | 아무도 부르지 않음 — 먼저 지워도 빨간줄 없음 |
| B13~B22 | 새 | `web/common/uselog/` ExcludeCrudLog → CrudLogRules → HistIdGenerator → CrudLogEntry → CrudLogMapper(+XML) → CrudLogWriter → CrudLogContext → ExecutedSqlCapture → CrudLogAspect | 불리는 쪽 먼저 |
| B23~B27 | diff | Lookup·Menu·Me·Login·Dashboard 컨트롤러 | `@ExcludeCrudLog` 한 줄씩(B13 다음) |
| B28~B33 | diff | Notice·McnInfo·McnModel·AuthMgt·MenuMgt·CodeMgt 컨트롤러 | 메서드 이름만 바꿈 |
| B34~B41 | 새 | `web/uselog/` dto 3 → mapper(+XML) → UseLogRules → UseLogService → UseLogController | DTO → 매퍼 → 규칙 → 서비스 → 컨트롤러 |
| B42~B45 | 새 | 시험 4개 | ✅ `gradlew.bat build --offline` — `CrudLogNamingGuardTest` 가 B28~B33 이름을 검사 |
| F1~F2 | 새 | `domain/useLog.ts` → `api/endpoints/useLog.ts` | F3·F6 이 부름 |
| F3~F5 | diff | `utils/excel.ts` · `components/search/SearchForm.tsx` · `components/search/optionFields.tsx` | 엑셀 기록 · 직접 그리는 칸에도 `disabled` 전달(공통 버그 수정) · 화면 콤보(공통) |
| F6 | diff | `screens/screenIds.ts` | 팝업이 화면키를 씀 |
| F7~F9 | 새 | `UseLogParts` → `popup/UseLogViewModal` → `UseLogPage` | 불리는 쪽 먼저 |
| F10 | diff | `screens/registry.ts` | F9 다음(먼저 넣으면 빨간줄) ✅ `npm run check` |
| D1~D7 | 새·diff | `db/change/use_log_menu.sql`, `db/tools/gen_ddl.py`, `db/ddl` 사본·참고 DDL, `db/check` 2개 | 화면 확인 전에 SQL |

## 3. 가져다 쓴 공통 모듈

| 부품 | 파일 | 무엇 |
|---|---|---|
| `menuField`·`MenuSelect` **[새]** | `components/search/optionFields.tsx` | 화면(메뉴) 콤보 — 내 메뉴의 화면들, 값 = 메뉴ID, 입력해서 거르기 |
| `downloadExcel` **(바뀜)** | `utils/excel.ts` | 내려받기 전에 엑셀 기록(E). 모든 화면의 엑셀이 저절로 기록됨 — 화면 코드는 그대로 |
| `rangeField(maxDays 31)`·`optionField`·`officeField`·`personField`·`keywordField` | `components/search/*` | 조회조건(공통 필드만). 관리자가 아니면 사무소 칸은 내 사무소로 고정(`disabled`) |
| `usePagedSearch` · `DataGrid` · `ResultHeader` · `ExcelButton` · `ActionBar` | `hooks`·`components/*` | 서버 페이징 20건, 엑셀·조회 버튼 |
| `DetailModal` + `DetailGrid` | `components/display/*` | 상세 팝업 |
| `ResultTag` · `fmtDateTime14` · `fmtPerson` · `fmtOffice` | `components/display/StatusTag.tsx` · `utils/format.ts` | 성공(초록)·실패(빨강), 일시·사람·사무소 표시 |
| 백엔드 `@ExcludeCrudLog` · `CrudLogAspect.entryOf` **[새]** | `web/common/uselog/*` | 기록 제외 표시, 엑셀 기록이 같은 행 모양을 씀 |

## 4. 화면별 설명

### 4-1. 사용기록조회

- 처음 기간 = 오늘. 기간은 꼭 고르고 31일까지(넘으면 칸 아래 안내, 서버도 400). [조회]는 검색필터 안 하나(Ctrl+Enter).
- 사무소: 관리자는 비우면 모든 사무소, 고르면 그 사무소. 관리자가 아니면 내 사무소로 고정·흐리게(고를 수 없음, 서버도 내 사무소로 덮어씀). 2026-11-17 수정: 공통 `SearchForm` 이 직접 그리는 칸에 `disabled` 를 넘기지 않아 고를 수 있게 보이던 버그 → 공통에서 고침, 내 정보가 늦게 읽혀도 칸 값을 내 사무소로 맞춤(`UseLogPage` effect).
- 사용자 = 고른 사무소의 인원 콤보, 화면 = 내 메뉴의 화면 콤보(검색), 사용자 IP = 완전일치(예 `10.1.2.3`).
- 표: 최근 순 20건씩. 결과가 '실패'면 마우스를 올리면 오류코드. 권한 = 기록 당시 권한코드.
- [엑셀] = 지금 보이는 20건(사유 없음). 이 엑셀도 사용기록(E)에 남습니다.
- 이 화면을 본 것(조회)도 기록에 남습니다(R) — 감사 조회 자체의 기록.

### 4-2. 상세 팝업 (`screens/popup/UseLogViewModal.tsx`)

- 행 두 번 클릭 → 서버에서 1건을 다시 읽어(파라미터 원문·실행 SQL) 보여 줌. 다른 사무소 기록이면 '사용기록을 찾을 수 없습니다.'
- 실행 SQL: 줄바꿈 그대로, 길면 상자 안 스크롤. [복사] → 버튼이 잠깐 '복사됨'(은행망은 http 라 브라우저 클립보드 API 대신 글상자 복사 방식).
- SQL 위 `/* NoticeMapper.selectList */` = 어느 매퍼 문장인지. 권한 검사·현재 사용자 같은 공통 SQL 은 빠져 있습니다.

### 4-3. 새 화면을 만들 때 (모든 개발자)

- 컨트롤러 메서드 이름을 `select…`·`insert…`·`update…`·`delete…` 로 시작하게 하면 기록은 자동입니다. 일괄 저장은 `updateBatch`.
- 기록하지 않을 API(화면이 자동으로 부르는 콤보 등)는 `@ExcludeCrudLog("이유")`. 둘 다 아니면 빌드가 실패합니다(`CrudLogNamingGuardTest`).
- 같은 컨트롤러 안에서 `this.selectX()` 로 부르는 것은 기록되지 않습니다(스프링 AOP 한계).

## 5. 백엔드 요약

- `web/common/uselog/**` — `CrudLogAspect`(가장 바깥, 권한 403 도 실패로 남음) + `ExecutedSqlCapture`(MyBatis 플러그인, 컨트롤러 실행 동안만) + `CrudLogWriter`(REQUIRES_NEW). 기록 실패는 `log.error` 만, 원래 예외는 감싸지 않음. 사용자가 없으면 기록 안 함.
- `web/uselog/**` — `GET /api/web/use-log`(QRY, 기간 필수 VAL_002·형식 VAL_003·31일 VAL_001), `GET /api/web/use-log/{histId}`(QRY, 404 ULG_001), `POST /api/web/use-log/excel`(누구나, 문구 없음). 이력번호는 19자리라 글자로 내려 보냄.
- 규칙 한 곳: `CrudLogRules`(기록 모양), `UseLogRules`(조회 검사·보이는 범위), `HistIdGenerator`(이력번호).
- 폐쇄망 개발 DB 확인 필요: 새 SQL 전부(INSERT 의 CLOB·메뉴ID 서브쿼리, 조회 조인), IP 가 WebtoB 를 거쳐도 PC 주소로 보이는지(Q-33).

## 6. 검사 · 확인

```bash
cd Backend/nh-atms-api
```

```bash
gradlew.bat build --offline
```

```bash
cd Frontend/nh-atms-web
```

```bash
npm run check
```

화면에서 눌러 볼 순서

1. 공지사항 [조회] → 공지 하나 [수정] 저장 → 기기정보 [엑셀].
2. 견본 › 견본 화면 › 사용기록조회 [조회] → 위 셋이 조회(R)·수정(U)·엑셀(E)로 보이는지.
3. 수정 행 두 번 클릭 → 파라미터에 바꾼 제목, 실행 SQL 에 `UPDATE TB_NOTICE … '바꾼 제목'` 이 보이는지 → [복사].
4. 남의 공지 수정 시도(관리자 아님) → 사용기록에 '실패'(오류코드 NTC_002).
5. 관리자가 아닌 사용자로 열기 → 사무소 칸이 내 사무소로 고정·흐림(고를 수 없음), 다른 사무소 기록이 안 보이는지.
6. 서버 로그에 `[사용기록] 저장 실패` 가 없는지(있으면 테이블·컬럼 확인).
