> 권한관리 화면(`sample.authMgt`)을 **빈 상태에서 처음부터 만든다고 보고** 무엇을 어떤 순서로 만드는지 정리한 문서입니다(2026-11-12 다시 씀).
> - 화면 설명은 **탭·팝업 단위**(§4)이고 프론트를 자세히 적었습니다. 그대로 가져다 쓴 공통 모듈은 §3 에 있습니다.
> - 부록 A 는 **§2 순서대로** 새 파일은 전체 소스, 기존 파일은 diff(권한관리 관련 줄만)입니다. 폐쇄망에서는 이 문서만 보고 옮겨 적을 수 있습니다(파일별 SHA-512 는 부록 B).
> - 예전 1·2·3차 변경 기록(diff)은 뺐습니다. 필요하면 Git 커밋을 봅니다: `33aef2c`(1차), `dbed596`(2차 부서·개인권한), `79b9b36`(3차 메뉴 권한 탭).
> - 경로 줄임: `src/` = `Frontend/nh-atms-web/src/`, `web/` = `Backend/nh-atms-api/atms-web/src/main/java/kr/co/nh/atms/web/`, `mapper/` = `Backend/nh-atms-api/atms-web/src/main/resources/mapper/`.

## 0. 한눈에 보기

**무엇을 하는 화면인가**
- 비유: 건물 출입증 관리실.
  - "어느 부서 사람에게 어떤 등급(권한코드) 출입증을 줄지"(부서권한)
  - "특정 사람에게 따로 줄지"(개인권한)
  - "등급별로 어느 방(메뉴)에 들어가 무엇을 할 수 있는지"(메뉴 권한)
- AS-IS "시스템 사용권한부여" 화면 구성을 기준으로 했습니다(조회조건 개인사번, 권한목록 부서별·개인별, 등록·엑셀·조회).

**화면 구성** — 홈 카드 → 탭 3개

| 탭·팝업 | 테이블 | 모양(복사 견본) | 하는 일 |
|---|---|---|---|
| 부서권한 탭 | `TB_SCR_AUTH` (`PER_NO = '1'`) | 공지사항(목록 + 팝업) | 부서에 권한코드 부여·수정·삭제 |
| 개인권한 탭 | `TB_SCR_AUTH` (`PER_NO ≠ '1'`) | 공지사항 | 사람에게 권한코드 부여·수정·삭제 |
| 등록/수정 팝업 | `TB_SCR_AUTH` (+ `TB_OFC_RAW`, `TB_INSA`) | 공지사항 입력 팝업 | 두 탭이 같이 씀(`kind` 로 구분) |
| 메뉴 권한 탭 | `TB_SCR_AUTH_MENU` (+ `TB_SCR_MENU`) | 표 체크 편집 + 일괄 저장 | 권한코드별 메뉴 조회·등록·수정·삭제·실행·사용 체크 |

**권한 판단 규칙** — 로그인 사용자의 권한코드를 고르는 법(`web/common/auth/CurrentUserLoader.java`)
1. 개인권한 = `TB_SCR_AUTH.PER_NO = 내 개인번호`, 부서권한 = `PER_NO = '1'` 이고 내 부서·사무소와 맞는 행.
   - 내 부서·사무소: `TB_INSA.SAMUSO` = `TB_OFC_RAW.SAMUSO` → 그 행의 `SAMUSO` = `DEPT_CODE`, `ONLINE_CODE` = `OFC_CODE`
2. 유효(승인 `APV_STS='Y'`, 만료 전)한 **개인권한이 있으면 개인권한만**. 없으면 부서권한. 둘 다 없으면 권한 없음.
3. 권한코드 → 메뉴별 5개 플래그는 `TB_SCR_AUTH_MENU`(`USE_YN='Y'` 인 행만). 관리자(`AUTH='A'`)는 전부 허용(잠정 Q-09).

**요청 흐름** — 예: 부서권한 탭 [조회]

```
[화면] UserAuthPane(kind=DEPT) ─ usePagedSearch ─▶ authMgtApi.users({kind:'DEPT', …})
   ─ GET /api/web/auth-mgt/users?kind=DEPT ─▶ AuthMgtController.users  (@RequireMenuAuth QRY: 화면 권한 검사)
   ─▶ AuthMgtService.searchUsers (조건 정리) ─▶ AuthMgtMapper.countUserList / selectUserList
   ─▶ TB_SCR_AUTH ⟕ TB_INSA ⟕ TB_OFC_REG_MGT ⟕ TB_OFC_RAW   ◀── PageResult<AuthUser> ── DataGrid 에 그림
```

