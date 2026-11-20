> 메뉴관리 화면(`sample.menuMgt`)을 빈 상태에서 처음부터 만든다고 보고 무엇을 어떤 순서로 만드는지 정리(2026-11-12).
> - 화면 설명은 화면·팝업 단위(§4), 프론트를 자세히. 가져다 쓴 공통 모듈은 §3.
> - 부록 A 는 §2 순서대로 새 파일 = 전체 소스, 기존 파일 = diff(메뉴관리 관련 줄만). 부록 B = SHA-512.
> - 경로 줄임: src/ = Frontend/nh-atms-web/src/, web/ = Backend/nh-atms-api/atms-web/src/main/java/kr/co/nh/atms/web/, mapper/ = Backend/nh-atms-api/atms-web/src/main/resources/mapper/

## 0. 한눈에 보기

- **무엇을 하는 화면인가** — 업무시스템 메뉴(`TB_SCR_MENU`)를 등록·수정·삭제하는 화면입니다. 비유하면 "건물 안내판 관리": 층(대메뉴) → 구역(중메뉴) → 방(소메뉴 = 실제 화면).
  - **대메뉴** = 앱 상단 메뉴(예: 종합관리), **중메뉴** = 좌측 메뉴(예: 장애목록), **소메뉴** = 좌측 메뉴 아래 항목 = 실제 화면.
- 사용자 결정(2026-11-12): 관리 방식 **D 목록 + 팝업**(공지사항 견본 모양), 채번 **① 상위 접두어 + 순번**, 팝업 상위 고르기 **① 대 → 중 연쇄 콤보**, 삭제 = **물리 삭제 + 권한별 메뉴 행도 함께**, 기존 견본 메뉴는 **이관 SQL** 로 새 규칙에 맞춤, 화면 위치 = 견본 아래 + 홈 카드.

| 화면·팝업 | 테이블 | 모양(복사 견본) | 하는 일 |
|---|---|---|---|
| 메뉴관리 화면 | `TB_SCR_MENU` | 공지사항(목록 + 팝업) — 단, 페이징 없이 트리 순서 전체 | 조회조건으로 거른 메뉴 목록, 체크 → 수정·삭제, 엑셀 |
| 메뉴 등록·수정 팝업 | `TB_SCR_MENU` | 공지사항 입력 팝업 + 연쇄 콤보 | 레벨 → 대메뉴 → 중메뉴 고르기, 메뉴ID 미리보기, 메뉴명·경로·순서·사용 |

**메뉴ID 규칙 (업무 규칙)**

1. 메뉴ID = **영문 대문자 3 + 중메뉴 번호 2 + 소메뉴 번호 2** (7자, `MENU_ID VARCHAR2(10)`).

   | 예 | 레벨 | 타입 | 상위 | 경로 |
   |---|---|---|---|---|
   | `SYS0000` | 대메뉴(상단) | G 그룹 | `'0'` | 없음 |
   | `SYS0300` | 중메뉴(좌측) — 시스템관리의 3번째 중메뉴 | G 그룹 | `SYS0000` | 없음 |
   | `SYS0312` | 소메뉴(화면) — 그 중메뉴의 12번째 소메뉴 | M 화면 | `SYS0300` | `/{slug}/{kebab}` 필수 |

2. 번호는 **같은 상위 안에서 가장 큰 번호 + 1**(01~99). 지운 번호는 다시 쓰지 않습니다.
3. 화면에 **보이는 순서는 `SORT_NO`** 로 따로 정합니다(번호 ≠ 순서). 순서를 바꿔도 메뉴ID 는 그대로.
4. 레벨은 새 컬럼 없이 **메뉴ID 모양**으로 판단합니다(`…0000` 대, `…00` 중, 그 밖 소). 모양이 다른 예전 ID 는 "규칙 밖".
5. 대메뉴만 접두어 3자를 사람이 정합니다(이미 쓰는 접두어면 저장 안 됨). 중·소메뉴 ID 는 전부 자동.
6. 상위는 메뉴ID 에 들어 있어 **수정에서 바꿀 수 없습니다**(PK). 옮기려면 새로 등록하고 예전 것은 '미사용'.
7. 하위가 있는 메뉴는 삭제할 수 없습니다(`MNU_001`). 삭제하면 그 메뉴의 권한별 메뉴(`TB_SCR_AUTH_MENU`) 행도 함께 지워집니다.

