> 코드관리 화면(2026-11-16) + **공통 확인 팝업·입력 팝업 틀 통일 + 중복 코드 정리 + 공통 콤보 TB_CODE_DTL 이전**.
> - **반입은 파일 단위**: 부록 A 의 새 파일은 통째로, diff 파일은 이 부 관련 줄만 바뀐 것(전체본을 덮어써도 됨). 부록 B = SHA-512.
> - 제8부(대시보드)까지 넣은 뒤 넣습니다.
> - 경로 줄임: src/ = Frontend/nh-atms-web/src/, web/ = Backend/nh-atms-api/atms-web/src/main/java/kr/co/nh/atms/web/, mapper/ = Backend/nh-atms-api/atms-web/src/main/resources/mapper/

## 0. 한눈에 보기

- 사용자 결정(2026-11-16)
  1. 코드관리 = **최종 프로토타입대로**: 왼쪽 코드구분(체크·순번·코드구분·코드명·사용여부), 오른쪽 상세코드(체크·순번·코드·코드명·**데이터1~5 모두, 칸 이름 고정**), 등록·수정·상세는 팝업, 권한 = 메뉴 권한 플래그, 엑셀 없음.
  2. 코드구분은 **마스터·상세 모두 VARCHAR2(10)**(사진 판독 2자리는 오독). 상세의 코드구분으로 마스터를 찾는다.
  3. **확인 팝업은 하나**: 화면 가운데, [취소][동작 이름], 삭제 = 빨강. 화면마다 말풍선·버튼 글자가 달랐던 것을 공통 `useConfirm` 하나로.
  4. **같은 기능 다른 코드 금지**: 입력 팝업 틀·작은 함수를 공통으로 올리고 기존 화면도 바꾼다(벤더 모듈은 그대로).
  5. 업무구분·처리구분은 값 목록이 없어 **직접 입력**(잠정 Q-28).
  6. 공통 코드 콤보는 삭제 예정 `TB_CO_CODE` 대신 **`TB_CODE_DTL`**(Q-10 확정).

```text
코드관리(sample.codeMgt)
 ├ 왼쪽  코드구분 TB_CODE_MASTER  [상세][등록][수정][삭제]
 │        행 클릭 → 오른쪽 · 두 번 클릭/[상세] → 상세 팝업(업무·처리구분·설명1~5) → [수정]
 └ 오른쪽 상세코드 TB_CODE_DTL (CODE_SECT 로 연결)  [등록][수정][삭제]
          두 번 클릭 → 상세 팝업 → [수정]

확인 팝업: 모든 [삭제] → useConfirm ─ 가운데 「선택한 2건의 상세코드를 삭제하시겠습니까?」 [취소] [삭제]
입력 팝업: FormModal(틀) → FormLoadGate(수정 원본 읽기) → 폼 → FormModalFooter([닫기][저장])
```

| 정리한 중복 | 전 | 후 |
|---|---|---|
| 삭제·확인 창 | ActionBar 말풍선 [취소][확인], 공지 상세 말풍선(기본 글자), 첨부 말풍선 [취소][삭제](빨강 아님), 견본 [아니오][예] | 공통 `useConfirm` 하나 — 가운데 [취소][삭제](빨강) |
| 입력 팝업 틀 | 권한관리·메뉴관리·공지 팝업이 Modal·제목·로딩·실패 안내·[닫기][저장]을 각자 복사 | `FormModal`·`FormLoadGate`·`FormModalFooter` |
| 작성자 표시 | 공지 상세(`writerOf`)·공지 입력(직접 문자열) 따로 | `fmtPerson` |
| 공백 떼고 비면 없음 | `trimOpt`(기기정보)·`?.trim() \|\| undefined`(권한·검색 팝업 2곳)·`keywordOf` 따로 | `trimOrUndef` |
| 날짜 포맷(백엔드) | 대시보드가 `yyyyMMdd(HHmmss)` 를 다시 만듦 | 공통 `Dates.YMD`·`Dates.DT14` |

## 1. 준비 — DB