## 1. 준비 — DB

| 테이블 | 쓰는 컬럼 | 읽기/쓰기 | 비고 |
|---|---|---|---|
| `TB_SCR_AUTH` 사용자별업무권한 | `DEPT_CODE`·`OFC_CODE`·`PER_NO`(PK), `AUTH`, `INS_DATE`, `INS_ID`, `APV_STS`, `MEMO`, `DUE_DATE`, 감사 4컬럼 | 읽기·쓰기 | `PER_NO='1'` = 부서권한 |
| `TB_SCR_AUTH_MENU` 권한별메뉴관리 | `AUTH`·`MENU_ID`(PK), `QRY/REG/UPD/DEL/EXE_ROL_YN`, `USE_YN`, 감사 4컬럼 | 읽기·쓰기 | 행 삭제 없음(사용 해제 = `USE_YN='N'`) |
| `TB_SCR_MENU` 업무시스템메뉴 | `MENU_ID`, `PARENT_MENU_ID`, `MENU_NM`, `MENU_TYPE`, `MENU_PATH`, `SORT_NO`, `USE_YN` | 읽기 | 메뉴 전체 목록 |
| `TB_INSA` 인사정보 | `PER_NO`, `NAME`, `SAMUSO` | 읽기 | 성명, 소속 |
| `TB_OFC_RAW` 사무소정보(Raw) | `SAMUSO`(부서코드, PK), `SAMUSO_NAME`, `ONLINE_CODE`(사무소코드) | 읽기 | 부서 콤보, 키 자동 채움. `db/ddl` 없음(Q-23) |
| `TB_OFC_REG_MGT` 사무소등록관리 | `OFC_CODE`, `KANM` | 읽기 | 사무소명 |
| `TB_CO_CODE` 공통코드 | `CODE_TYPE`, `CODE`, `CODE_NM` | 읽기 | 권한명 — 설정 `atms.auth-mgt.auth-code-type` 이 있을 때만(Q-20) |

1. 테이블이 있는지 `db/check/01_exists.sql`, 컬럼이 맞는지 `02_columns.sql` 로 먼저 봅니다(새로 만들지 않음).
2. 메뉴 행 1개와 처음 메뉴 권한을 넣습니다. **처음 한 번은 SQL 로 넣어야 합니다**(권한이 없으면 권한관리 화면 자체를 못 엶). 그다음부터는 화면에서 넣습니다.

```sql
INSERT INTO TB_SCR_MENU (MENU_ID, PARENT_MENU_ID, MENU_NM, MENU_TYPE, MENU_PATH, SORT_NO, USE_YN, CRT_DTM, CRT_CMENO, LSCHG_DTM, LS_CMENO)
SELECT 'SMP005', 'SMP000', '권한관리', 'M', '/sample/auth-mgt', 5, 'Y',
       TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), '<내 개인번호>', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), '<내 개인번호>'
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM TB_SCR_MENU WHERE MENU_ID = 'SMP005');
```

```sql
INSERT INTO TB_SCR_AUTH_MENU (AUTH, MENU_ID, QRY_ROL_YN, REG_ROL_YN, UPD_ROL_YN, DEL_ROL_YN, EXE_ROL_YN, USE_YN, CRT_DTM, CRT_CMENO, LSCHG_DTM, LS_CMENO)
SELECT 'A', 'SMP005', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y',
       TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), '<내 개인번호>', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), '<내 개인번호>'
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM TB_SCR_AUTH_MENU WHERE AUTH = 'A' AND MENU_ID = 'SMP005');
```

```sql
COMMIT;
```

- 메뉴 경로 `/sample/auth-mgt` 는 화면키 `sample.authMgt` 에서 자동으로 만들어지는 경로와 **한 글자도 달라선 안 됩니다**(점 뒤 camelCase → kebab).
- 관리자(`AUTH='A'`)는 메뉴 권한 행이 없어도 전부 허용이라, 위 두 번째 SQL 은 관리자가 아닌 권한코드에 줄 때 그 코드로 바꿔 씁니다.

## 2. 만들 파일 — 빨간줄 없이 가는 순서