```text
[메뉴관리 화면] ─ GET  /api/web/menu-mgt/menus ──────────▶ MenuMgtController ─▶ MenuMgtService.search ─▶ MenuMgtMapper.selectAll ─▶ [TB_SCR_MENU]
     │                                   ◀── 트리 순서 + 레벨·하위 수·상위 경로 ──┘ (서버가 펴고 거름)
[등록 팝업] ── POST /api/web/menu-mgt/menus ───────────▶ create: 상위 확인 → MenuMgtRules.nextId(MAX+1) → insertMenu
[수정 팝업] ── PUT  /api/web/menu-mgt/menus/{menuId} ──▶ update: 이름·경로·순서·사용만
[삭제 버튼] ── POST /api/web/menu-mgt/menus/delete ────▶ delete: 하위 검사 → deleteAuthMenus(TB_SCR_AUTH_MENU) → deleteMenus
     └ 쓰기 성공 뒤: 목록 다시 읽기 + 왼쪽 메뉴(GET /api/web/menu/my) 다시 읽기
```

## 1. 준비 — DB

| 테이블 | 쓰는 컬럼 | 읽기/쓰기 | 비고 |
|---|---|---|---|
| `TB_SCR_MENU` 업무시스템메뉴 | `MENU_ID`(PK), `PARENT_MENU_ID`, `MENU_NM`, `MENU_TYPE`, `MENU_PATH`, `SORT_NO`, `USE_YN`, 감사 4컬럼 | 읽기·쓰기 | 메뉴ID 3단계 규칙 |
| `TB_SCR_AUTH_MENU` 권한별메뉴관리 | `MENU_ID` | 지우기만 | 메뉴를 지울 때 그 메뉴의 행도 함께 |

1. 테이블은 이미 있습니다(`db/check/01_exists.sql`·`02_columns.sql` 로 확인 — 새로 만들지 않음).
2. **처음 한 번 — 이관 SQL** `db/change/menu_three_level.sql`(부록 A D1)을 **한 문장씩** 돌립니다.
   - 예전 견본 메뉴 `SMP000`(그룹) · `SMP001~`(화면) → `SMP0000`(대) · `SMP0100` '견본 화면'(중, 새로) · `SMP0101~`(소).
   - 권한별 메뉴 행(`TB_SCR_AUTH_MENU`)의 메뉴ID 도 새 ID 로 바꿉니다. 화면 권한 검사는 `MENU_PATH` 로 메뉴를 찾으므로 경로가 같으면 권한은 그대로입니다.
   - 마지막에 메뉴관리 메뉴 행(`/sample/menu-mgt`, '견본 화면' 아래 다음 번호)을 넣습니다.
   - `<내 개인번호>` 는 실행하는 사람 개인번호로 바꿉니다. 0) · 7) 의 SELECT 로 전후를 비교하고 맨 끝 `COMMIT;`.
   - **폐쇄망 개발 DB 에서 처음 확인**합니다(이 클라우드에서는 Oracle 에 붙을 수 없음). 다시 돌려도 같은 결과가 되도록 `NOT EXISTS`·`REGEXP_LIKE` 조건을 걸어 두었습니다.
3. 관리자(`AUTH='A'`)는 메뉴 권한 행이 없어도 전부 허용입니다. 그 밖의 권한코드는 **권한관리 화면 → 메뉴 권한 탭**에서 메뉴관리 메뉴를 체크해 줍니다.

## 2. 만들 파일 — 빨간줄 없이 가는 순서

