# CLAUDE.md — nh-atms-common (NH-ATMS 공통 뼈대 + 견본)

> 이 저장소에서 일하는 Claude 세션용 규칙. 사람용 안내는 `README.md`, 설계 근거는 `docs/00_설계.md`.
> 규칙마다 "그 규칙이 들어 있는 파일"을 괄호로 적었다. 말로만 있는 규칙은 없다 — 헷갈리면 그 파일을 연다.
> 경로 줄임말: `src/` = `Frontend/nh-atms-web/src/`, `web/` = `Backend/nh-atms-api/atms-web/src/main/java/kr/co/nh/atms/web/`, `mapper/` = `Backend/nh-atms-api/atms-web/src/main/resources/mapper/`, `atms-common/` = `Backend/nh-atms-api/atms-common/src/main/java/kr/co/nh/atms/common/`.

## 답변 규칙

- 모든 답변 전에 사용자에게 **5지선다 질문**을 먼저 해서 요구사항을 분명히 한 뒤, 자세하고 정확하게 답한다.
  - `AskUserQuestion` 도구로 선택지 **4개** + 도구가 자동으로 붙이는 "기타(직접 입력)" = 5개. 추천안을 첫 번째에 둔다.
  - 질문 전에 파일을 읽어 선택지를 구체적으로 만들어도 된다.
  - 예외: 사용자의 메시지가 방금 한 질문의 **답**이면 다시 묻지 않고 그 답대로 진행한다(질문 되풀이 금지).
  - 예외: 이 세션이 **하위 에이전트**(리드가 `agent-briefs/` 지시문으로 띄운 작업자)이면 묻지 않는다. 지시문 범위 안에서만 일한다.
- 사용자는 **에이텍 김태균 선임(프론트엔드 담당)** 이다.
  - 쉽게, 천천히, 짧은 bullet 로 쓴다. 어려운 말은 비유로 푼다(예: "프록시 = 개발 서버가 API 요청을 백엔드로 대신 전달해 주는 것").
  - 백엔드·DB 이야기는 "프론트에서 보면 무엇이 달라지나"부터 말한다.
- "보고용" 을 달라고 하면 **메시지만 짧게** 쓴다(설명·표 없이 붙여 넣을 글).
- 답은 한국어로 한다.

## 서버·폐쇄망 규칙 (nh-atms `CLAUDE.md` "서버·폐쇄망 규칙" 그대로, `docs/00_설계.md` §0.1·§10)