> **원칙: 불리는 파일을 먼저, 부르는 파일을 나중에.** 예를 들어 서비스가 DTO·매퍼를 부르면 DTO → 매퍼 → 서비스 순입니다.
> 순서대로 만들면 각 파일을 저장하는 순간 IDE 빨간줄(없는 클래스·타입)이 생기지 않습니다. 단계 끝의 ✅ 에서 한 번 확인합니다.
> - **새** = 부록 A 의 전체 소스를 그 경로에 새 파일로 만듭니다(UTF-8, 줄끝 LF).
> - **diff** = 기존 파일에 부록 A 의 diff 대로 `-` 줄을 지우고 `+` 줄을 넣습니다(앞뒤 공백 줄은 위치 찾기용). **권한관리 관련 줄만** 보여 줍니다 — 같은 파일의 다른 변경(테스트·목·시드 삭제 등)은 폐쇄망에 이미 반영됐다고 봅니다.
> - 번호는 부록 A 의 제목 번호와 같습니다.

### 2-1. 백엔드 (`Backend/nh-atms-api`)

| # | 구분 | 파일 | 왜 이 순서 |
|---|---|---|---|
| B1 | diff | `atms-common/.../api-message/api-messages.tsv` | 서비스가 쓸 오류 코드(`ATH_001`~`006`)를 먼저. 자바 컴파일과는 무관하지만 빠지면 `ApiMessageCodeGuardTest` 가 실패 |
| B2 | 새 | `web/authmgt/dto/AuthUserKey.java` | DTO 는 다른 권한관리 파일을 부르지 않음 → 맨 먼저 |
| B3 | 새 | `web/authmgt/dto/AuthUser.java` | (공통 `AuditFields` 만 씀) |
| B4 | 새 | `web/authmgt/dto/AuthUserSearchDto.java` | (공통 `PageRequest` 만 씀) |
| B5 | 새 | `web/authmgt/dto/AuthUserSave.java` | |
| B6 | 새 | `web/authmgt/dto/AuthUserDeleteRequest.java` | `AuthUserKey` 를 씀 → B2 다음 |
| B7 | 새 | `web/authmgt/dto/AuthDeleteResult.java` | |
| B8 | 새 | `web/authmgt/dto/AuthDept.java` | |
| B9 | 새 | `web/authmgt/dto/AuthCode.java` | |
| B10 | 새 | `web/authmgt/dto/AuthMenu.java` | |
| B11 | 새 | `web/authmgt/dto/AuthMenuSaveResult.java` | |
| B12 | diff | `web/common/auth/CurrentUserMapper.java` | 로더가 부를 `selectDeptAuths` 를 먼저 |
| B13 | diff | `resources/mapper/common/CurrentUserMapper.xml` | B12 의 SQL |
| B14 | diff | `web/common/auth/CurrentUserLoader.java` | `DEPT_PER_NO`·`pickEffective` — B12 를 부르고, B15 가 이 상수를 씀 |
| B15 | 새 | `web/authmgt/service/AuthMgtRules.java` | `CurrentUserLoader.DEPT_PER_NO` 를 씀 → B14 다음 |
| B16 | 새 | `web/authmgt/mapper/AuthMgtMapper.java` | DTO 를 씀 → B2~B11 다음 |
| B17 | 새 | `resources/mapper/authmgt/AuthMgtMapper.xml` | B16 의 SQL(XML 은 빨간줄 대상 아님, 실행 때 검사) |
| B18 | 새 | `web/authmgt/service/AuthMgtService.java` | DTO·매퍼·규칙·로더를 모두 씀 |
| B19 | 새 | `web/authmgt/controller/AuthMgtController.java` | 서비스를 씀 → 맨 끝 |
| ✅ | | `gradlew.bat compileJava --offline` | 여기까지 컴파일 오류 0 |
| B20 | diff | `src/test/.../common/auth/CurrentUserLoaderTest.java` | 테스트는 본 코드 다음 |
| B21 | diff | `src/test/.../common/auth/MenuAuthServiceTest.java` | 화면키→경로 예시 한 줄 |
| B22 | 새 | `src/test/.../authmgt/AuthMgtServiceTest.java` | |
| ✅ | | `gradlew.bat build --offline` | 테스트까지 통과 |

### 2-2. 프론트 (`Frontend/nh-atms-web/src`)