> **원칙: 불리는 파일을 먼저, 부르는 파일을 나중에.** 예를 들어 서비스가 DTO·규칙·매퍼를 부르면 DTO → 규칙 → 매퍼 → 서비스 순입니다.
> 순서대로 만들면 각 파일을 저장하는 순간 IDE 빨간줄(없는 클래스·타입)이 생기지 않습니다. 단계 끝의 ✅ 에서 한 번 확인합니다.
> - **새** = 부록 A 의 전체 소스를 그 경로에 새 파일로 만듭니다(UTF-8, 줄끝 LF).
> - **diff** = 기존 파일에 부록 A 의 diff 대로 `-` 줄을 지우고 `+` 줄을 넣습니다(앞뒤 공백 줄은 위치 찾기용). **메뉴관리 관련 줄만** 보여 줍니다 — 제1부(권한관리)는 이미 반영됐다고 봅니다.
> - 번호는 부록 A 의 제목 번호와 같습니다.

### 2-1. 백엔드 (`Backend/nh-atms-api`)

| # | 구분 | 파일 | 왜 이 순서 |
|---|---|---|---|
| B1 | diff | `atms-common/.../api-message/api-messages.tsv` | 서비스가 쓸 오류 코드(`MNU_004`~`009`)를 먼저. 빠지면 `ApiMessageCodeGuardTest` 실패 |
| B2 | 새 | `web/menumgt/dto/MenuMgtRow.java` | DTO 는 다른 메뉴관리 파일을 부르지 않음 → 맨 먼저(공통 `AuditFields` 만 씀) |
| B3 | 새 | `web/menumgt/dto/MenuMgtSearchDto.java` | |
| B4 | 새 | `web/menumgt/dto/MenuMgtSave.java` | |
| B5 | 새 | `web/menumgt/dto/MenuMgtDeleteRequest.java` | |
| B6 | 새 | `web/menumgt/dto/MenuMgtDeleteResult.java` | |
| B7 | 새 | `web/menumgt/service/MenuMgtRules.java` | 채번 규칙 — 공통 `BusinessException` 만 씀. 서비스·테스트가 부름 |
| B8 | 새 | `web/menumgt/mapper/MenuMgtMapper.java` | `MenuMgtRow` 를 씀 → B2 다음 |
| B9 | 새 | `mapper/menumgt/MenuMgtMapper.xml` | B8 의 SQL(XML 은 빨간줄 대상 아님, `MapperXmlLoadTest` 가 검사) |
| B10 | 새 | `web/menumgt/service/MenuMgtService.java` | DTO·규칙·매퍼를 모두 씀 |
| B11 | 새 | `web/menumgt/controller/MenuMgtController.java` | 서비스를 씀 → 맨 끝 |
| ✅ | | `gradlew.bat compileJava --offline` | 여기까지 컴파일 오류 0 |
| B12 | 새 | `src/test/.../menumgt/MenuMgtRulesTest.java` | 테스트는 본 코드 다음 |
| B13 | 새 | `src/test/.../menumgt/MenuMgtServiceTest.java` | |
| ✅ | | `gradlew.bat build --offline` | 테스트까지 통과 |

### 2-2. 프론트 (`Frontend/nh-atms-web/src`)

| # | 구분 | 파일 | 왜 이 순서 |
|---|---|---|---|
| F1 | diff | `config/messages.ts` | 팝업이 쓸 검사 문구 `VMSG.menuPath` 를 먼저(없으면 F7 에 빨간줄) |
| F2 | diff | `screens/screenIds.ts` | 화면키 `sample.menuMgt`·`popup.menuEdit` — 팝업·화면이 이 키를 타입으로 씀 |
| F3 | 새 | `domain/menuMgt.ts` | 타입. 다른 메뉴관리 파일을 부르지 않음 |
| F4 | 새 | `api/endpoints/menuMgt.ts` | F3 타입을 씀 |
| F5 | 새 | `screens/sample/MenuMgtColumns.tsx` | F3 타입을 씀(레벨 옵션·메뉴ID 표시를 F6·F7 이 씀) |
| F6 | 새 | `screens/sample/MenuMgtParts.tsx` | F4·F5 를 씀 |
| F7 | 새 | `screens/popup/MenuEditModal.tsx` | F1·F2·F4·F5·F6 을 씀 |
| F8 | 새 | `screens/sample/MenuMgtPage.tsx` | F5·F6·F7 을 씀 |
| F9 | diff | `screens/registry.ts` | F8 화면 파일을 등록 → F8 다음 |
| F10 | diff | `screens/menuSeed.ts` | 내장 메뉴 3단계 + 메뉴관리(F2 키 사용) |
| F11 | diff | `screens/home/HomePage.tsx` | 홈 카드(F2 키 사용) |
| ✅ | | `npm run check` | 타입검사·lint 오류 0 |