- 테이블 2개는 **판독본 참고 DDL**(`db/ddl/TB_CODE_MASTER.sql`·`TB_CODE_DTL.sql`) — 개발 DB 에 있는지 `db/check/01_exists.sql`·`02_columns.sql` 로 먼저 확인(컬럼 수 14 / 12, `CODE_SECT` 가 10자리인지).
- 메뉴: `db/change/code_mgt_menu.sql` 을 `menu_three_level.sql` 다음에 한 문장씩, 마지막 COMMIT(메뉴관리 화면에서 등록해도 같음). 권한은 권한관리 → 메뉴 권한 탭(관리자 A 는 없어도 됨).
- 공통 콤보를 쓰는 화면(기기사·권한명 설정을 켠 경우)은 이제 `TB_CODE_DTL` 에 그 코드구분이 있어야 이름이 보입니다.

## 2. 만들 파일 — 빨간줄 없이 가는 순서

| # | 구분 | 파일 | 왜 이 순서 |
|---|---|---|---|
| B1 | diff | `api-messages.tsv` | 서비스가 쓰는 오류 코드 CDM_001~005 |
| B2~B8 | 새 | `web/codemgt/dto/*` 7개 | 매퍼·서비스가 부름 |
| B9~B10 | 새 | `CodeMgtMapper.java` → `mapper/codemgt/CodeMgtMapper.xml` | DTO 다음 |
| B11~B13 | 새 | `CodeMgtRules` → `CodeMgtService` → `CodeMgtController` | 규칙 → 서비스 → 컨트롤러 |
| B14 | 새 | `CodeMgtServiceTest` | ✅ `gradlew.bat build --offline` |
| B15~B24 | diff | 매퍼 XML 5개(Lookup·AuthMgt·McnInfo·McnModel·Dashboard) + 주석 3개 + `DashboardRules`·`DashboardService` | TB_CO_CODE → TB_CODE_DTL, 날짜 포맷 공통화 — 서로 독립 |
| F1~F3 | diff | `utils/object.ts` · `utils/format.ts` · `config/messages.ts` | 아래 공통 부품·화면이 부름 |
| F4~F5 | 새·diff | `components/feedback/useConfirm.tsx` → `feedback/index.ts` | F6·F9·F13 이 부름 |
| F6 | diff | `components/auth/ActionBar.tsx` | F4 다음 |
| F7~F8 | 새·diff | `components/display/FormModal.tsx` · `components/grid/MasterDetailGrids.tsx` | F14~F16·F24·F25 가 부름 |
| F9~F12 | diff | `FileAttachList` · `optionFields` · `McnSearchModal` · `PersonSearchModal` | F1·F4 다음 |
| F13~F18 | diff | 공지 상세·공지 입력·권한 입력·메뉴 입력 팝업 · `McnInfoParts` · `AuthMessageTab` | F1~F8 다음 |
| F19 | diff | `screens/screenIds.ts` | 코드관리 팝업이 화면키를 씀 |
| F20~F25 | 새 | `domain/codeMgt` → `api/endpoints/codeMgt` → `CodeMgtColumns` → `CodeMgtParts` → `CodeMgtModals` → `CodeMgtPage` | 불리는 쪽 먼저 |
| F26 | diff | `screens/registry.ts` | F25 다음(먼저 넣으면 빨간줄) ✅ `npm run check` |
| D1~D8 | 새·diff | `db/change/code_mgt_menu.sql`, `db/tools/gen_ddl.py`, `db/ddl` 사본·참고 DDL, `db/check` 2개 | 화면 확인 전에 SQL |

## 3. 가져다 쓴 공통 모듈

| 부품 | 파일 | 무엇 |
|---|---|---|
| `useConfirm` **[새]** | `components/feedback/useConfirm.tsx` | `ask({ content: CMSG.deleteSelected(n, '코드구분'), danger: true, onOk })` — 가운데 [취소][삭제]. ActionBar 의 `confirm` 이 자동으로 이것 |
| `FormModal`·`FormLoadGate`·`FormModalFooter` **[새]** | `components/display/FormModal.tsx` | 입력 팝업 틀 · 수정 원본 읽기(스켈레톤·실패 안내) · [닫기][저장](권한) |
| `MasterDetailGrids` (+`columns`·`selected`·`onSelect`) | `components/grid/MasterDetailGrids.tsx` | 좌우 4:8, 삭제·조회 뒤 선택을 바깥에서 비움 |
| `DataGrid` `bordered` | `components/grid/DataGrid.tsx` | 칸 구분선, 남은 높이 채움, '총 n건' |
| `DetailModal` + `DetailGrid` | `components/display/*` | 상세 팝업 |
| `ActionBar` (`open` 라벨 '상세'·create·update·delete) | `components/auth/ActionBar.tsx` | 권한 흐림, 삭제 확인 |
| `keywordField`·`ynField` | `components/search/*` | 조회조건(공통 필드만) |
| `usedCol` · `UseYnTag` **[새]** | `components/grid/columns.tsx` · `components/display/StatusTag.tsx` | 사용여부 '사용'(초록)·'미사용'(주황) — 메뉴관리·기기정보도 같은 칸으로(2026-11-17) |
| `trimOrUndef` · `fmtPerson` **[새]** | `utils/object.ts` · `utils/format.ts` | 폼 → 본문, 작성자 표시 |