| # | 구분 | 파일 | 왜 이 순서 |
|---|---|---|---|
| F1 | diff | `components/grid/EditableGrid.tsx` | 메뉴 권한 표가 쓸 `rowButtons` 옵션을 먼저(없으면 F7 에 빨간줄) |
| F2 | diff | `screens/screenIds.ts` | 화면키 `sample.authMgt`·`popup.authUserEdit` — 팝업·탭이 이 키를 타입으로 씀 |
| F3 | 새 | `domain/authMgt.ts` | 타입. 다른 권한관리 파일을 부르지 않음 |
| F4 | 새 | `api/endpoints/authMgt.ts` | F3 타입을 씀 |
| F5 | 새 | `screens/sample/AuthMgtColumns.tsx` | F3 타입을 씀 |
| F6 | 새 | `screens/popup/AuthUserEditModal.tsx` | F2·F3·F4·F5 를 씀 |
| F7 | 새 | `screens/sample/AuthMgtParts.tsx` | F1~F6 을 씀 |
| F8 | 새 | `screens/sample/AuthMgtPage.tsx` | F7 을 씀 |
| F9 | diff | `screens/registry.ts` | F8 화면 파일을 등록 → F8 다음 |
| F10 | diff | `screens/menuSeed.ts` | 내장 메뉴(F2 키 사용) |
| F11 | diff | `screens/home/HomePage.tsx` | 홈 카드(F2 키 사용) |
| ✅ | | `npm run check` | 타입검사·lint 오류 0 |

### 2-3. DB 확인 SQL (선택)

| # | 구분 | 파일 | 하는 일 |
|---|---|---|---|
| D1 | diff | `db/check/01_exists.sql` | 확인 대상에 `TB_OFC_RAW` 추가 |
| D2 | diff | `db/check/02_columns.sql` | 〃 |

- 문서 `docs/07_API_계약.md` §8 은 계약(정본)이라 코드보다 먼저 읽어 둡니다(코드 빨간줄과는 무관).

## 3. 가져다 쓴 공통 모듈 (새로 만들지 않음)

> 비유: 레고 블록. 권한관리 화면은 아래 블록을 끼워 맞춘 것이고, 새로 깎은 블록은 거의 없습니다. 새 화면도 먼저 여기서 찾습니다(전체 목록 `docs/05_공통모듈_목록.md`).

| 모듈 | 파일 | 권한관리에서 쓴 곳 | 쓰는 법 한 줄 |
|---|---|---|---|
| `PageFrame` | `components/page/PageFrame.tsx` | 화면 틀(제목·브레드크럼·부제) | `<PageFrame screenKey={screenKey} subtitle="…">` |
| antd `Tabs` | antd | 탭 3개 | `items=[{key, label, children}]`, `activeKey` 로 지금 탭 |
| `ActionBar` | `components/auth/ActionBar.tsx` | 조회·엑셀·등록·수정·삭제 버튼 | `actions=[{key:'create', onClick}]` — 권한 없으면 알아서 흐림, `confirm` 주면 확인창 |
| `SearchForm` + 칸 팩토리 | `components/search/SearchForm.tsx`, `fields.tsx` `officeField` | 조회조건(사무소·검색어·권한·승인상태) | `fields=[officeField({required:false}), {name,label,type}]` |
| `allToUndef` | `components/search/params.ts` | '전체'·빈 값을 파라미터에서 빼기 | `auth: allToUndef(v.auth)` |
| `usePagedSearch` | `hooks/usePagedSearch.ts` | 부서·개인권한 목록 조회·페이지 | `search.search(조건)`, `search.rows`·`total`·`pagination`·`loading` |
| `ResultHeader` · `ExcelButton` | `components/page/` | 건수줄·엑셀 버튼 | `<ResultHeader title total selected note>` |
| `DataGrid` + 컬럼 함수 | `components/grid/DataGrid.tsx`, `columns.tsx` (`indexCol`·`stackCol`·`textCol`·`col`) | 부서·개인권한 목록 | `rowKey`·`rowSelection`·`onRowDoubleClick`·`pagination` |
| `EditableGrid` | `components/grid/EditableGrid.tsx` | 메뉴 권한 체크 표 | `editable:{type:'check', checkValues:['Y','N']}`, `canAdd`·`canUpdate`·`canDelete`, **`rowButtons={false}`**, `onSave(ch)` |
| `excelColumnsOf` · `downloadExcel` | `components/grid/columns.tsx`, `utils/excel.ts` | 엑셀(화면 컬럼 그대로) | `downloadExcel(excelColumnsOf(columns), rows, '파일명')` |
| antd `Modal` + `ModalTitle` | `components/display/ModalTitle.tsx` | 등록/수정 팝업 | `destroyOnHidden` 으로 열 때마다 새로 그림 |
| `DetailGrid` | `components/display/DetailGrid.tsx` | 팝업 안 "라벨 \| 값" 표 | `items=[{label, value, required, span}]` |
| `formItem` + 규칙 함수 | `components/forms/formItem.tsx`, `utils/rules.ts` (`req`·`reqSelect`·`maxLen`·`maxBytes`·`byteCount`) | 팝업 입력 검사 | `formItem('memo', <Input/>, [maxBytes(100)])` |
| `OfficeSelector` | `components/search/fields.tsx` | 개인권한 등록: 사무소 고르기 | 콤보 + 검색 팝업 |
| `PersonSelect` · `codeNameLabel` | `components/search/lookupFields.tsx` | 개인권한 등록: 사람 고르기 / '[코드] 이름' 글 | `<PersonSelect form={form} officeField="ofcCode" clearOnParentChange />` |
| `AuthButton` | `components/auth/AuthButton.tsx` | 팝업 [저장] | `need={row ? 'upd' : 'reg'}` — 권한 없으면 흐림 |
| `EmptyState` | `components/feedback` | 팝업에서 원본 못 읽었을 때 | `<EmptyState text action>` |
| `usePermission` | `hooks/usePermission.ts` | 메뉴 권한 저장 전 권한 확인 | `perm.canReg`·`canUpd` |
| `useAppMessage` · `DENIED_TEXT` | `hooks/useAppMessage.ts`, `components/auth/useAllowed.ts` | 권한 부족 경고 | `message.warning(DENIED_TEXT)` |
| `CMSG` | `config/messages.ts` | 삭제 확인창 문구 | `CMSG.deleteSelected(n, '부서권한')` |
| `fmtDateTime14` · `fmtOffice` | `utils/format.ts` | 일시·사무소 표시 | 포맷은 이 파일에서만(lint 가 막음) |
| `fromYmd` · `toYmd` | `utils/date.ts` | 만료일 ↔ 14자리 | 저장할 때 `YYYYMMDD` + `235959` |
| `compactParams` | `utils/object.ts` | 빈 조건 빼기 | `compactParams({…})` |
| `api` (http) | `api/http.ts` | 서버 호출 | 응답 봉투 벗기기·성공/오류 문구 자동 표시 |
| React Query | `@tanstack/react-query` | `useQuery`(읽기)·`useMutation`(쓰기)·`invalidateQueries` | 쓰기 성공하면 `authMgtKeys.all` 무효화 → 목록 자동 다시 읽음 |
| 백엔드 `@RequireMenuAuth`·`MenuAuthService` | `web/common/auth/` | API 마다 화면 권한 검사, 일괄 저장은 종류별 검사 | `@RequireMenuAuth(screen = SCREEN, need = Perm.QRY)` |
| 백엔드 `PageRequest`·`PageResult`·`AuditFields`·`OfficeCode`·`DbBytes`·`CurrentUser`·`BatchChanges` | `atms-common`, `web/common` | 페이징·감사 컬럼 자동·코드 6자리·바이트 길이·현재 사용자·일괄 저장 본문 | 견본과 같은 쓰임 |