### 2-3. DB 이관 SQL

| # | 구분 | 파일 | 하는 일 |
|---|---|---|---|
| D1 | 새 | `db/change/menu_three_level.sql` | 견본 메뉴를 3단계 메뉴ID 로 옮기고 메뉴관리 메뉴 행을 넣음(§1-2) |

- 문서 `docs/07_API_계약.md` §9 는 계약(정본)이라 코드보다 먼저 읽어 둡니다(코드 빨간줄과는 무관).

## 3. 가져다 쓴 공통 모듈 (새로 만들지 않음)

| 모듈 | 파일 | 이 화면에서 쓴 곳 | 쓰는 법 한 줄 |
|---|---|---|---|
| 화면 틀 | `src/components/page/PageFrame.tsx` | 화면 | 제목·부제·새로고침·오른쪽 버튼 자리 |
| 버튼 줄 | `src/components/auth/ActionBar.tsx` | 화면 | `actions` 에 표준 키(search·excel·create·update·delete) — 권한 없으면 알아서 흐림, delete 는 `confirm` |
| 저장 버튼 | `src/components/auth/AuthButton.tsx` | 팝업 | `need="reg"`(등록)·`"upd"`(수정) |
| 조회조건 | `src/components/search/SearchForm.tsx` | 화면 | `fields` 배열(select·input·custom) |
| 조건값 정리 | `src/components/search/params.ts` `allToUndef` | Parts | '전체'·빈 값 → undefined |
| 결과 머리 | `src/components/page/ResultHeader.tsx`, `ExcelButton.tsx` | 화면 | 건수·선택 수·안내 글, 엑셀 버튼 |
| 표 | `src/components/grid/DataGrid.tsx` | 화면 | `clientPageSize={0}`(페이징 없음), `sortable={false}`(트리 순서 유지), 체크 선택·더블클릭 |
| 컬럼 도우미 | `src/components/grid/columns.tsx` `col`·`centerCol`·`excelColumnsOf` | Columns·화면 | 화면 컬럼 = 엑셀 컬럼 |
| 엑셀 | `src/utils/excel.ts` `downloadExcel` | 화면 | `downloadExcel(excelColumnsOf(columns), rows, '메뉴관리')` |
| 상세 칸 배치 | `src/components/display/DetailGrid.tsx`, `ModalTitle.tsx` | 팝업 | 라벨 + 값 칸, 팝업 제목 |
| 폼 칸 | `src/components/forms/formItem.tsx` | 팝업 | `formItem(name, <컨트롤/>, RULES.x)` |
| 검사 규칙 | `src/utils/rules.ts` `req`·`reqSelect`·`maxBytes`·`byteCount`·`maxLen`·`numRange`·`check` | 팝업 | `RULES … satisfies Record<…>` |
| 문구 | `src/config/messages.ts` `VMSG`·`CMSG` | 팝업·화면 | 검사 문구(`VMSG.menuPath`·`upperLetters`), 삭제 확인(`CMSG.deleteSelected`) |
| 날짜 표시 | `src/utils/format.ts` `fmtDateTime14` | Columns | 최종변경일시 |
| 빈 값 빼기 | `src/utils/object.ts` `compactParams` | API·Parts | 보낼 파라미터에서 빈 값 제거 |
| 왼쪽 메뉴 다시 읽기 | `src/stores/useNavStore.ts` `fetchMenus(true)` | Parts `useAfterMenuChange` | 메뉴를 바꾼 뒤 사이드 패널이 바로 따라오게 |
| antd `Select` 검색·옵션 그리기 | antd 6 (`showSearch={{ filterOption }}`, `optionRender`, `popupRender`) | Parts·팝업 | 새 npm 없음 |

