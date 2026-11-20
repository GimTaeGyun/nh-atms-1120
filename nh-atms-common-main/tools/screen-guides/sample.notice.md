> 공지사항을 **견본에서 실제 화면으로**(2026-11-13) + **검색필터 공통 필드 규칙**(앞으로 만드는 화면도 같음).
> - **반입은 파일 단위**: 부록 A 의 23개 파일 = 새 파일 1개(공통 `optionFields.tsx`) + 전체 교체 21개 + diff 1개(응답코드 TSV 두 줄). 교체 파일은 통째로 덮어쓰기만 하면 됩니다. 부록 B = SHA-512.
> - 제1~5부를 먼저 넣은 뒤 넣습니다(F6·F7·F9 는 제5부 파일의 다음 판, F8 은 제2부 파일의 다음 판).
> - DB 변경 없음(`TB_NOTICE` 그대로, 새 컬럼 없음).
> - 경로 줄임: src/ = Frontend/nh-atms-web/src/, web/ = atms-web …/kr/co/nh/atms/web/, mapper/ = atms-web …/resources/mapper/

## 0. 한눈에 보기

- **비유**: 공지 게시판에 "**누구에게 보이는 글인지**" 꼬리표가 붙습니다. 꼬리표가 비어 있으면 **전 직원 게시판**(전체 공지), 사무소 이름이 붙어 있으면 **그 사무소 게시판**(그 사무소 사람 + 관리자만 봄).
  그리고 검색필터 칸은 이제 **공통 부품 상자에서만** 꺼내 씁니다 — 화면마다 콤보·입력칸을 따로 깎지 않습니다.
- 사용자 결정(2026-11-13)
  1. **남의 글 수정·삭제 = 관리자(권한 A)만.** 그 밖은 본인 글만, 그것도 그 화면의 수정·삭제 권한이 있을 때만(권한이 없으면 본인 글도 못 고침).
  2. **전체 공지(사무소 비움) 등록 = 관리자만.** 그 밖은 '사무소 지정' 공지만, 사무소는 내 사무소로 고정.
  3. **사무소 공지 읽기 = 그 사무소 + 관리자.** 목록 기본 = "전체 공지 + 내 사무소"(관리자는 모든 공지). 조회조건 '사무소 지정'은 관리자만.
  4. **검색필터는 공통 필드만** — 없는 것은 공통에 추가 후 사용(검색구분+검색어·권한코드 콤보·검색어 칸·선택지 콤보), 공지사항·권한관리·메뉴관리·기기모델코드·기기정보 모두 바꿈.

| 부분 | 전(견본) | 후(실제 화면) |
|---|---|---|
| 목록 범위 | 모든 공지 | **전체 공지 + 내 사무소**(관리자 = 모두) — 서버가 거름 |
| 조회조건 | 작성일 · 사무소 · 작성자 · 검색구분 · 검색어(로컬 부품) | 작성일 · **공지 범위** · [관리자] 사무소 · 검색구분+검색어 — **모두 공통 필드** |
| 목록 칸 | 사무소 | **범위**(전체 = 파란 꼬리표 / 사무소명) + 제목 옆 **N**(3일 안) |
| 수정·삭제 버튼 | 화면 권한만 보고 켬 | 고른 글이 **내 글**일 때만(관리자는 모두), 서버도 403 `NTC_002` |
| 상세 팝업 | 표 + [수정] | 범위·작성자·작성일·조회수 표 + 본문 + **이전 글/다음 글** + [수정]·[삭제](본인 글만) |
| 등록·수정 팝업 | 사무소 콤보(비우기 가능) | **공지 범위 라디오**(전체 공지 = 관리자만) + 사무소, 제목 200·내용 4000바이트 글자 수 |
| 권한이 조회뿐 | 버튼만 흐림 | 버튼 흐림 + 제목 옆 '**읽기 전용** — 등록·수정·삭제 권한이 없습니다.' |

```text
누가 무엇을 보나 (TB_NOTICE.OFC_CODE)
  OFC_CODE 비움      → 전체 공지   : 모두 봄                       · 등록 = 관리자만
  OFC_CODE = 000080  → 사무소 공지 : 000080 사람 + 관리자만 봄      · 등록 = 관리자 또는 000080 사람(내 사무소)
  고치기·지우기       : 관리자 = 모든 글 / 그 밖 = 본인 글(PER_NO = 나) + 화면 권한 UPD·DEL
  다른 사무소 글      : 관리자가 아니면 목록에 안 나오고, 번호로 열어도 404 NTC_001(없는 것처럼)
```