## 4. 화면별 설명 (탭·팝업 단위)

### 4-1. 홈 카드 → 화면 열기
- **파일**: `src/screens/home/HomePage.tsx` 의 `CARDS` 배열에 한 줄을 넣습니다.
  - 내용: `{ key: 'sample.authMgt', title, desc, icon }`
- **동작**: 카드를 누르면 `useOpenScreen()(key)` 가 탭으로 화면을 엽니다.
  - 화면키가 `screens/registry.ts` 에 등록돼 있어야 열립니다.
  - 왼쪽 메뉴에 보이려면 DB `TB_SCR_MENU` 행이 있어야 합니다(§1).
- **등록 3곳**
  - `screenIds.ts`: `'sample.authMgt': ''` 와 팝업 `'popup.authUserEdit': ''` (값은 비움 — 화면번호를 쓰지 않음)
  - `registry.ts`: `def('sample.authMgt', '권한관리', 'sample', '', () => import('./sample/AuthMgtPage'))` — 지연 로딩이라, 처음 열 때 파일을 받아옵니다
  - `menuSeed.ts`: 서버 메뉴가 비었을 때 대신 쓰는 내장 메뉴에 한 줄(개발 모드 전용)

### 4-2. 화면 틀 — `AuthMgtPage.tsx`
- **구성**: `PageFrame`(제목·부제) 안에 antd `Tabs` 3개(부서권한 / 개인권한 / 메뉴 권한).
- **핵심 코드**

```tsx
const [tab, setTab] = useState<TabKey>('dept');
<Tabs activeKey={tab} onChange={(k) => setTab(k as TabKey)} items={[
  { key: 'dept',   label: '부서권한', children: <UserAuthPane kind="DEPT"   screenKey={screenKey} active={active && tab === 'dept'} /> },
  { key: 'person', label: '개인권한', children: <UserAuthPane kind="PERSON" screenKey={screenKey} active={active && tab === 'person'} /> },
  { key: 'menu',   label: '메뉴 권한', children: <MenuAuthPane screenKey={screenKey} active={active && tab === 'menu'} /> },
]} />
```