- 백엔드 공통: `ApiResponse`(성공 문구 코드 `S002`·`S003`·`S005`), `@RequireMenuAuth`·`Perm`(권한), `BusinessException`·`NotFoundException`(오류 코드), `DbBytes`(메뉴명 100바이트), `AuditFields`(감사 4컬럼 자동), `CurrentUser`(로그).

## 4. 화면별 설명

### 4-1. 홈 카드 / 메뉴 → 화면 열기

- 등록 3곳: `screens/screenIds.ts`(키) → `screens/registry.ts`(`def('sample.menuMgt', '메뉴관리', 'sample', '', () => import('./sample/MenuMgtPage'))`) → 메뉴 행(이관 SQL 6번 · 내장 메뉴 `menuSeed.ts`).
  - **제3부(메뉴 DB 전환)에서 바뀜**: `def` 는 `def('sample.menuMgt', '메뉴관리', () => import(…))` 로 줄고, 내장 메뉴 `menuSeed.ts`·대분류 표 `l1.ts` 는 삭제됩니다(메뉴 행은 DB 로만).
- 홈 카드: `HomePage.tsx` `CARDS` 에 한 줄(아이콘 `MenuOutlined`). 카드를 누르면 `openScreen('sample.menuMgt')`.
- 화면 경로 `/sample/menu-mgt` 는 화면키에서 자동으로 만들어집니다(점 뒤 camelCase → kebab). 메뉴 행의 `MENU_PATH` 와 한 글자도 달라선 안 됩니다.
- **왼쪽 메뉴가 3단계로 보이는 원리**: 사이드 패널(`components/layout/SidePanel.tsx`)은 대메뉴(상단 칸)의 자식 = 폴더(중메뉴), 폴더의 자식 = 화면(소메뉴)으로 그립니다. 그래서 이관 뒤 '견본' 아래에 '견본 화면' 폴더가 생기고 그 안에 화면들이 보입니다.
- **상단 대메뉴 칸**은 이 부 시점에는 `screens/l1.ts` `L1_DEFS` 의 `name` 이 대메뉴 `MENU_NM` 과 같아야 보였습니다. **제3부(메뉴 DB 전환)부터는 DB 대메뉴가 그대로 상단에 나타납니다**(코드 수정 없음).

### 4-2. 화면 틀 (`MenuMgtPage.tsx`)

- 구성(위 → 아래)
  1. 제목 줄: '메뉴관리' + 부제(3단계·메뉴ID 규칙 한 줄) + 오른쪽 `ActionBar`(조회·엑셀·등록·수정·삭제)
  2. 조회조건: 대메뉴(콤보) · 레벨(전체/대/중/소) · 사용(전체/사용/미사용) · 검색어(메뉴ID·메뉴명·경로)
  3. 결과 머리: '메뉴 목록 (n건)' + 안내 글 + 엑셀 버튼
  4. 표: 체크 · 메뉴ID(`SYS 03 12`, `00` 흐리게) · 메뉴명(레벨만큼 들여쓰기 `└`) · 레벨(대/중/소 + G/M) · 상위(경로 '시스템관리 › 공통코드') · 하위 수 · 메뉴경로 · 순서 · 사용 · 최종변경
- 핵심 코드

```tsx
const [params, setParams] = useState<MenuMgtSearch>(ALL);            // 처음엔 조건 없음 → 열자마자 전체 목록
const list = useQuery({ queryKey: menuMgtKeys.list(params), queryFn: () => menuMgtApi.list(params) });
const after = useAfterMenuChange();                                   // 목록 + 왼쪽 메뉴 다시 읽기
const remove = useMutation({ mutationFn: menuMgtApi.remove, onSuccess: after });

const onSearch = (v: Record<string, unknown>) => {
  setSelected([]);
  const next = toMenuParams(v);
  if (JSON.stringify(next) === JSON.stringify(params)) void list.refetch(); // 조건이 같으면 다시 읽기
  else setParams(next);                                                     // 다르면 새 키 → 저절로 읽음
};
```