## 1. 준비 — DB

- 테이블 변경 없음. `TB_NOTICE`(공지)·`TB_OFC_REG_MGT`(사무소명)를 읽고 씁니다.
- "사무소 비움 = 전체 공지" 는 정본에 없는 해석이라 **잠정 Q-24**(`docs/90_현업질문_후보.md`). 답이 다르면 `NoticeRules.canSee` 와 매퍼 `visible` 두 곳만 고칩니다.
- 관리자 = 권한코드 'A'(잠정 Q-09), 내 사무소 = 로그인 권한 사무소(`TB_SCR_AUTH.OFC_CODE`).

## 2. 만들 파일 — 빨간줄 없이 가는 순서

> **원칙: 불리는 파일을 먼저, 부르는 파일을 나중에.** 공통 부품(F1~F5) → 그 부품을 쓰는 화면(F6~F15) 순입니다.
> - **새** = 부록 A 의 전체 소스를 새 파일로(UTF-8, 줄끝 LF). **교체** = 기존 파일을 부록 A 전체본으로 통째로 덮어쓰기. **diff** = `-` 줄 빼고 `+` 줄 넣기.

### 2-1. 백엔드 (`Backend/nh-atms-api`)

| # | 구분 | 파일 | 왜 이 순서 |
|---|---|---|---|
| B1 | diff | `atms-common/.../api-message/api-messages.tsv` | 서비스가 던질 `NTC_002`·`NTC_003` 을 먼저. 빠지면 `ApiMessageCodeGuardTest` 실패 |
| B2 | 교체 | `web/notice/dto/NoticeSearchDto.java` | DTO 는 다른 공지 파일을 부르지 않음 → 먼저 |
| B3 | 교체 | `web/notice/dto/Notice.java` | 〃 |
| B4 | 교체 | `web/notice/service/NoticeRules.java` | `Notice`(B3)를 씀 |
| B5 | 교체 | `web/notice/mapper/NoticeMapper.java` | `Notice`·`NoticeSearchDto` 를 씀 |
| B6 | 교체 | `mapper/notice/NoticeMapper.xml` | B5 의 SQL(`MapperXmlLoadTest` 가 검사) |
| B7 | 교체 | `web/notice/service/NoticeService.java` | B2~B5 를 모두 씀. 컨트롤러는 그대로(바뀐 것 없음) |
| ✅ | | `gradlew.bat compileJava --offline` | 컴파일 오류 0 |
| B8 | 교체 | `src/test/.../notice/NoticeServiceTest.java` | 테스트는 본 코드 다음 |
| ✅ | | `gradlew.bat build --offline` | 테스트까지 통과 |

### 2-2. 프론트 (`Frontend/nh-atms-web/src`)

| # | 구분 | 파일 | 왜 이 순서 |
|---|---|---|---|
| F1 | 교체 | `hooks/useLookup.ts` | 공통 권한 콤보(F4)가 부를 `useAuthCodes` 를 먼저 |
| F2 | 교체 | `components/search/SearchForm.tsx` | 필수 문구 한 줄로(F4 `authField({ required: true })` 가 기대함) |
| F3 | 새 | `components/search/optionFields.tsx` | 공통 새 부품. `SearchForm` 타입만 씀 |
| F4 | 교체 | `components/search/lookupFields.tsx` | F1 훅을 씀 |
| F5 | 교체 | `components/search/McnSearchModal.tsx` | 기번 입력 공통 부품 |
| ✅ | | `npm run check` | 여기까지 넣어도 오류 0(옛 화면 파일은 아직 로컬 부품을 쓰지만 그대로 돎) |
| F6 | 교체 | `screens/popup/AuthUserEditModal.tsx` | F4 `AuthSelect` 를 씀. **로컬 `useAuthCodes`·`authOptions` 가 빠지므로 F7 과 함께** |
| F7 | 교체 | `screens/sample/AuthMgtParts.tsx` | F1·F3·F4 를 씀(F6 의 옛 export 를 더는 부르지 않음) |
| F8 | 교체 | `screens/sample/MenuMgtParts.tsx` | F3·F4 를 씀 |
| F9 | 교체 | `screens/sample/McnModelParts.tsx` | F3 을 씀 |
| F10 | 교체 | `screens/sample/McnInfoParts.tsx` | F5 를 씀 |
| F11 | 교체 | `domain/notice.ts` | 공지 타입(계약 §3) |
| F12 | 교체 | `screens/sample/NoticeParts.tsx` | F3·F11 을 씀 |
| F13 | 교체 | `screens/popup/NoticeViewModal.tsx` | F12 `ScopeTag` 를 씀 |
| F14 | 교체 | `screens/popup/NoticeEditModal.tsx` | F12 상한값을 씀 |
| F15 | 교체 | `screens/sample/NoticePage.tsx` | F12~F14 를 모두 씀 → 맨 끝 |
| ✅ | | `npm run check` | 타입검사·lint 오류 0 |