- **왜 이렇게**
  - 부서권한·개인권한은 **같은 부품(`UserAuthPane`)에 `kind` 만 다르게** 넘깁니다. 코드를 두 번 쓰지 않으려는 것입니다.
  - `active && tab === …` 는 단축키를 **지금 보이는 탭에만** 걸려는 것입니다. 안 그러면 숨은 탭의 버튼도 같이 눌립니다.
  - antd Tabs 는 처음 누른 탭만 그리고, 한 번 그린 탭은 상태(조회조건·목록)를 기억합니다.

### 4-3. 부서권한 탭 / 개인권한 탭 — `AuthMgtParts.tsx` `UserAuthPane`
- **화면 구성** (위에서 아래로)
  1. `ActionBar` 버튼: 조회·엑셀·등록·수정(1건 체크 때)·삭제(여러 건 체크 때, 확인창)
  2. `SearchForm` 조회조건(`userFields(kind)`)
     - 사무소: 비우면 전체
     - 검색어: 부서권한 탭은 "부서"(부서코드·부서명), 개인권한 탭은 "개인번호/성명"(AS-IS 개인사번)
     - 권한: 있는 권한코드 콤보
     - 승인상태: 전체/승인/미승인
  3. `ResultHeader`: "권한목록(부서별)" 또는 "(개인별)", 건수, 선택 수, 안내문, 엑셀 버튼
  4. `DataGrid` 목록(`userColumns(page, size, kind)`)
     - 칸: 순번 · 부서정보(코드/부서명) · 사무소정보(코드/사무소명) · **사용자(개인권한 탭만)** · 권한 · 승인상태(태그) · 만료일시(비면 '무기한') · 메모 · 등록정보(일시/등록자)
     - AS-IS 칸(순번·부서정보·사무소정보·권한·등록정보)에 승인상태·만료일시·메모를 더했습니다.
  5. `AuthUserEditModal` 팝업(§4-4)
- **핵심 코드와 뜻**

```tsx
const search = usePagedSearch<AuthUserSearch, AuthUser>({ key: authMgtKeys.users, fetch: authMgtApi.users }); // 조회·페이지·로딩 한 번에
const onSearch = (v) => { setSelected([]); search.search(toUserParams(v, kind)); };   // 폼 값 → 파라미터(kind 포함)
const remove = useMutation({ mutationFn: authMgtApi.removeUsers,
                             onSuccess: () => qc.invalidateQueries({ queryKey: authMgtKeys.all }) }); // 지우면 목록 다시 읽기
<DataGrid rowKey={userKeyText} rowSelection={{ selectedRowKeys: selected, onChange: (k) => setSelected(k.map(String)) }}
          onRowDoubleClick={(r) => setEdit({ open: true, target: keyOf(r) })} pagination={search.pagination} … />
```

- **키가 3개**라 행 구분값은 문자열 `'부서-사무소-개인번호'`(`userKeyText`)로 만듭니다. 체크 선택값도 이 글자입니다.
- 화면에서 **문구를 직접 띄우지 않습니다**. "등록되었습니다", "2건 삭제되었습니다" 같은 글은 서버가 보내고 `api/http.ts` 가 자동으로 띄웁니다.
- **쓰는 API**
  - `GET /auth-mgt/users?kind=DEPT|PERSON&…` (목록)
  - `POST /auth-mgt/users/delete` (`{keys:[…]}`, 1~100건)

### 4-4. 등록/수정 팝업 — `AuthUserEditModal.tsx`
- **3단 구조** (공지사항 입력 팝업과 같음)
  1. **겉 `Modal`**: `destroyOnHidden` 이라 닫으면 안을 버리고, 열 때마다 새 폼으로 시작합니다.
  2. **읽기 `AuthUserEditBody`**: 수정이면 원본을 `GET /users/{dept}/{ofc}/{perNo}` 로 읽은 뒤 폼을 그립니다(그 전에는 스켈레톤).
     - `gcTime: 0` 이라 닫으면 캐시도 버려서, 다음에 열 때 새로 읽습니다.
  3. **폼 `AuthUserForm`**: 검사 규칙 `RULES` 와 저장 `useMutation`.
- **칸 구성**

