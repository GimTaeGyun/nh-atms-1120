> **홈 = 대시보드**(2026-11-16 사용자 결정 — 예전 견본 카드 바로가기 삭제). 설계서 26-0000 초기화면을 실제 화면으로.
> - 규칙은 서버 한 곳(`DashboardRules`)과 설정(`atms.dashboard.level.*`)에 모았습니다 — **추후 바뀔 수 있음**(잠정 Q-25·26·27).
> - 반입: 새 파일 백엔드 10개·프론트 5개·DB 4개 + 고칠 파일(diff 6개·교체 1개·삭제 1개). 부록 B = SHA-512.
> - DB: `db/change/dashboard_menu.sql` 을 먼저 돌려야 대시보드 API 가 열립니다(메뉴 권한).
> - 경로 줄임: src/ = Frontend/nh-atms-web/src/, web/ = atms-web …/kr/co/nh/atms/web/, mapper/ = atms-web …/resources/mapper/

## 0. 한눈에 보기

- **비유**: 사무소 현황판입니다. 누가 보느냐(권한코드)에 따라 **볼 수 있는 사무소 범위**가 정해지고, [조회]를 누른 순간의 숫자를 보여 줍니다.
- 사용자 결정(2026-11-16)
  1. 레벨 = 권한관리의 3권한(로그인 개인권한, 없으면 부서권한): **A = 중앙본부, T = 영업본부, L = 영업점**(그 밖 코드 = 영업점).
  2. 내 사무소 = **인사 소속**(`TB_INSA.SAMUSO` → `TB_OFC_RAW.ONLINE_CODE`), 영업본부 소속 영업점 = `TB_OFC_REG_MGT.JUNG_MOJUM`.
  3. 통신단절 = **전원 OFF 만**, 관리모점 = **코너 소속영업점**, 캐릭터 = 빈 자리.
  4. 대시보드를 **메뉴로 등록**해 권한관리 조회 권한으로 막음. 공지는 공지사항 모듈과 연결(제목 = 상세 팝업, 전체보기 = 공지 탭).
  5. 기기상태정보 4행, 엑셀 없음. 판독본 테이블 3개는 참고 DDL 까지.

| 레벨 | 지역 구분 | 사무소 구분 | 처음 조회 범위 |
|---|---|---|---|
| 중앙본부(A) | 켬(전국 + 영업본부들) | 켬(본부를 고르면 소속 사무소) | 전국 |
| 영업본부(T) | 잠금(내 본부) | 켬(전체 + 소속 사무소) | 내 본부 전체 |
| 영업점(L·그 밖) | 잠금 | 잠금(내 사무소) | 내 사무소 |

```text
화면(사진 비율)
 ┌ 사무소명(큰 남색) · 사무소 코드 ─────────── 캐릭터 자리 ┐ ┌ ① 자동화기기 공지사항   전체보기 › ┐
 │ • 지역 구분 [밑줄 콤보]   [🔍 조회] [↻]                   │ │ - 전체 제목 N ……… 26/11/16      │
 │ • 사무소 구분 [밑줄 콤보]                                 │ │ (최신 6건, 제목 = 상세 팝업)      │
 │ [⑦ 운영 대수 54대] [③ 일평균 63건] [⑤ ● 정상 45 ● 장애 3 / ● 보수중 6 ● 통신단절 0] │ └──────────────────┘
 └ ④⑥ 기기상태정보 (4행 보임, 나머지 표 안 스크롤) — 관리점·관리점코드·관리모점·관리모점코드 | 코너명 … 관리업체
```

## 1. 준비 — DB

1. `db/check/01_exists.sql` · `02_columns.sql` — `TB_OP_ATM_STAT`(기기상태)·`TB_OP_DEAL_OFCSUM`(거래집계)·`TB_EE_MCN_ERR_HIST`(장애이력)가 개발 DB 에 있는지, 컬럼이 판독본과 같은지 먼저 봅니다. 세 테이블은 정본 CSV 사본에 없어 **사진 판독본 행**으로 참고 DDL 을 만들었습니다(`db/ddl/*.sql` 경고 줄).
2. `db/change/dashboard_menu.sql` — 대메뉴 `DSB0000` '대시보드'(상단 첫 칸) → `DSB0100` → `DSB0101` `/main/dashboard`, 권한 A·T·L 에 조회. **한 문장씩, 마지막 COMMIT**, `<내 개인번호>` 바꿔서. 안 돌리면 홈에서 '권한이 없습니다'(403).

## 2. 만들 파일 — 빨간줄 없이 가는 순서

### 2-1. 백엔드