- 계약 `docs/07_API_계약.md` §3(공지 범위·권한·앞뒤 글), 응답코드 `docs/06_응답코드.md`, 카탈로그 `docs/05_공통모듈_목록.md` §3 을 같이 갱신했습니다(문서는 빨간줄과 무관).

## 3. 가져다 쓴 공통 모듈 (+ 이번에 공통에 넣은 것)

| 부품 | 파일 | 어디에 |
|---|---|---|
| `rangeField` 작성일(최근 1개월, 최대 366일) | `components/search/dateFields.tsx` | 공지 조회조건 |
| `officeField` 사무소 · `OfficeSelector` | `components/search/fields.tsx` | 공지 조회조건(관리자) · 등록 팝업 |
| **[새]** `optionField`·`OptionSelect` 고정 선택지(맨 위 '전체') | `components/search/optionFields.tsx` | 공지 범위 · 권한관리 승인상태 · 메뉴관리 레벨·대메뉴 |
| **[새]** `keywordTypeFields`·`keywordTypeOf`·`keywordTypeText` 검색구분+검색어 | `components/search/optionFields.tsx` | 공지 |
| **[새]** `keywordField`·`keywordOf` 검색어 | `components/search/optionFields.tsx` | 권한관리 · 메뉴관리 · 기기모델코드 |
| **[새]** `enabledBy` 다른 칸 값에 따라 켜기 | `components/search/optionFields.tsx` | 공지(범위 '사무소 공지' 일 때만 사무소) |
| **[새]** `AuthSelect`·`authField` 권한코드 | `components/search/lookupFields.tsx` | 권한관리 부서·개인 조회조건, 메뉴 권한(필수), 입력 팝업(`keepValue`) |
| **[새]** `McnNoInput`·`mcnNoField` 기번 입력 + 🔍 | `components/search/McnSearchModal.tsx` | 기기정보 |
| `ynField` 사용여부 | `components/search/lookupFields.tsx` | 메뉴관리(`ynField('useYn', '사용', { defaultValue: '' })`) |
| `usePermission` · `ActionBar` · `AuthButton` | `hooks/usePermission.ts` · `components/auth/*` | 버튼 흐림, 읽기 전용 |
| `DetailModal` · `DetailGrid` · `formItem` · `byteCount`·`maxBytes` | `components/display/*` · `components/forms/formItem.tsx` · `utils/rules.ts` | 상세·입력 팝업 |
| `DataGrid` · `excelColumnsOf` · `downloadExcel` · `PrintLayout` | `components/grid/*` · `utils/excel.ts` · `components/print/*` | 목록·엑셀·출력 |

- **검색필터 규칙(앞으로 모든 화면)**: `SearchForm` 의 `fields` 에는 위 같은 공통 팩토리만 넣습니다. `{ type: 'select', options }`·`{ type: 'input' }`·로컬 Select/Input 칸 금지. 맞는 부품이 없으면 공통(`components/search`)에 먼저 추가 → `docs/05_공통모듈_목록.md` §3 에 한 줄.

## 4. 화면별 설명

### 4-1. 조회조건 (`NoticeParts.tsx` `useNoticeFields`)