| 칸 | 부서권한 등록 | 개인권한 등록 | 수정(두 탭 공통) |
|---|---|---|---|
| 부서 | **부서 콤보**(`DeptSelect`: `GET /depts?q=` — 부서코드·부서명 검색, 최대 200) | 소속 부서 자동 표시(`PersonDept`: `GET /person-dept/{perNo}`) | 보여 주기만 |
| 사무소 | 고른 부서의 `ONLINE_CODE` 자동 표시 | `OfficeSelector`(사람 찾기용) | 보여 주기만 |
| 사용자 | "부서권한 (개인번호 1 고정)" | `PersonSelect`(그 사무소 인원) | 보여 주기만 |
| 권한 | `AuthCodeInput`: 있는 코드 자동완성 + 새 코드(2자) 입력 | 같음 | 수정 가능 |
| 승인상태 | 승인(Y)/미승인(N), 기본 Y | 같음 | 수정 가능 |
| 만료일 | 날짜(비우면 무기한) → 저장할 때 `YYYYMMDD235959` | 같음 | 수정 가능 |
| 메모 | 100바이트(한글 2바이트) 카운터 | 같음 | 수정 가능 |

- **보내는 본문**(`toBody`)
  - 부서권한: `{perNo:'1', deptCode, auth, apvSts, dueDate, memo}`
  - 개인권한: `{perNo, auth, …}`
  - 부서·사무소 키는 **서버가 `TB_OFC_RAW` 로 채웁니다**. 화면이 보낸 값은 쓰지 않습니다.
- **검사**: `RULES` 한 곳(`satisfies Record<…>` 로 칸마다 규칙이 빠지면 타입 오류). 서버도 같은 검사를 합니다(오류 문구 `VAL_001`).
- **저장**: `onFinish` → `save.mutate(body, { onSuccess: onClose })`. 성공했을 때만 닫고, 실패하면 팝업이 열린 채 서버 문구만 뜹니다.
  - [저장] 버튼은 `AuthButton need={row ? 'upd' : 'reg'}` 라 권한이 없으면 흐립니다.
- **같이 들어 있는 도우미**: 다른 탭도 이 파일에서 가져다 씁니다.
  - `useAuthCodes()`: 권한코드 목록 `GET /auth-codes`
  - `authOptions()`: 콤보 옵션
  - `AuthCodeInput`
- **서버 오류**

| 코드 | 언제 |
|---|---|
| `ATH_002` | 같은 키가 이미 있음 |
| `ATH_004` | 부서 없음 |
| `ATH_005` | 부서의 사무소코드 없음 |
| `ATH_006` | 사람의 소속 부서 없음 |
| `E404` | 인사정보에 없는 사람 |

### 4-5. 메뉴 권한 탭 — `AuthMgtParts.tsx` `MenuAuthPane` · `AuthMenuEditor`
- **화면 구성**
  1. `ActionBar` [조회]
  2. `SearchForm` 조회조건: **권한 콤보 1칸(필수)**. 있는 권한코드만 나오고, 새 코드는 부서·개인권한 탭에서 먼저 부여합니다.
  3. [조회] 전에는 "권한을 고르고 [조회]를 누르세요." 안내만 보입니다.
  4. [조회] 후 `ResultHeader`("메뉴 권한 — [T]") + `EditableGrid`(메뉴 전체, 화면 전체 폭)
     - 칸: 순번 · 메뉴(깊이만큼 들여쓰기 + 그룹/화면 태그, 메뉴 미사용 표시) · 메뉴ID · **조회·등록·수정·삭제·실행·사용 체크** · 최종변경
     - 툴바: 변경 건수 · 엑셀 · [저장] · [취소]. **행추가·행삭제 없음**(`rowButtons={false}`, `canAdd`·`canDelete` false)
- **규칙**
  - '사용' 체크 = 그 권한이 그 메뉴를 가짐.
  - DB 에 (권한, 메뉴ID) 행이 없는 메뉴는 **6칸 모두 미체크**(서버가 `NVL(…,'N')` 으로 내려 줌).
  - 체크를 바꾸면 그 칸에 "변경" 표시가 붙고 툴바에 변경 건수가 나옵니다. [저장]하면 **바꾼 행만** `POST /menus/batch` `{added:[], updated:[바꾼 행], deleted:[]}` 로 보냅니다.
    - 서버는 PK(권한+메뉴ID)로 행이 있으면 UPDATE, 없으면 INSERT 합니다.
  - 사용을 끄고 저장하면 행은 남고 `USE_YN='N'`. 사용을 안 켜고 조회만 켜도 그대로 `USE_YN='N'` 으로 저장됩니다(사용자 결정).
- **핵심 코드와 뜻**