- 뜻·왜 이렇게
  - **페이징이 없습니다**: 메뉴는 수백 건 이하이고 트리 순서(부모 다음 자식)로 봐야 해서 `usePagedSearch` 대신 `useQuery` 로 전체를 받습니다. 표도 `clientPageSize={0}`·`sortable={false}`(헤더 정렬이 트리 순서를 깨지 않게).
  - **열자마자 조회**: 공지사항은 [조회]를 눌러야 하지만, 메뉴는 전체가 한눈에 보여야 해서 처음부터 조건 없이 읽습니다. 이 첫 조회(`ALL`)는 등록 팝업 콤보와 **같은 캐시**를 씁니다.
  - **쓰기 뒤 왼쪽 메뉴도 다시 읽기**: 메뉴 테이블을 바꾸는 화면이라 `useNavStore.fetchMenus(true)` 로 사이드 패널을 바로 갱신합니다(다른 화면은 invalidate 만).
  - 성공·오류 문구는 서버가 보내고 자동으로 뜹니다(화면에 `message.success`·`try/catch` 없음).
- 쓰는 API: `GET /api/web/menu-mgt/menus`, `POST …/menus/delete`
- 삭제: 체크한 메뉴들 → 확인창(`CMSG.deleteSelected(n, '메뉴')`) → 서버가 하위 검사. 중메뉴와 그 소메뉴를 **같이 체크**하면 함께 지워집니다. 하나만 고르고 하위가 남으면 "하위 메뉴가 존재하여 삭제할 수 없습니다."(`MNU_001`).

### 4-3. 조회조건 · 콤보 부품 (`MenuMgtParts.tsx`)

- `MENU_FIELDS` — 조회조건 4칸. 대메뉴 칸은 `custom` 으로 `TopMenuSelect`(아래 콤보 옵션 재사용, 비우면 전체).
- `toMenuParams(v)` — 폼 값 → `MenuMgtSearch`. '전체'·빈 값은 뺍니다(`allToUndef`·`compactParams`), 레벨은 숫자로.
- `useAllMenus()` — 조건 없는 전체 목록(`menuMgtKeys.list(ALL)`) — 콤보 원천.
- `previewMenuId(rows, level, parentId, prefix)` — **서버 `MenuMgtRules` 와 같은 규칙**으로 새 메뉴ID 를 미리 계산합니다(보여 주기만, 저장 때 서버가 확정). 규칙을 바꾸면 **둘 다** 고칩니다.

```ts
const head = parentId.slice(0, level === 2 ? 3 : 5);       // 중메뉴: 'SYS', 소메뉴: 'SYS03'
const max = rows.filter((r) => r.parentMenuId === parentId && shape.test(r.menuId) && r.menuId.startsWith(head))
                .reduce((m, r) => Math.max(m, Number(r.menuId.slice(at, at + 2))), 0);
return { id: level === 2 ? `${head}${two(max + 1)}00` : `${head}${two(max + 1)}` };  // SYS0400 / SYS0313
```

- **콤보 가독성**(중메뉴가 많아질 때) — `topOptions`·`midOptions`·`MenuOptionLine`·`matchMenuOption`·`comboFoot`
  1. 대메뉴를 먼저 고르면 중메뉴 후보는 **그 대메뉴 것만**(최대 99).
  2. 한 줄 = **번호(고정폭) · 이름 · 하위 수**(`03 공통코드 · 소 12`) — 번호로도 찾고 ID 와 바로 연결.
  3. 콤보 안 **검색**: 숫자 1~2자리면 번호(`3` = `03`), 그 밖은 이름·접두어 부분일치.
  4. **미사용은 맨 아래·흐리게**, 번호를 다 쓴(99) 중메뉴는 고를 수 없음('번호 다 씀').
  5. 목록 높이는 antd 기본(약 8줄) + 스크롤, 아래 줄에 '전체 n개 · 검색 안내'(`popupRender`).
  6. 고른 뒤 칸에는 `[SYS] 시스템관리`·`03 공통코드` 로 보입니다.

### 4-4. 표 컬럼 (`MenuMgtColumns.tsx`)