- 서버 명령은 **한 줄에 하나씩**. 명령마다 따로 ` ```bash ` 블록에 넣는다(사진 보고 그대로 칠 수 있게).
- `sudo`, `su` 는 절대 쓰지 않는다(은행 서버 HIWARE 통제 — 금지 명령 3번이면 세션 잠김).
- 은행 개발망은 **폐쇄망**이다. 인터넷·npm·maven 다운로드가 안 된다.
  - npm 패키지·Gradle 의존성을 **새로 추가하지 않는다**(`docs/00_설계.md` §4). 있는 것: `Frontend/nh-atms-web/package.json`, `Backend/nh-atms-api/**/build.gradle`.
  - 프론트는 반입된 npm 캐시로 `npm ci --offline`, 백엔드는 `--offline` 빌드.
- 반입물은 텍스트 위주로. 바이너리·압축 안의 압축은 피하고, **파일별 SHA-512** 를 붙인다(`docs/00_설계.md` §10).
- **반입은 파일/모듈 단위 (2026-11-13 사용자 결정)** — 전체 소스가 아니라 바뀐 파일만 반입한다(다른 소스 영향 최소화). 그래서 변경은 **고칠 파일 수를 최소로**(공통 테마·CSS 한 곳에서 해결되면 화면 파일은 건드리지 않음) 계획하고, 화면 개발 가이드(09)에 그 파일 목록 + 전체본(또는 새 화면은 새 파일 전체·diff) + SHA-512 를 남긴다(json step `kind: replace` = 전체 교체).
- 프론트 TypeScript 대상은 **ES2020** 이다(`Frontend/nh-atms-web/tsconfig.app.json` `"target": "ES2020"`).
  - 쓰지 말 것: `.at()`, `Object.hasOwn`, `structuredClone`, `replaceAll`, `findLast`.
- 비밀값(DB 비밀번호·개인번호·키)은 저장소에 넣지 않는다. 로컬 DB 계정은 각 PC 의 `D:\local-db.yml`(`Backend/nh-atms-api/atms-common/src/main/resources/local-common.yml` 주석).

## 이 저장소는 무엇인가

- 개발자(사람)와 각자의 Claude 가 **이 저장소 하나만 보고** 화면별 프론트·백엔드·SQL 을 같은 규칙으로 만들게 하는 "공통 뼈대 + 견본"이다(`docs/00_설계.md` §0).
  - 비유: 새 화면을 찍어 내는 **본(틀)**. 견본 화면 3개가 틀이고, 공통 모듈 화면이 부품 카탈로그다.
- 들어 있는 것
  - `Frontend/nh-atms-web/` — React 19 · antd 6 · Vite 7 · TS(ES2020). 실행 가능한 프론트.
  - `Backend/nh-atms-api/` — Spring Boot 3.5 · Java 21 · MyBatis. 모듈 `atms-common` + `atms-web`, 패키지 `kr.co.nh.atms.web`.
  - `db/` — TO-BE 정본 사본(`db/ddl/_source_*.csv`), 참고용 DDL(생성물), 확인 SQL(`db/README.md`). 견본 시드 `db/seed` 는 2026-11-11 삭제(사용자 결정, 폐쇄망에서도 삭제).
  - `docs/` — 설계·API 계약·가이드. `agent-briefs/` — 화면 에이전트 지시문.
- 화면 9개(`Frontend/nh-atms-web/src/screens/registry.ts`)
  - 대시보드 `main.dashboard` — **홈 탭 = 대시보드**(2026-11-16 사용자 결정, 견본 카드 바로가기 삭제). 설계서 26-0000 초기화면: 사무소명 · 지역/사무소 구분 콤보([조회]만, 새로고침 없음) · ⑦ 운영 대수 · ③ 일평균 거래량(전 영업일) · ⑤ 기기상태 2×2(누르면 표 거름) · ① 공지 6건(제목 = 상세 팝업, 전체보기 = 공지 탭) · ④⑥ 기기상태정보(4행). 이미지(280×160)·바로가기 가로 3칸 = 오른쪽 끝 정렬, 바로가기 = 공지사항·기기정보·코드관리(2026-11-17, `DashboardParts.tsx` `QUICK_LINKS` 한 곳). 레벨 = 권한코드 A 중앙본부·T 영업본부·L 영업점(설정 `atms.dashboard.level.*`), 내 사무소 = 인사 소속, 본부 소속 = `JUNG_MOJUM` — 규칙 한 곳 `web/dashboard/service/DashboardRules.java`(잠정 Q-25·26·27, 바뀔 수 있음). 메뉴 등록 SQL `db/change/dashboard_menu.sql`. `web/dashboard/**`, `screens/main/Dashboard*.tsx`, 계약 §10, 화면 개발 가이드 제8부
  - 공통 모듈 `sample.commonModules` — 부품 카탈로그(실제 API 로 시연)
  - 공지사항 `sample.notice` — **팝업형 CRUD** 견본이자 실제 화면(2026-11-13, 사용자 결정: 사무소코드가 빈 공지 = 전체 공지(잠정 Q-24), 사무소 공지는 그 사무소 + 관리자만 보임, **전체 공지 등록·남의 글 수정·삭제 = 관리자만**, 그 밖은 내 사무소 공지 + 본인 글만 — 서버 `web/notice/service/NoticeRules.java` 403 `NTC_002`·`NTC_003`, 프론트 `NoticeParts.tsx` `canEditNotice`. 상세 팝업 이전·다음 글, 새 글 N 3일, 계약 §3, 화면 개발 가이드 제6부)
  - 기기정보 `sample.mcnInfo` — **목록/편집 탭형** 견본
  - 기기모델코드 `sample.mcnModel` — **표 직접 편집·일괄 저장형** 견본
  - 권한관리 `sample.authMgt` — 실제 관리 화면(2026-11-11, 사용자 결정: AS-IS "시스템 사용권한부여" 구성 기준, 홈 카드로 진입). 탭 ① 부서권한 ② 개인권한 — 둘 다 `TB_SCR_AUTH`(공지사항 모양, **`PER_NO='1'` = 부서권한**, 2026-11-11) ③ 메뉴 권한 `TB_SCR_AUTH_MENU`(상단 권한 콤보 → 메뉴 전체 체크 표 → [저장], '사용' 체크 = 보유, 행추가·행삭제 없음 — 사용 해제는 `USE_YN='N'`). 로그인 권한 = 유효한 개인권한 우선, 없으면 내 부서(`TB_INSA.SAMUSO`→`TB_OFC_RAW` 의 `SAMUSO`=`DEPT_CODE`·`ONLINE_CODE`=`OFC_CODE`)의 부서권한(`CurrentUserLoader.pickEffective`, `docs/04_DB_규칙.md` §4-2). `web/authmgt/**`, `screens/sample/AuthMgt*.tsx`, `screens/popup/AuthUserEditModal.tsx`, 계약 §8. 소스는 사용자가 폐쇄망에서 직접 반영, Git 에도 같은 소스를 올려 둔다. 처음부터 만들기 문서 = `docs/09_권한관리_폐쇄망_적용가이드.md` 제1부(아래 "화면 개발 가이드" 규칙)
  - 메뉴관리 `sample.menuMgt` — 실제 관리 화면(2026-11-12, 사용자 결정: 목록 + 팝업 = 공지사항 모양, 홈 카드로 진입). `TB_SCR_MENU` 3단계 대메뉴(상단)·중메뉴(좌측)·소메뉴(화면). **메뉴ID = 영문 대문자 3 + 중메뉴 번호 2 + 소메뉴 번호 2**(`SYS0000` 대·`SYS0300` 중·`SYS0312` 소, 대·중 = G, 소 = M), 번호는 같은 상위 안 MAX+1(지운 번호 재사용 안 함), 순서는 `SORT_NO` 따로, 레벨은 ID 모양으로(새 컬럼 없음), 상위는 수정 불가. 팝업 상위 고르기 = 대 → 중 연쇄 콤보(antd `Select` 검색·번호·하위 수·미사용 맨 아래). 삭제 = 물리 삭제 + 그 메뉴의 `TB_SCR_AUTH_MENU` 행도 함께, 하위가 남으면 `MNU_001`. 규칙 한 곳: `web/menumgt/service/MenuMgtRules.java`(+ 프론트 미리보기 `screens/sample/MenuMgtParts.tsx` `previewMenuId` — 둘을 같이 고친다). 예전 견본 메뉴 이관 SQL `db/change/menu_three_level.sql`(SMP000 → SMP0000·SMP0100·SMP0101~). `web/menumgt/**`, `screens/sample/MenuMgt*.tsx`, `screens/popup/MenuEditModal.tsx`, 계약 §9, 화면 개발 가이드 제2부
  - 코드관리 `sample.codeMgt` — 실제 관리 화면(2026-11-16, 사용자 결정: 최종 프로토타입대로). 왼쪽 코드구분 `TB_CODE_MASTER`(체크·순번·코드구분·코드명·사용여부, 나머지는 상세 팝업) · 오른쪽 상세코드 `TB_CODE_DTL`(체크·순번·코드·코드명·**데이터1~5 모두, 칸 이름 고정**), 등록·수정·상세 = 팝업, 권한 = 메뉴 권한 플래그, 엑셀 없음. **코드구분은 마스터·상세 모두 VARCHAR2(10)**(판독 2자리 정정), 상세의 `CODE_SECT` 로 마스터를 찾음, 상세가 남은 코드구분은 삭제 금지 `CDM_002`. 업무구분·처리구분은 직접 입력(잠정 Q-28). **공통 콤보(`/api/web/lookup/codes`)·기기사명·권한명도 `TB_CODE_DTL` 로 옮김(Q-10 확정, `TB_CO_CODE*` 삭제 예정)**. 메뉴 행 SQL `db/change/code_mgt_menu.sql`. `web/codemgt/**`, `screens/sample/CodeMgt*.tsx`, `screens/popup/CodeMgtModals.tsx`, 계약 §11, 화면 개발 가이드 제9부
  - 사용기록조회 `sample.useLog` — 실제 관리 화면(2026-11-17, 사용자 결정: 받은 "사용기록(CRUD 감사로그) 재구현 가이드"대로). **모든 화면의 조회·등록·수정·삭제·엑셀을 서버가 자동으로 `TB_SCR_CUD_HIST` 에 남기고**(아래 백엔드 규칙 "메서드 이름 = 사용기록"), 이 화면은 읽기만: 기간 필수·최대 31일·서버 페이징 20건, 관리자 = 전체·그 밖 = 내 권한 사무소 기록만, 행 두 번 클릭 = 상세 팝업(파라미터·실행 SQL·[복사]), 엑셀(사유 없음). 수행작업 R·C·U·D·E(정본 주석은 C·U·D, 잠정 Q-30), IP·권한은 PARAMS 머리(Q-32), 이력번호 = 시각14+서버번호1+일련4(`atms.uselog.node`, Q-31), 보관기간 없음(Q-33). 예전 행위이력(`web/actionlog`, `T_USER_ACTION_LOG`)은 삭제. 메뉴 SQL `db/change/use_log_menu.sql`. `web/common/uselog/**`(기록), `web/uselog/**`(화면 API), `screens/sample/UseLog*.tsx`, `screens/popup/UseLogViewModal.tsx`, 계약 §12, 화면 개발 가이드 제10부

### 이웃 저장소와의 관계 (`docs/00_설계.md` §0.1·§2)

| 저장소 | 이 저장소에서의 쓰임 | 하면 안 되는 것 |
|---|---|---|
| `nh-atms` (GitHub `GimTaeGyun/nh-atms`) | 원본. 부품·규칙·정본 CSV 를 **참고만** 한다 | 거기 파일을 고치거나 커밋하지 않는다 |
| `nh-atms-server` (배포킷 + 소스 스냅숏) | 배포 방식(JEUS WAR·JNDI·`D:\local-db.yml`)을 **복사해 온 출처** | 설정·인프라(yml·properties·gradle·Jenkins·Docker·스크립트)를 **절대 바꾸지 않는다**. 바꿀 일이 생기면 먼저 사용자에게 묻는다 |

- 이 저장소의 폴더 이름 `Frontend/nh-atms-web`, `Backend/nh-atms-api` 는 배포킷 Jenkinsfile 경로와 같게 일부러 맞췄다 → 이름을 바꾸지 않는다(`docs/00_설계.md` §3).

## 먼저 볼 문서

| 순서 | 문서 | 언제 |
|---|---|---|
| 1 | `docs/00_설계.md` | 무엇이 정해졌나(§0.1 결정, §13.1 결정 결과), 왜 그렇게 했나 |
| 2 | `docs/07_API_계약.md` | **응답 모양의 정본.** 백엔드 DTO·프론트 `domain/*.ts` 는 여기와 글자 하나까지 같아야 한다 |
| 3 | `docs/02_프론트_개발가이드.md` | 새 화면 순서, 공통 부품 찾는 순서 |
| 4 | `docs/03_백엔드_개발가이드.md` | 패키지·컨트롤러·서비스·매퍼 규칙, 감사 컬럼 |
| 5 | `docs/04_DB_규칙.md` | TB_ 규칙 요약, 이 저장소가 쓰는 테이블 |
| 6 | `docs/05_공통모듈_목록.md` | 날짜·콤보·팝업·표·출력 부품 카탈로그(쓰는 예) |
| - | `docs/09_권한관리_폐쇄망_적용가이드.md` · `docs/92_화면개발가이드_작성법.md` | 화면 개발 가이드(화면마다 제N부, 생성물) · 그 작성법 |
| - | `docs/93_반입목록.md` (생성물) | **다음 반입에 들어갈 파일·변경점을 모듈별로**(지난 반입 기준 커밋 이후, 최종본 한 번만). 파일을 바꾸면 `tools/carry-in.json` 에 그 파일을 모듈에 넣고 `python3 -I tools/gen_carry_in.py` — `--check` 가 모듈에 빠진 파일을 잡는다. 반입을 마치면 `base` 를 그때 커밋으로 올린다 |
| - | **Figma `[NH농협은행] ATMS GUI`** (2026-11-12 기록) — https://www.figma.com/design/voJt0DqmYV0WcNdVoju8SH/-NH%EB%86%8D%ED%98%91%EC%9D%80%ED%96%89--ATMS-GUI?node-id=294-955&t=mvJfHzIj0OswJiAA-1 (fileKey `voJt0DqmYV0WcNdVoju8SH`, node `294-955`) | 화면 모양(배치·색·글꼴)을 맞출 때. 폐쇄망 PC 에서는 안 열리므로 인터넷 PC 또는 클라우드 세션의 Figma MCP 로 본다 |
| - | `docs/90_현업질문_후보.md` | 정본·현업 답이 없는 것 → 질문 후보(번호 `Q-xx`) |
| - | `agent-briefs/README.md` | 화면을 에이전트에게 나눠 맡길 때 |
| - | `db/README.md`, `db/ddl/README.md` | DB 확인 순서 (`db/seed` 는 2026-11-11 사용자 결정으로 삭제 — 폐쇄망 개발 DB 에 메뉴·권한 행이 이미 있음) |

- 문서와 코드가 다르면 **코드가 지금 사실**이다. 문서를 고치거나 사용자에게 알린다.
- 살아있는 부품 예시는 공통 모듈 화면: `Frontend/nh-atms-web/src/screens/sample/CommonModulesPage.tsx` + `screens/sample/commonModules/*Tab.tsx`(탭마다 [코드 보기]).

## 정해진 것 (바꾸지 않음, `docs/00_설계.md` §0.1·§13.1)

- **NH-ATMS 업무 모듈 5개 동거 (2026-11-09 사용자 결정)** — `Backend/nh-atms-api/atms-{journal,batch,file,if-internal,if-external}` 는 NH-ATMS 에서 가져온 것(개발서버 함께 배포용, `settings.gradle` 주석). **2026-11-10 사용자 결정: 5개 모두 이 저장소의 `atms-common` 하나를 쓴다**(`build.gradle` 원본 그대로 `project(':atms-common')`, `atms-common-nh` 분리 방식 폐기) — 그래서 `atms-common` 에 `util/GlobalIdGenerator`(if-external 용)와 `jeus-common.yml` 의 `jeus.webtob-url`·`jeus.url.*`·`jeus.agent-entry.*`(journal·file 용)가 있다. 이 공통의 보안 설정이 5개 모듈에도 적용된다(모듈 자체 허용 경로는 그대로, batch `/api/batch/**` 는 401). 이 저장소 규칙(응답코드·화면키·새 의존성 금지 등)은 **`atms-common`·`atms-web`·`Frontend` 에만** 적용한다. 5개 모듈은 원본 NH-ATMS 규칙(JWT·`T_` 테이블·한글 문구)을 그대로 두며, 고칠 때는 nh-atms-server `NH-ATMS/CLAUDE.md` 를 따른다. `ApiMessageCodeGuardTest` 는 이 저장소 앱 2개만 검사한다.
- **로그인 필수 · 세션 사용자** (2026-11-11 사용자 결정, server1109 에서 2026-11-25 옮김. 그 전엔 "로그인 없음 · 고정 사용자") — `/login` 에서 개인번호·비밀번호로 로그인(`POST /api/web/auth/login`)하면 서버 HTTP 세션에 개인번호가 들어가고, `/api/web/**` 사용자는 그 세션으로 정해진다(`web/login/**`, `web/common/auth/SessionUserFilter.java`·`SessionUserConfig.java`).
  - 설정 `atms.fixed-user.per-no`·`atms.login.enabled` 는 **없앴다**. 다시 만들지 않는다.
  - 세션 없음 401 `ME_002`, 세션 개인번호가 `TB_INSA` 에 없음 401 `ME_001` → 프론트는 로그인 화면으로(`src/app/MeGate.tsx`, `src/api/http.ts` `bindUnauthorized`). 헤더에 [로그아웃](`src/components/layout/AppHeader.tsx`) — 조회 캐시·탭을 지운다.
  - 비밀번호는 공통 테스트 비밀번호 1개(`web/login/service/InsaLoginVerifier.java` `TEST_PASSWORD` = `src/screens/login/testAccounts.ts` `TEST_PASSWORD`, 잠정 Q-17). 두 곳은 같이 바꾼다.
  - 로그인 화면 옆 테스트 계정 6명은 `src/screens/login/testAccounts.ts`. 저장소에는 `CHANGE_ME_1`~`6` — 실제 개인번호는 **내부 Gitea 사본에서만** 넣는다(GitHub 에 개인번호 금지).
  - 비밀번호는 node-forge 로 암호화해 보낸다(`docs/07_API_계약.md` §7, `atms-common/security/crypto/**`, `src/utils/fieldCrypto.ts`). 개발서버는 키 파일 `/home/atmswas/keys` 가 없으면 기동 실패(`application-jeus.yml` `crypto.field`). npm `node-forge` 는 "새 의존성 금지"의 사용자 승인 예외.
  - 이름·사무소는 `TB_INSA`, 권한은 `TB_SCR_AUTH`(개인권한 우선, 없으면 부서권한 `PER_NO='1'`) → `TB_SCR_AUTH_MENU`(`CurrentUserLoader.java`). 권한 행은 권한관리 화면에서 넣는다.
- **설정은 배포킷 방식 복사**(이 저장소 안에만) — `D:\local-db.yml` + `LocalDbLoginGuard`, `orai18n`, DB 테스트 `@Tag("db")`(기본 제외, `-PdbTest` 로만), `-Pjeus` WAR + JNDI `dsATMS01`(`Backend/nh-atms-api/build.gradle` 머리말).
- **이름: SQL 만 대문자** — SQL 키워드·테이블·컬럼은 대문자, 컬럼은 정본 이름 그대로(`SELECT OFC_CODE, KANM FROM TB_OFC_REG_MGT`). 자바·JSON·TS 필드는 MyBatis 자동 변환 camelCase(`OFC_CODE` → `ofcCode`).
- **권한은 5개 플래그** — `TB_SCR_AUTH_MENU` 의 `QRY/REG/UPD/DEL/EXE_ROL_YN`.
  - 서버: `@RequireMenuAuth(screen = "<화면키>", need = Perm.X)`(`web/common/auth/RequireMenuAuth.java`, `Perm.java`). 조회 GET 도 QRY, 쓰기는 REG·UPD·DEL(`docs/07_API_계약.md` §0). 일괄 저장처럼 한 요청에 여럿이면 서비스에서 `MenuAuthService.check`(`web/mcnmodel/service/McnModelService.java`).
  - 프론트: `usePermission(screenKey)` → `{canQry, canReg, canUpd, canDel, canExe, isAdmin}`(`src/hooks/usePermission.ts`). 버튼은 `ActionBar`·`AuthButton` 이 알아서 흐리게 한다(`src/components/auth/ActionBar.tsx` `ACTION_DEFS`).
  - 관리자: `AUTH='A'` 이면 전부 허용(잠정 Q-09).
- **화면번호를 소스에 쓰지 않는다** — 식별자는 영문 화면키(`src/screens/screenIds.ts`, 값은 모두 `''`). 파일 이름·라우트·테스트·SQL 주석 어디에도 번호를 쓰지 않는다.
  - 화면키 규칙: `{대분류 slug}.{camelCase 이름}`, 팝업 `popup.{이름}`. 라우트·메뉴 경로는 `/{slug}/{kebab 이름}`(`src/screens/registry.ts` `menuPathOf`, 백엔드 `MenuAuthService.menuPathOf` 와 같은 규칙).
- **버전 표기 금지** — 폴더·파일·클래스·설정 키·SQL 파일 이름에 `v1`·`V2` 같은 세대 표시를 쓰지 않는다(`db/README.md`·`db/ddl/README.md` "버전 번호를 붙이지 않는다", nh-atms `CLAUDE.md`).
- **"지어내지 않기"** — 근거 없는 테이블·컬럼·코드값·규칙을 만들지 않는다. 답이 없는 규칙은 `static` 메서드 **한 곳**에 모으고 javadoc 에 `잠정 Q-xx` 를 단다(예: `web/notice/service/NoticeRules.java` `nextNo`, `web/common/DbBytes.java` `length`).

## DB — TO-BE 테이블 (`docs/00_설계.md` §7, nh-atms `Document/2.개발문서/21_TOBE_DB설계지침.md`)

- 정본은 nh-atms `Document/2.개발문서/20_TOBE컬럼정의서.csv` · `20_TOBE엔터티목록.csv`(테이블 113개).
  - 이 저장소 안의 사본: **`db/ddl/_source_columns.csv`** · `_source_entities.csv`. 단, 이 저장소가 쓰는 **15개 테이블만** 들어 있다(`db/tools/gen_ddl.py` `TABLES`).
  - 15개 밖의 테이블은 정본 전체(옆에 받아 둔 `../nh-atms/Document/2.개발문서/20_TOBE컬럼정의서.csv`)에서 찾거나 리드에게 그 행을 달라고 한다.
- 테이블·컬럼·타입·PK·NULL 은 정본과 **똑같이**. 정본에 없는 테이블·컬럼은 **지어내지 않고** `docs/90_현업질문_후보.md` 에 질문으로 올린다(방법: `agent-briefs/db-table.md`).
- `db/ddl/*.sql` 은 생성물이다. 손으로 고치지 않는다 → `python3 -I db/tools/gen_ddl.py`(`db/ddl/README.md`).
- 감사 컬럼 4개 `CRT_DTM`·`CRT_CMENO`·`LSCHG_DTM`·`LS_CMENO` 는 **자동으로 채워진다**.
  - DTO 가 `AuditFields` 를 상속하면 `AuditInterceptor` 가 INSERT 때 4개, UPDATE 때 2개를 넣는다(`atms-common/audit/AuditInterceptor.java`).
  - 매퍼는 `#{crtDtm}`, `#{crtCmeno}`, `#{lschgDtm}`, `#{lsCmeno}` 를 **쓰기만** 한다. 값을 손으로 만들지 않는다(`mapper/notice/NoticeMapper.xml` `insert`·`update`).
- 날짜·시간은 CHAR 문자열(`YYYYMMDD`, `YYYYMMDDHH24MISS`). 정본이 DATE 로 정한 컬럼만 예외(예: `TB_NOTICE.WRT_DATE` → `TO_CHAR(… ,'YYYYMMDD') AS WRT_DATE`).
- CHAR 비교는 `CAST(#{v} AS CHAR(n))`, CHAR/VARCHAR2 가 섞인 조인은 `TRIM`/`RTRIM`. 인덱스 쪽 컬럼은 가공하지 않는다(`mapper/mcninfo/McnInfoMapper.xml` 머리 주석).
- `${}` 금지, 페이징은 `OFFSET #{offset} ROWS FETCH NEXT #{limit} ROWS ONLY` + 건수 SQL 따로(`mapper/notice/NoticeMapper.xml` `selectList`/`countList`).
- 물리 FK 를 만들지 않는다. "쓰는 중이면 삭제 금지" 같은 검사는 서비스에서(`web/mcnmodel/service/McnModelRules.java` `deletable`).
- 은행 개발 DB 에 이 테이블이 있는지는 `db/check/01_exists.sql`·`02_columns.sql` 로 먼저 본다. DDL 은 참고용(`db/README.md`). 새 메뉴 행은 `docs/04_DB_규칙.md` §5 SQL 틀, 권한은 권한관리 화면.
- 이 클라우드에서는 Oracle 에 붙을 수 없다 → 새 SQL 은 보고에 "폐쇄망 개발 DB 확인 필요" 로 적는다.

## 새 화면을 만드는 순서 (견본 복사)

1. **모양을 고른다** — 가장 비슷한 견본 하나를 복사해서 시작한다.

   | 화면 모양 | 복사할 견본 | 프론트 | 백엔드 |
   |---|---|---|---|
   | 목록 + 팝업으로 등록·수정·상세 | 공지사항 | `screens/sample/NoticePage.tsx`, `NoticeParts.tsx`, `screens/popup/NoticeEditModal.tsx`, `NoticeViewModal.tsx` | `web/notice/**`, `mapper/notice/NoticeMapper.xml` |
   | [목록] / [등록·수정] 탭 | 기기정보 | `screens/sample/McnInfoPage.tsx`, `McnInfoParts.tsx`, `McnInfoColumns.tsx` | `web/mcninfo/**`, `mapper/mcninfo/McnInfoMapper.xml` |
   | 왼쪽 목록 + 오른쪽 표 직접 편집 → [저장] 한 번에 | 기기모델코드 | `screens/sample/McnModelPage.tsx`, `McnModelParts.tsx`, `McnModelColumns.tsx` | `web/mcnmodel/**`, `mapper/mcnmodel/McnModelMapper.xml` |
   | 탭 여러 개에 위 모양이 섞인 관리 화면 | 권한관리 | `screens/sample/AuthMgtPage.tsx`, `AuthMgtParts.tsx`, `AuthMgtColumns.tsx`, `screens/popup/AuthUserEditModal.tsx` | `web/authmgt/**`, `mapper/authmgt/AuthMgtMapper.xml` |
   | 목록(페이징 없음, 트리 순서) + 팝업, 팝업 안 연쇄 콤보 | 메뉴관리 | `screens/sample/MenuMgtPage.tsx`, `MenuMgtParts.tsx`, `MenuMgtColumns.tsx`, `screens/popup/MenuEditModal.tsx` | `web/menumgt/**`, `mapper/menumgt/MenuMgtMapper.xml` |

   - 사람용 같은 표 + 부품별 참고 위치: `docs/08_개발매뉴얼_공지사항.md` §0-1.
   - 프론트 경로는 `Frontend/nh-atms-web/src/` 기준, 백엔드 `web/` 는 `Backend/nh-atms-api/atms-web/src/main/java/kr/co/nh/atms/web/`, `mapper/` 는 `atms-web/src/main/resources/mapper/`.
2. **계약을 먼저 쓴다** — `docs/07_API_계약.md` 에 절을 하나 더한다(경로·요청·응답 모양·권한·문구 코드).
3. **프론트 묶음 3종**(`src/` 아래): `domain/<area>.ts` · `api/endpoints/<area>.ts`(`<area>Api` + `<area>Keys`) · `screens/<slug>/{Name}Page.tsx`. (2026-11-11 사용자 결정: 프론트 단위 테스트(vitest)·e2e(Playwright)·목 서버(MSW, `src/mocks`, `dev:mock`) 삭제. 백엔드 테스트는 그대로.)
4. **백엔드 묶음**: `web/<업무>/{controller,service,mapper,dto}` + `mapper/<업무>/*.xml` + 서비스 Mockito 테스트 1개 이상.
5. **공유 파일 연결**(리드 몫): `screens/screenIds.ts` 키 한 줄, `screens/registry.ts` `def(...)` 한 줄, 응답코드 `api-messages.tsv`, 메뉴 행은 코드가 아니라 메뉴관리 화면(DB)에서.
6. **검사** → 아래 "커밋 규칙"의 명령 → 문서 갱신.
7. **화면 개발 가이드를 쓴다 (2026-11-12 사용자 결정 — 새 화면 개발 요청마다 반드시)** — `docs/09_권한관리_폐쇄망_적용가이드.md`(화면 개발 가이드)에 **제N부**를 이어 붙인다. 형식·작성법: `docs/92_화면개발가이드_작성법.md`, 견본: 제1부 권한관리.
   - `tools/screen-guides/<화면키>.md`(본문 §0 한눈에 · §1 DB · §2 빨간줄 없는 개발 순서 · §3 가져다 쓴 공통 모듈 · §4 탭·팝업별 설명(프론트 자세히) · §5 백엔드 요약 · §6 검사)와 `<화면키>.json`(steps = 의존 순서: 불리는 파일 먼저) 을 쓰고 `python3 -I tools/gen_screen_guide.py` 로 문서를 만든다(부록 A = 새 파일 전체 · 기존 파일은 그 화면 관련 줄만 diff, 부록 B = SHA-512).
   - 그 화면 소스를 나중에 고치면 도구를 다시 돌려 부록을 갱신한다. 커밋 전 `python3 -I tools/gen_screen_guide.py --check`.
   - 답변에서도 같은 형식으로 제시한다: 개발 순서표(새/diff·왜 이 순서·✅ 확인 지점) → 화면별 설명 → 가져다 쓴 모듈.

- **메뉴는 DB(`TB_SCR_MENU`)·메뉴관리 화면으로만 관리한다(2026-11-12 사용자 결정).** 프론트에 메뉴 표·내장 메뉴가 없다(`screens/l1.ts`·`menuSeed.ts`·`VITE_MENU_SOURCE` 삭제). 상단 대메뉴·아이콘 줄(모두 같은 기본 아이콘, `components/layout/topItems.ts`)·사이드 패널·탭 제목·브레드크럼 = `GET /api/web/menu/my` 응답(`stores/useNavStore.ts`, 상단 칸 키 = 대메뉴ID, 홈 = `HOME_TOP`). `registry.ts` 는 화면키 → 화면 파일 연결 + 메뉴에 없을 때 쓸 이름만. 메뉴에 있지만 화면 코드가 없으면 사이드 패널에 '준비 중', 보이는 메뉴가 없으면 안내 + [다시 시도].
- 자세한 지시는 `agent-briefs/screen-fullstack.md`(혼자 끝까지), `screen-frontend.md`(프론트 먼저), `screen-backend.md`(계약 먼저).

### 프론트 핵심 규칙 (견본에서 그대로 보이는 것)

- **글자·색 기준 (2026-11-13 사용자 결정, 현업 화면 기준)** — 본문·표·입력·버튼·좌측 메뉴 `var(--fs)`, 보조 글자 `var(--fs-sm)`, 배지·꼬리글·오류 문구만 `var(--fs-xs)`.
  - **화면 비율**: 2026-11-13 에 크기를 모두 × 0.8 로 줄였다가 **2026-11-18 사용자 결정으로 되돌림**(폐쇄망 크롬 100% 에서 너무 작았음, 휴대폰 데모와 같은 크기가 기준). 지금 값 = 글자 **14px**·보조 13px·배지 12px, 입력 높이 32px, 머리줄 56px(`theme/tokens.ts` `UI_SCALE = 1`·`sc()`). CSS 의 px·화면 폭 기준(`config/constants.ts` 1280·1600·1050·1366)도 되돌렸다. 새 CSS·인라인 크기는 그대로의 값으로 쓴다(TS 에서는 `sc(값)`).
  - 글자색은 `var(--text)`·`--text-secondary`·`--text-muted` 만(이보다 연한 회색 금지, 흰 바탕 대비 4.5:1 이상). antd 컴팩트 모드는 쓰지 않는다(입력 글자가 12px 로 줄어듦). 화면 코드에 `fontSize: 11` 같은 숫자·연한 색을 직접 쓰지 않는다(`src/theme/tokens.ts`·`global.css`, 화면 개발 가이드 제4부).
- **콘텐츠는 흰 바탕·스크롤 없음, 표는 남은 높이 채움 (2026-11-13 사용자 결정)** — `DataGrid`·`EditableGrid` 에 `height` 를 주지 않으면 부모가 세로 flex 일 때 남은 높이를 채운다(`components/grid/useFillHeight.ts`, 화면 틀·탭 칸·마스터/디테일은 CSS 로 세로 flex — `components/page/page.css`). 그래서 목록 표는 `height` 없이 `PageFrame`(또는 탭 칸) 바로 아래에 두고 일반 div 로 감싸지 않는다. 팝업 안 표는 예전처럼 내용 높이(화면 개발 가이드 제5부). 표 끝 구분(같은 날 결정): 테두리 없는 표도 바깥 테두리 상자, 페이징 없는 표는 아래 '총 n건' 한 줄(`DataGrid`·`EditableGrid` 가 알아서, 화면 개발 가이드 제7부).

- 화면 뼈대: `PageFrame` → `SearchForm` → `ResultHeader`(+`ExcelButton`) → `DataGrid`, 조회는 `usePagedSearch`(페이징 없으면 `useSearch`)(`screens/sample/NoticePage.tsx`).
- **검색필터는 공통 필드만 (2026-11-13 사용자 결정, 앞으로 만드는 화면도 같음)** — `SearchForm` `fields` 안의 칸은 `components/search` 팩토리만 쓴다: 사무소 `officeField` · 기번 `mcnField`/`mcnNoField` · 권한 `authField` · 사용여부 `ynField` · 날짜 `rangeField` 등 · 고정 선택지 `optionField` · 검색어 `keywordField` · 검색구분+검색어 `keywordTypeFields` · 다른 칸 값에 따라 켜기 `enabledBy`(`optionFields.tsx`). 화면 파일에 `{ type: 'select' | 'input' }`·로컬 Select/Input 칸을 쓰지 않는다. 맞는 부품이 없으면 **먼저 공통에 추가**하고 `docs/05_공통모듈_목록.md` §3 에 한 줄 적는다(목록을 화면이 만드는 콤보는 `OptionSelect` 에 옵션만 얹음 — `MenuMgtParts.tsx` `TopMenuSelect`).
- 버튼·권한: `ActionBar`(표준 키 `search·excel·print·create·update·delete·save…`)와 `AuthButton need="reg|upd|…"`. 권한 판단은 `usePermission` 한 곳.
  - **조회 버튼은 하나 (2026-11-13 사용자 결정)** — 같은 화면(탭 칸·화면 틀)에 `SearchForm` 이 있으면 `ActionBar` 가 `search` 버튼을 그리지 않는다(검색필터 안 [조회]만, Ctrl+Enter 는 그대로, `components/auth/ActionBar.tsx`). 그래도 `actions` 에 `search` 는 넣는다(단축키용).
- **상세 설명 생략 (2026-11-13 사용자 결정, 표 높이 확보)** — `PageFrame subtitle`·`ResultHeader note`(info) 는 그리지 않는다. 꼭 보일 상태 안내는 `PageFrame alert`(예: 공지 '읽기 전용'), AS-IS 빨간 경고문은 `ResultHeader noteTone="danger"`. 편집 표는 `ResultHeader` 대신 `EditableGrid title` 로 '제목 (n건) · 변경 내역 · 버튼'을 한 줄에(`AuthMgtParts.tsx` 메뉴 권한).
- **사용여부 칸은 공통 `usedCol`** (2026-11-17 사용자 결정) — '사용' 초록·'미사용' 주황 태그(`components/grid/columns.tsx`, 상세 팝업은 `UseYnTag`). 화면에서 사용여부를 따로 그리지 않는다.
- 엑셀: `downloadExcel(excelColumnsOf(columns), rows, '파일명')` — 화면 컬럼 정의로 만든다. 엑셀 전용 컬럼 목록을 따로 만들지 않는다. `downloadExcel` 이 내려받기 전에 사용기록(E)을 서버에 알린다(2026-11-17) — 엑셀은 꼭 이 함수로.
- **확인 팝업은 하나 (2026-11-16 사용자 결정)** — 삭제·승인 등 묻는 창은 공통 `useConfirm`(`components/feedback/useConfirm.tsx`: 화면 가운데, [취소][동작 이름], 삭제 = 빨강·처음 포커스 [취소]). `ActionBar` 의 `confirm` 도 이것을 쓴다. 화면에서 `Popconfirm`·`Modal.confirm`·`window.confirm` 을 직접 쓰지 않는다(게시·iframe 에서 `window.confirm` 은 막혀 무반응).
- **같은 기능을 화면마다 다시 만들지 않는다 (2026-11-16 사용자 결정)** — 두 화면 이상에서 쓰면 공통(`components/*`·`utils/*`)으로 올린 뒤 쓴다(예: `fmtPerson`, `trimOrUndef`, `FormModal`). 새로 만든 공통은 `docs/05_공통모듈_목록.md` 에 한 줄.
- 입력 팝업: 틀은 공통 `FormModal` + `FormLoadGate`(수정 원본 읽기) + `FormModalFooter`([닫기][저장])(`components/display/FormModal.tsx`, 2026-11-16), 안쪽 폼 컴포넌트는 열 때마다 새로 그림, 검사는 파일 위 `RULES … satisfies Record<…>` + `formItem(name, control, RULES.x)`(`screens/popup/NoticeEditModal.tsx`). 규칙 함수는 `src/utils/rules.ts`, 문구는 `src/config/messages.ts` `VMSG`·`CMSG` 에서만.
- API 호출은 `src/api/endpoints/<area>.ts` 에서만. 화면에서 axios·fetch 직접 금지.
- 성공·오류 문구는 서버가 만들고 자동으로 뜬다 → 화면에 `message.success` 금지, API 주위 `try/catch` 금지. 쓰기 `onSuccess` 는 `qc.invalidateQueries({ queryKey: <area>Keys.all })` 만(`docs/00_설계.md` §6.3).
- 포맷은 `src/utils/format.ts` 만(로컬 재정의는 lint 가 막음, `eslint.config.js`).

### 백엔드 핵심 규칙 (`docs/00_설계.md` §5.1~§5.2, `web/notice/**`)

- 컨트롤러: 클래스 `@RequestMapping` 없이 **메서드마다 전체 경로**(`@GetMapping("/api/notice")`), 반환 `ApiResponse<T>`.
  - **메서드 이름 = 사용기록 정책 (2026-11-17 사용자 결정)** — API 메서드 이름은 `select`·`insert`·`update`·`delete` 로 시작(→ 사용기록 R·C·U·D, 일괄 저장은 `updateBatch`). 기록하지 않을 API(화면이 자동으로 부르는 콤보·메뉴 등)는 `@ExcludeCrudLog("이유")`(`web/common/uselog/ExcludeCrudLog.java`). 둘 다 아니면 `CrudLogNamingGuardTest` 가 빌드를 막는다. 기록·실행 SQL 잡기는 자동(`web/common/uselog/CrudLogAspect.java`, 계약 §12-1) — 화면 코드에서 할 일 없음.
  - 조회조건 `XxxSearchDto extends PageRequest` 를 `@ModelAttribute`, 본문 `@RequestBody @Valid`.
  - 쓰기 성공 문구는 코드로: `ApiResponse.ok(data, "S002")`, 값이 들어가면 `ApiResponse.ok(r, "S005", r.getCount())`.
- 서비스: 클래스 `@Transactional(readOnly = true)`, 쓰기 메서드 `@Transactional`. 변경 로그 `log.info("[업무] … user={}", CurrentUser.id())`.
- 오류: `BusinessException("<코드>")` 400, `NotFoundException` 404, `ForbiddenException` 403.
- 문구는 `Backend/nh-atms-api/atms-common/src/main/resources/api-message/api-messages.tsv` 의 **코드로만**. 응답·예외에 한국어 문구를 쓰지 않는다(`ApiMessageCodeGuardTest` 가 막음). 새 코드는 TSV 에 한 줄(코드·S/E·문구·쓰는 곳, 탭 구분).
- DTO 는 감사 컬럼이 있는 테이블이면 `extends AuditFields`(`atms-common/audit/AuditFields.java`).
- 클래스 단순 이름은 `kr.co.nh.atms.web` 전체에서 유일(`TypeAliasUniquenessTest`). 예: 공통 조회 DTO 가 `LookupOffice`·`LookupModel` 인 이유.
- 매퍼 XML 은 `MapperXmlLoadTest` 가 DB 없이 문법·id·resultType 을 검사한다.
- 설정 키는 `atms.<업무>.<키>`, 기본값은 `@Value("${atms.x.y:기본}")` 으로(yml 은 리드만).

## 병렬 에이전트 — 파일 소유 (`agent-briefs/README.md`)

- 에이전트는 **자기 묶음 파일만** 만든다·고친다.
  - 프론트: `screens/<slug>/{Name}Page.tsx`(+`{Name}Parts.tsx`·`{Name}Columns.tsx`), `screens/popup/{Name}Modal.tsx`, `domain/<area>.ts`, `api/endpoints/<area>.ts`.
  - 백엔드: `web/<업무>/**`, `mapper/<업무>/*.xml`, `src/test/java/kr/co/nh/atms/web/<업무>/**`.
- **리드만** 고치는 공유 파일: `screens/{screenIds,registry,navigation}.ts`, `types/*`, `components/*`, `hooks/*`, `utils/*`, `config/*`, `stores/*`, `theme/*`, `web/common/**`, `atms-common/**`, `api-messages.tsv`, `application*.yml`, `build.gradle`, `db/**`, `docs/**`, `CLAUDE.md`, `agent-briefs/**`.
- 에이전트는 공유 파일에 넣을 줄을 **보고서에 붙여 넣을 수 있는 글자 그대로** 적는다. 리드가 합친다.
- 에이전트는 `git commit`·`push` 를 하지 않는다(리드가 한다).

## 커밋 규칙

- 커밋 전에 프론트·백엔드 검사를 둘 다 통과시킨다. 결과(통과/실패, 테스트 수)를 보고에 적는다.
- 프론트(타입검사 + lint, `Frontend/nh-atms-web/package.json` `check` — 프론트 테스트는 2026-11-11 삭제):

```bash
cd Frontend/nh-atms-web
```

```bash
npm run check
```

- 백엔드(컴파일 + 테스트, DB 테스트 `@Tag("db")` 는 자동 제외). 폐쇄망 PC 는 끝에 `--offline` 을 붙인다(Windows 는 `gradlew.bat`):

```bash
cd Backend/nh-atms-api
```

```bash
sh gradlew build --no-daemon
```

- `db/` 를 건드렸으면 저장소 맨 위에서 생성물 확인(종료코드 0 이어야 함, `db/ddl/README.md`):

```bash
python3 -I db/tools/gen_ddl.py --check
```

- 중간 저장은 `wip:` 커밋으로 자주 하고 **항상 푸시**한다. 완성 커밋은 `feat:`·`fix:`·`docs:` 처럼 앞말을 붙인다(`git log` 참고).
- 작업은 세션이 정해 준 브랜치에서 한다. 정해진 게 없으면 main 에서 새 브랜치를 딴다. 강제 푸시하지 않는다.
- 커밋에 넣지 않는 것: `node_modules/`, `dist*/`, `build/`, 스크린샷(`Frontend/nh-atms-web/docs/screenshots/`) — `.gitignore`.

## 실행 (자세한 것은 `docs/01_실행_배포.md`)

- 프론트 + 내 PC 백엔드 (`Frontend/nh-atms-web` 에서, 목 서버는 2026-11-11 삭제)(`DEV_PROXY_TARGET=http://localhost:8080`, `Frontend/nh-atms-web/.env.development`):

```bash
npm run dev
```

- 백엔드(로컬 프로필, `D:\local-db.yml` 에 DB 계정 필요. 사용자는 화면에서 로그인):

```bash
cd Backend/nh-atms-api
```

```bash
sh gradlew :atms-web:bootRun --offline
```

- 개발서버용 WAR(JEUS, JNDI `dsATMS01`):

```bash
sh gradlew bootWar -Pjeus --offline
```

- **API 경로 `/api/web/` (2026-11-10, 사용자 결정 — WebtoB VHOST/URI 구성에 맞춤):** atms-web 컨트롤러는 모두 `/api/web/...`, 화면은 `VITE_API_BASE=/api/web`(env.ts 기본값도). 공통 `SecurityConfig` 의 허용 경로는 `/api/web/auth/**`(그 밖 `/api/**` 는 인증), `SessionUserFilter`(옛 `FixedUserFilter`) 는 `/api/web/` 만 보고 `/api/web/auth/` 는 건너뜀. 새 API 는 `/api/web/` 아래에 만든다.