```tsx
const onSearch = (v) => { const a = allToUndef(v.auth); if (!a) return;
  if (a === auth) void qc.invalidateQueries({ queryKey: authMgtKeys.authMenus(a) }); // 같은 권한 다시 조회 = 새로 읽기
  setAuth(a); };
<AuthMenuEditor key={auth} auth={auth} … />            // key 가 바뀌면 표를 새로 그림(이전 권한의 편집 내용 버림)
<EditableGrid rowKey="menuId" canAdd={false} canDelete={false} rowButtons={false}
              canUpdate={perm.canReg || perm.canUpd} onSave={onSave} … />
const onSave = async (ch) => { if (lacksMenuPermission(ch, perm)) { message.warning(DENIED_TEXT); return; } // 미리 권한 확인
  await save.mutateAsync({ added: [], updated: ch.updated, deleted: [] }); };
```

- **권한 확인**(`lacksMenuPermission`)
  - 원래 행이 없던 메뉴(`mappedYn='N'`)를 저장 = 새로 넣음 → **등록 권한**
  - 있던 메뉴 = 고침 → **수정 권한**
  - 서버도 같은 규칙으로 다시 검사합니다(403 `SYS_403`).
- **트리 순서**: 메뉴는 서버가 부모 → 자식 순으로 펴서 깊이(`lvl`)를 붙여 줍니다(`AuthMgtService.flattenTree`). 화면은 그 순서대로 그리고 `lvl` 만큼 들여씁니다.

## 5. 백엔드 요약 (자세한 것은 계약 §8, 부록 A)

| API | 권한 | 하는 일 |
|---|---|---|
| `GET /api/web/auth-mgt/users` | QRY | 부서·개인권한 목록(`kind`, 페이징) |
| `GET /api/web/auth-mgt/users/{dept}/{ofc}/{perNo}` | QRY | 1건 |
| `POST /api/web/auth-mgt/users` | REG | 등록 — 키는 서버가 `TB_OFC_RAW` 로 채움 (S002) |
| `PUT /api/web/auth-mgt/users/{dept}/{ofc}/{perNo}` | UPD | 권한·승인상태·만료일·메모 수정 (S003) |
| `POST /api/web/auth-mgt/users/delete` | DEL | 다건 삭제 (S005) |
| `GET /api/web/auth-mgt/depts?q=` | QRY | 부서 콤보 |
| `GET /api/web/auth-mgt/person-dept/{perNo}` | QRY | 사람 소속 부서·사무소 |
| `GET /api/web/auth-mgt/auth-codes` | QRY | 권한코드 목록 |
| `GET /api/web/auth-mgt/menus?auth=` | QRY | 메뉴 전체 × 이 권한(트리 순서) |
| `POST /api/web/auth-mgt/menus/batch` | 새로 넣음 REG · 고침 UPD | 메뉴 권한 일괄 저장 (S009) — 삭제 없음 |

- **쓰기가 끝나면** 서버가 로그인 사용자 캐시(`CurrentUserLoader.evictAll`)를 비웁니다. 내 권한을 바꾸면 바로 반영되고, 메뉴는 새로고침해야 반영됩니다.
- **잠정 규칙**은 `AuthMgtRules`·`CurrentUserLoader` 한 곳에 모았습니다.
  - Q-11: 같은 종류가 여러 행일 때 어느 행을 쓰나
  - Q-18: 승인상태 값
  - Q-20: 권한명 원천
  - Q-23: `TB_OFC_RAW` 코드 자리수·폐쇄 부서

## 6. 검사 · 확인

```bash
cd Frontend/nh-atms-web
```

```bash
npm run check
```

```bash
cd Backend/nh-atms-api
```

```bash
gradlew.bat build --offline
```

1. `npm run dev` + 백엔드 `bootRun` → 홈 **권한관리** 카드.
2. 부서권한 탭
   - [조회]
   - [등록]: 부서 콤보 → 권한 → 저장 → "등록되었습니다."
   - 1건 체크 → [수정]
   - 여러 건 체크 → [삭제]
3. 개인권한 탭: [등록] 사무소 → 사용자 → 소속 부서가 자동으로 나오는지 → 저장.
4. 메뉴 권한 탭: 권한 콤보 → [조회] → 미등록 메뉴가 미체크인지 → 체크 → [저장] → "권한이 저장되었습니다."
5. 권한 판단: 개인권한이 없는 사람이 그 부서의 부서권한 버튼 상태로 보이는지, 개인권한을 주면 그쪽으로 바뀌는지.
- 이 저장소(클라우드)는 Oracle 에 붙지 못합니다. **SQL 은 폐쇄망 개발 DB 에서 처음 확인**합니다.