- `MenuIdText` — `SYS0312` → **SYS** 03 12, `00` 은 흐리게 → 레벨이 한눈에. 7자가 아니면 그대로.
- `LevelTag` — 대(파랑)·중(하늘)·소(회색) + 타입 G/M. 규칙 밖 ID 는 '규칙 밖'.
- `menuColumns()` — 메뉴명은 레벨만큼 들여쓰고 대메뉴는 굵게. 엑셀은 같은 컬럼 정의(`excel:` 로 글자만).
- 길이 상한 `MENU_NM_MAX_BYTES = 100`·`MENU_PATH_MAX_LEN = 300`·`SORT_NO_MAX = 99999` — 서버 `MenuMgtRules` 와 같은 값.

### 4-5. 메뉴 등록·수정 팝업 (`popup/MenuEditModal.tsx`)

- 구조: `Modal`(`destroyOnHidden`) → 안쪽 `MenuForm`(열 때마다 새로 그림) → `DetailGrid` 한 줄씩 → [닫기]·[저장](`AuthButton`).
- 칸 표

| 칸 | 등록 | 수정 |
|---|---|---|
| 레벨 | 라디오 대메뉴(상단)·중메뉴(좌측)·소메뉴(화면), 기본 소메뉴 | 없음(위치 줄에 태그로) |
| 대메뉴 | 레벨이 중·소일 때 콤보(검색·번호·중 n). 조회조건에서 대메뉴를 골랐으면 그 값으로 시작 | 〃 |
| 중메뉴 | 레벨이 소일 때 콤보(대메뉴 고르기 전엔 막힘, 검색·번호·소 n) | 〃 |
| 접두어 | 레벨이 대일 때 — 영문만 남기고 대문자(`PrefixInput`), 3자 | 없음 |
| 위치 | — | '시스템관리 › 공통코드 › **은행코드**' + 레벨 태그(읽기만) |
| 메뉴ID | 미리보기(크게 `SYS 03 13`) 또는 못 만드는 까닭('중메뉴를 고르세요.' 등) | 그대로(읽기만) |
| 메뉴명 | 필수, 100바이트(글자 수 표시) | 〃 |
| 메뉴경로 | 소메뉴만 필수, `/{slug}/{kebab}`. 중메뉴를 고르면 같은 대메뉴 화면 경로의 앞부분(`/sys/`)을 채움 | 화면(M)만 |
| 순서 | 상위를 고르면 그 상위의 마지막 순서 + 1(대메뉴는 +100) | 그대로 |
| 사용 | 체크(기본 사용) — 끄면 메뉴에서 숨김 | 〃 |

- 핵심 코드

```tsx
const level = (Form.useWatch('menuLevel', form) ?? target?.menuLevel ?? 3) as MenuLevel;
const topMenuId = Form.useWatch('topMenuId', form);                  // 고른 대메뉴
const mids = midOptions(rows, topMenuId);                            // → 그 대메뉴의 중메뉴만
const preview = isNew ? previewMenuId(rows, level, parentOf({ menuLevel: level, topMenuId, midMenuId }), prefix) : { id: target.menuId };

/** 상위가 바뀌면 순서 = 그 상위의 마지막 + 1, 경로 앞부분 = 같은 대메뉴 화면 경로 */
const onParentChange = (next: Partial<MenuFormValues>) => { … form.setFieldsValue(patch); };
```