| # | 구분 | 파일 | 왜 이 순서 |
|---|---|---|---|
| B1 | diff | `atms-common/.../api-messages.tsv` | 서비스가 던질 `DSH_001` 먼저 |
| B2~B5 | 새 | `web/dashboard/dto/*` 4개 | DTO 는 다른 대시보드 파일을 부르지 않음 |
| B6 | 새 | `web/dashboard/service/DashboardRules.java` | 순수 규칙(아무것도 안 부름) |
| B7 | 새 | `web/dashboard/mapper/DashboardMapper.java` | DTO 를 씀 |
| B8 | 새 | `mapper/dashboard/DashboardMapper.xml` | B7 의 SQL |
| B9 | 새 | `web/dashboard/service/DashboardService.java` | B2~B8 + 공지 `NoticeService`(제6부) 를 씀 |
| B10 | 새 | `web/dashboard/controller/DashboardController.java` | 서비스를 씀 |
| ✅ | | `gradlew.bat compileJava --offline` | |
| B11 | 새 | `src/test/.../dashboard/DashboardServiceTest.java` | 테스트는 마지막 |
| ✅ | | `gradlew.bat build --offline` | |

### 2-2. 프론트

| # | 구분 | 파일 | 왜 이 순서 |
|---|---|---|---|
| F1 | diff | `screens/screenIds.ts` | 화면키 `main.dashboard` |
| F2 | 새 | `domain/dashboard.ts` | 타입 |
| F3 | 새 | `api/endpoints/dashboard.ts` | F2·공지 타입을 씀 |
| F4 | 새 | `screens/main/DashboardParts.tsx` | F2 + 공지 `NoticeParts`(N 표시) |
| F5 | 새 | `screens/main/dashboard.css` | 모양 |
| F6 | 새 | `screens/main/DashboardPage.tsx` | F3~F5 + 공통 `OptionSelect`·`DataGrid`·공지 `NoticeViewModal` |
| F7 | diff | `screens/registry.ts` | F6 등록 |
| F8 | 교체 | `screens/home/HomePage.tsx` | 홈이 F6 을 그림 |
| F9 | 삭제 | `screens/home/home.css` | 쓰는 곳 없음 |
| ✅ | | `npm run check` | |

### 2-3. DB 폴더

| # | 구분 | 파일 | 하는 일 |
|---|---|---|---|
| D1 | 새 | `db/change/dashboard_menu.sql` | 메뉴·권한 행(§1-2) |
| D2·D3 | diff | `db/check/01_exists.sql`·`02_columns.sql` | 확인 대상 3개 추가 |
| D4 | diff | `db/tools/gen_ddl.py` | 생성 대상 3개 추가 |
| D5~D7 | 새 | `db/ddl/TB_OP_ATM_STAT.sql` 등 3개 | 참고 DDL(생성물, 판독본) |

- `db/ddl/_source_columns.csv`·`_source_entities.csv` 에 판독본 행이 더해졌습니다(반입 목록에 있음, 이 부록에는 생략 — 길어서).

## 3. 가져다 쓴 공통 모듈

| 부품 | 파일 | 어디에 |
|---|---|---|
| `OptionSelect` | `components/search/optionFields.tsx` | 지역·사무소 구분 콤보(전국·전체 맨 위, 검색, 잠금) — CSS 로 밑줄형 |
| `DataGrid` (`height`, `clientPageSize={0}`, `bordered`) | `components/grid/DataGrid.tsx` | 기기상태정보 4행 높이 + 칸 세로 구분선 + '총 n건' |
| `ResultHeader` (`title`, `total`, `leftActions`) | `components/page/ResultHeader.tsx` | '기기상태정보 (n건)' + 상태 거름 꼬리표 |
| `NoticeViewModal` | `screens/popup/NoticeViewModal.tsx` | 공지 상세(수정·삭제 숨김, 이전·다음 글) |
| `isNewNotice`·`NEW_DAYS` | `screens/sample/NoticeParts.tsx` | 공지 N 표시 |
| `useOpenScreen` | `screens/navigation.ts` | [전체보기] = 공지사항 탭 |
| `NoticeService` (서버) | `web/notice/service/NoticeService.java` | 공지 목록·상세(보이는 범위 규칙 그대로) |

## 4. 화면별 설명

### 4-1. 사무소 선택 · [조회]

- 콤보 처음 값 = 서버 `scope` 의 `defaultRegion`·`defaultOfcCode`. 손대지 않으면 그 값으로 한 번 자동 조회합니다.
- 콤보를 바꾸면 '조건이 바뀌었습니다 — [조회]를 누르면 반영됩니다' 만 보이고 숫자는 그대로. **[조회]를 눌러야** 다시 읽습니다(새로고침 버튼 없음). [↻] = 콤보를 처음 값으로.
- 중앙본부는 지역을 고르기 전에는 사무소 구분이 잠깁니다(전국 = 사무소 '전체').

### 4-1-1. 바로가기 (2026-11-17 추가)