## 4. 화면별 설명

### 4-1. 코드관리

- 화면을 열면 코드구분 전체(코드구분 순). 조회조건 = 검색어(코드구분·코드명) · 사용여부. [조회]는 검색필터 안 하나(Ctrl+Enter 도 됨).
- 왼쪽 행 클릭 → 오른쪽에 그 코드구분의 상세코드. 체크는 수정(1건)·삭제(여러 건) 대상 — [수정]은 체크가 없으면 지금 고른 행.
- [삭제] → 공통 확인 팝업 → [삭제]. 상세코드가 남은 코드구분은 서버가 막고 '상세코드가 있는 코드구분은 삭제할 수 없습니다. (MCN_VEND)' 가 뜹니다 — 상세를 먼저 지우거나 사용여부 N.
- 오른쪽 데이터 칸은 '데이터1~5' 모두(칸 이름 고정, 코드구분의 설명1~5 와 연결하지 않음). 데이터4(숫자 -9999~9999)·데이터5(한 글자)는 좁은 칸.

### 4-2. 팝업 4개 (`screens/popup/CodeMgtModals.tsx`)

- 코드구분 상세 → [수정] → 코드구분 수정(키는 보이기만). 등록은 코드구분(공백 없이 10자 이내)부터.
- 상세코드 상세 → [수정] → 상세코드 수정. 코드구분은 왼쪽에서 고른 것.
- 네 팝업 모두 공통 틀만 씁니다 — 새 팝업도 이 파일을 복사해 시작하면 됩니다.

### 4-3. 확인 팝업 통일 (모든 화면)

- 공지사항·메뉴관리·권한관리·기기정보의 [삭제], 공지 상세 [삭제], 첨부파일 삭제가 모두 같은 가운데 팝업으로 바뀝니다. 처음 포커스는 [취소] — Enter 를 눌러도 바로 지워지지 않습니다.
- 앞으로 확인이 필요하면 `useConfirm` 만 씁니다(`Popconfirm`·`window.confirm` 금지 — 게시 페이지에서는 `window.confirm` 이 막혀 아무 반응이 없음).

## 5. 백엔드 요약

- `web/codemgt/**` — 컨트롤러는 메서드마다 전체 경로 `/api/web/code-mgt/...` + `@RequireMenuAuth(screen = "sample.codeMgt")`. 서비스가 검사(`CodeMgtRules`) → 없음 404 `CDM_001`·`CDM_004`, 겹침 400 `CDM_003`·`CDM_005`, 상세 남은 코드구분 삭제 400 `CDM_002`.
- 공통 콤보 `/api/web/lookup/codes/{codeType}`: 응답 모양은 그대로(`codeType`=`CODE_SECT`, `desc1~3`=`DATA1~3`, `sortOrd`=null), 사용 중 = 코드구분 `USED_YN` 이 'N' 이 아닌 것, 코드 순(잠정 Q-29).
- 폐쇄망 개발 DB 확인 필요: 새 SQL 전부(코드관리·공통 콤보·기기사명·권한명 조인).

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

1. 견본 › 견본 화면 › 코드관리 — 왼쪽 행 클릭 → 오른쪽 상세코드, 두 번 클릭 → 상세 팝업 → [수정].
2. 오른쪽 2건 체크 → [삭제] → 가운데 확인 팝업 [삭제] → 'n건 삭제되었습니다.'
3. 상세코드가 있는 코드구분 체크 → [삭제] → 막힘 문구.
4. 메뉴관리·공지사항에서 [삭제] → 같은 가운데 확인 팝업인지.
5. 공지·권한·메뉴 등록 팝업 — 모양·[닫기][저장]이 예전과 같은지(틀만 공통으로 바뀜).