```tsx
rangeField({ name: 'period', label: '작성일', maxDays: 366, defaultValue: lastMonth() }),
optionField('scope', '공지 범위', scopeOptions(admin), { allLabel: scopeAllLabel(admin), widthClass: 'normal' }),
...(admin ? [enabledBy(officeField({ required: false }), 'scope', (v) => v === 'OFC')] : []),
...keywordTypeFields({ options: FIELD_OPTIONS }),   // 제목·내용·작성자 ('전체'는 공통이 붙임)
```

- **공지 범위** 선택지: 관리자 = 모든 공지 / 전체 공지만 / 사무소 공지, 그 밖 = 전체 공지 + 내 사무소 / 전체 공지만 / 내 사무소 공지만.
- **사무소** 칸은 관리자에게만 있고, 범위가 '사무소 공지'일 때만 켜집니다(`enabledBy` — 꺼지면 값도 비움). 비우면 모든 사무소의 사무소 공지.
- **검색구분 '전체'** 면 검색어 칸이 꺼지고 비워집니다(`keywordTypeFields`). 검색어가 있을 때만 `field`·`keyword` 를 보냅니다(`keywordTypeOf`).
- `toNoticeParams`: `scope` 가 'OFC' 일 때만 `ofcCode` 를 보냄. 서버도 관리자가 아니면 `ofcCode` 를 버립니다(두 겹).

### 4-2. 목록 · 버튼 (`NoticePage.tsx`, 컬럼 `noticeColumns`)

- 컬럼: 순번 · 번호 · **범위**(`ScopeTag`: 전체 = 파란 꼬리표, 사무소 = '[코드] 이름', 엑셀은 글자) · 제목(+ **N**: `isNewNotice` = 작성일이 오늘부터 `NEW_DAYS`(3)일 안) · 작성자 · 작성일 · 조회수.
- 버튼 켜짐 규칙(서버와 같음, `NoticeParts` 3절)

  | 버튼 | 켜지는 때 |
  |---|---|
  | [등록] | 화면 권한 REG (`ActionBar` 가 알아서) |
  | [수정] | 1건 선택 + `canEditNotice` = 관리자 또는 (UPD 권한 + 내 글) |
  | [삭제] | 1건 이상 선택 + 모두 `canDeleteNotice` = 관리자 또는 (DEL 권한 + 내 글) |

- 제목 옆 안내(`permNote`): 조회 권한뿐 → '읽기 전용 — 등록·수정·삭제 권한이 없습니다.', 관리자가 아님 → '본인이 쓴 글만 수정·삭제할 수 있습니다. 등록은 내 사무소 공지만.', 관리자 → 없음.

### 4-3. 상세 팝업 (`popup/NoticeViewModal.tsx`)

- 여는 쪽이 `GET /api/web/notice/{no}?hit=Y`(조회수 +1)로 읽어 넘깁니다. 응답에 이전·다음 글이 함께 옵니다(내가 볼 수 있는 공지 안에서, 조회조건과 무관).
- 표: 제목 · 공지 범위(+ 누구에게 보이는지 한 줄) · 번호 · 작성자 · 작성일 · 조회수 · 최종변경 · 내용(줄바꿈 그대로).
- 아래 두 줄: **다음 글**(번호가 바로 큰 글) / **이전 글**(바로 작은 글). 누르면 그 글을 다시 읽습니다(그 글 조회수 +1). 없으면 흐린 '…이 없습니다.'.
- [삭제](확인창)·[수정]은 `canDelete`·`canEdit` 일 때만 보입니다. 삭제하면 팝업이 닫히고 목록을 다시 읽습니다.

### 4-4. 등록·수정 팝업 (`popup/NoticeEditModal.tsx`)

- **공지 범위** 라디오: 전체 공지 / 사무소 지정. 관리자가 아니면 '전체 공지'가 흐리고 사무소가 **내 사무소로 고정**(콤보 흐림) — 서버도 403 `NTC_003`.
- 사무소 칸은 '사무소 지정'일 때만 필수(`dependencies: ['scope']` 로 범위를 바꾸면 다시 검사). '전체 공지'면 사무소를 보내지 않습니다(`toBody`).
- 제목 200·내용 4000 바이트 글자 수(`byteCount`)가 칸 오른쪽 아래에 보입니다(서버 `DbBytes` 와 같은 계산, 잠정 Q-07).
- 관리자가 아닌 사람이 예전 전체 공지(본인 글)를 고치면 내 사무소 공지로 바뀝니다.