- 사무소명은 왼쪽, 캐릭터 이미지(**280×160**, 사진 비율)와 **바로가기 가로 3칸**은 **오른쪽 끝 정렬**(기기상태 상자 오른쪽 끝과 같은 선, 2026-11-17)(칸당 72px, 높이 = 이미지 160px, 시안 C: 위쪽 파란 띠·이름 + 설명 한 줄).
- 지역/사무소 콤보 바로 아래에 운영대수 카드를 붙였다(간격 최소, 카드 높이 = 상태 상자, '상세보기 ›' 는 상태 상자 위 빈 곳에 겹침) → 남는 높이는 아래 기기상태정보 표가 가져감.
- 대상 = **공지사항 · 기기정보 · 코드관리**(2026-11-17 사용자) — `DashboardParts.tsx` `QUICK_LINKS` **한 곳**(`{ screenKey, label, desc }`, `null` = 흐린 '바로가기 · 대상 미정' 칸).
- 누르면 그 화면 탭이 열린다. 조회 권한이 없는(메뉴에 없는) 화면은 흐리게, 누르면 '권한이 없습니다.'

### 4-2. 카드 · 상태 박스

- ⑦ 총 운영 대수 = 범위 안 기기 수, 'YYYY-MM-DD HH:MM 기준' = [조회] 시각.
- ③ 일평균 거래량 = 전 영업일 거래 건수 합 ÷ 운영 대수, 'YYYY-MM-DD (전 영업일) 기준'.
- ⑤ 2×2 상태(정상·장애 / 보수중·통신단절). **칸마다 회색 테두리 상자**(바깥 큰 상자 없음, 2026-11-16 사용자 결정), 누른 칸은 파란 테두리 + 옅은 파랑. 칸을 누르면 아래 표가 그 상태만, 한 번 더 또는 표 머리 '장애만 ✕' 꼬리표(✕ 만이 아니라 꼬리표 어디를 눌러도·Enter 도 풀림)·'상세보기 ›' = 전체.

### 4-3. 공지사항

- 최신 6건 '- [전체] 제목 [N] …… 26/11/16'. 전체 공지(사무소 빈 것)만 '전체' 꼬리표, 앞의 '-' 는 한 번만(프로토타입의 '- - 전체' 중복 수정).
- 제목 = 공지 상세 팝업(조회수 +1, 이전·다음 글, **대시보드에서는 수정·삭제 없음**), [전체보기] = 공지사항 화면 탭.

### 4-4. 기기상태정보

- 범위 안 기기 전체, 4행 높이(나머지 표 안 스크롤), 엑셀 없음.
- **공통 모듈만**(2026-11-16 사용자 결정): 머리 = `ResultHeader`('기기상태정보 (n건)' + 상태 거름 꼬리표), 표 = `DataGrid bordered`(모든 칸 세로 구분선 + 바깥 테두리). 화면 전용 표 CSS(탭 모양 머리·파란 윗선·관리모점코드 뒤 나눔선)는 없앰.
- 컬럼 순서 = 설계서: 관리점·관리점코드·관리모점·관리모점코드 | 코너명·기번·기종·제조사·모델명·기기상태·기기상태상세·주소·관리업체.

## 5. 백엔드 요약

- `DashboardRules` — `levelOf`(권한코드 → 레벨, 설정 대응표), `statusOf`(전원 OFF → 중지 → 장애 → 정상), `prevBizDay`(토·일·휴일 건너뜀, 최대 14일), `dealAvg`.
- `DashboardService` — 레벨별 범위 강제(영업본부 = 지역 고정, 영업점 = 사무소 고정, 범위 밖 403 `DSH_001`), 요약 계산, 판정용 원천 값은 응답에서 비움. 공지 = `NoticeService.search`(6건)·`get(no, true)`.
- 매퍼 `scope` 조각: 사무소 = 관리점 또는 관리모점이 그 사무소, 본부 = 관리점 또는 관리모점이 본부 소속(본부 자신 포함).
- 컨트롤러 5개 모두 `@RequireMenuAuth(screen = "main.dashboard", need = QRY)`.

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

화면에서 눌러 볼 순서(`dashboard_menu.sql` 뒤)

1. 권한 A 사용자: 홈 = 대시보드, '전국', 지역 구분 켜짐 → 본부를 고르면 사무소 구분 켜짐 → [조회] 로만 숫자가 바뀌는지.
2. 권한 T 사용자: 지역 구분이 내 본부로 잠김, 사무소 구분에서 소속 사무소를 골라 [조회].
3. 권한 L 사용자: 두 콤보 잠김, 내 사무소 숫자만.
4. 상태 칸 누르기 → 표 거름 / 공지 제목 → 상세 팝업 / [전체보기] → 공지사항 탭.

- 새 SQL(범위·기기·거래·휴일)과 판독본 테이블 3개는 **폐쇄망 개발 DB 에서 처음 확인**합니다. 이 저장소에서는 매퍼 문법(`MapperXmlLoadTest`)과 서비스 규칙 테스트 6개만 확인했고, 화면은 가짜 API 로 레벨별로 눌러 확인했습니다.