- 보내는 본문(`toBody`): 등록 = `{ menuLevel, parentMenuId(중: 대메뉴ID, 소: 중메뉴ID), prefix(대만), menuNm, menuPath(화면만), sortNo, useYn }`, 수정은 같은 모양이지만 서버가 이름·경로·순서·사용만 씁니다.
- 검사(`RULES`): 레벨·대메뉴·중메뉴 선택, 접두어 영문 대문자 3자(`VMSG.upperLetters`), 메뉴명 필수·100바이트, 메뉴경로 필수·300자·모양(`VMSG.menuPath`), 순서 0~99999. 레벨에 따라 그리지 않는 칸은 폼에 없어서 검사도 하지 않습니다.
- 저장 → 성공하면 목록·왼쪽 메뉴 다시 읽기 + 팝업 닫기. 메뉴ID 를 못 만드는 상태(상위 미선택·번호 다 씀)면 [저장]이 흐립니다.
- 서버 오류 코드: `MNU_005`(접두어 겹침), `MNU_006`(번호 다 씀), `MNU_007`(상위 모양), `MNU_008`(경로 겹침), `MNU_009`(동시 등록 — 다시 저장), `MNU_004`(없음), `VAL_001`(필드명).
- 왜 이렇게: 상위 고르기는 사용자 결정 ① 연쇄 콤보. 메뉴ID 는 서버가 정하지만 사용자가 저장 전에 결과를 보도록 미리보기를 둡니다. 경로·순서 자동 채움은 실수를 줄이기 위한 것이고 고쳐 쓸 수 있습니다.

## 5. 백엔드 요약

| API | 권한 | 하는 일 |
|---|---|---|
| `GET /api/web/menu-mgt/menus` | QRY | 메뉴 전체를 트리 순서로 펴고 레벨·하위 수·상위 경로를 붙여 조건(대메뉴·레벨·사용·검색어)으로 거름 |
| `POST /api/web/menu-mgt/menus` | REG | 레벨별 상위 확인 → `MenuMgtRules.nextId`(MAX+1) 또는 `topId`(접두어+0000) → 넣기, `S002` |
| `PUT /api/web/menu-mgt/menus/{menuId}` | UPD | 메뉴명·경로(화면만)·순서·사용, `S003` |
| `POST /api/web/menu-mgt/menus/delete` | DEL | 1~100건, 하위 검사(`MNU_001`) → 권한별 메뉴 행 → 메뉴 물리 삭제, `S005` |

- 규칙 한 곳: `MenuMgtRules`(메뉴ID 모양 `TOP_ID`·`MID_ID`·`LEAF_ID`, `levelOf`, `topId`, `nextId`, 경로 모양 `PATH`). 프론트 `previewMenuId` 와 같은 규칙.
- 동시 등록: 번호를 센 뒤 넣기 전에 같은 번호가 생기면 PK 중복 → `MNU_009`(500 대신). 다시 저장하면 다음 번호.
- 캐시 없음: 메뉴 권한 검사(`MenuAuthService`)와 내 메뉴(`MenuService`)는 매번 DB 를 읽어서 비울 캐시가 없습니다.
- 잠정: 최상위 상위값 `'0'`·메뉴경로 = 화면 경로(Q-05), 메뉴명 바이트 계산(Q-07).

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

화면에서 눌러 볼 순서(이관 SQL 을 돌린 뒤)

1. 홈 → '메뉴관리' 카드 → 목록이 바로 뜨고 '견본 › 견본 화면 › …' 이 들여쓰기로 보이는지, 왼쪽 메뉴에 '견본 화면' 폴더가 생겼는지.
2. [등록] → 소메뉴 → 대메뉴 '견본' → 중메뉴 '01 견본 화면' → 메뉴ID 미리보기 `SMP 01 0n`, 경로 앞부분 `/sample/` 이 채워지는지 → 이름·경로 입력 → [저장] → '등록되었습니다.' + 목록·왼쪽 메뉴에 나타나는지.
3. [등록] → 대메뉴 → 접두어 `smp` 입력(대문자로 바뀜) → 미리보기 '이미 쓰는 접두어입니다.' → 다른 접두어로 저장.
4. 행 더블클릭 → 수정 팝업에서 위치·메뉴ID 가 읽기 전용인지, 순서·사용을 바꿔 저장되는지.
5. 중메뉴 하나만 체크 → [삭제] → '하위 메뉴가 존재하여 삭제할 수 없습니다.' / 그 소메뉴까지 함께 체크하면 지워지는지.
6. 조회조건 대메뉴·레벨·사용·검색어로 걸러지는지, [엑셀] 이 들여쓰기 그대로 나오는지.

- SQL(이관·매퍼)은 **폐쇄망 개발 DB 에서 처음 확인**합니다. 이 저장소에서는 매퍼 문법만 `MapperXmlLoadTest` 로 검사했습니다.