### 4-5. 다른 화면 검색필터 (공통 필드로 바꿈)

| 화면 | 전 | 후 |
|---|---|---|
| 권한관리 부서·개인 | `{ type: 'input' }` 검색어, 로컬 `AuthSelect`, `{ type: 'select' }` 승인상태 | `keywordField` · `authField()` · `optionField('apvSts', …)` |
| 권한관리 메뉴 권한 | 로컬 `AuthSelect required` + 필수 문구 두 줄 | `authField({ required: true })` (문구 한 줄 — F2) |
| 권한관리 입력 팝업 | 로컬 `AuthCodeSelect` | 공통 `AuthSelect allowAll={false} keepValue` |
| 메뉴관리 | 로컬 Select(대메뉴) · `{ type: 'select' }` 레벨·사용 · `{ type: 'input' }` | 대메뉴 = 공통 `OptionSelect` 에 옵션·한 줄 모양·번호 검색만 얹음 · `optionField` · `ynField` · `keywordField` |
| 기기모델코드 | `{ type: 'input' }` 검색어 | `keywordField` |
| 기기정보 | 로컬 `McnNoSearchInput` | 공통 `mcnNoField()` |

## 5. 백엔드 요약

- `NoticeRules` — 규칙 한 곳: `canSee(n, admin, myOfc)` · `canModify(n, admin, myPerNo)` · `canWriteTo(ofcCode, admin, myOfc)`, 범위 값 `COMMON`·`OFC`.
- `NoticeService`
  - 조회: `scope` 검사(그 밖 값 400 `VAL_003`), 관리자가 아니면 `restrict=true`·`myOfc=내 사무소`(요청 값 무시), 사무소 지정은 관리자 + `OFC` 일 때만.
  - 상세: 볼 수 없으면 404 `NTC_001`(조회수 안 올림) → 조회수 +1 → 앞뒤 글(`selectNeighbors`).
  - 등록: `canWriteTo` 아니면 403 `NTC_003`. 수정: 볼 수 있나(404) → 본인 글(403 `NTC_002`) → 사무소(403 `NTC_003`).
  - 삭제: 관리자가 아니면 `selectOwners` 로 모두 본인 글인지 먼저(하나라도 아니면 아무것도 안 지움).
- 매퍼: `<sql id="visible">`(관리자가 아니면 `OFC_CODE IS NULL OR = 내 사무소`)를 목록·건수·앞뒤 글이 함께 씀. `${}` 없음.
- 컨트롤러는 그대로(경로·권한 플래그 같음).

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

1. 관리자로 공지사항 → [조회] → 범위 칸에 '전체' 꼬리표와 사무소명이 섞여 보이는지, 3일 안 글 제목 옆 N.
2. 공지 범위 '사무소 공지' → 사무소 칸이 켜짐 → 사무소 고르고 [조회] / 범위를 바꾸면 사무소 칸이 꺼지고 비는지.
3. 검색구분 '제목' → 검색어 칸 켜짐 → '점검' [조회] / 검색구분 '전체' → 검색어 칸이 비고 꺼지는지.
4. 제목 클릭 → 상세 → '이전 글' 을 눌러 넘어가는지, [수정]·[삭제]가 보이는지(관리자).
5. 관리자가 아닌 계정(권한관리에서 권한 A 가 아닌 사용자)으로: 목록에 다른 사무소 공지가 없는지, 남의 글을 고르면 [수정]·[삭제]가 흐린지, [등록] 팝업에서 '전체 공지'가 흐리고 사무소가 내 사무소로 고정인지.
6. 조회 권한만 있는 계정: 제목 옆 '읽기 전용', 상세에 [수정]·[삭제] 없음.
7. 권한관리 메뉴 권한 탭 → 권한 없이 [조회] → '권한을 선택하세요.' **한 줄만** / 메뉴관리 조회조건 대메뉴 콤보에서 'SYS' 입력으로 걸러지는지.

- 새 SQL(`visible`·앞뒤 글·작성자)은 **폐쇄망 개발 DB 에서 처음 확인**합니다. 이 저장소에서는 매퍼 문법만 `MapperXmlLoadTest` 로 검사했고, 화면은 가짜 API 로 관리자·일반·읽기 전용 세 경우를 눌러 확인했습니다.
