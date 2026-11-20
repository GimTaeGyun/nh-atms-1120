> 공통 모양 변경 2탄(2026-11-13): ① 콘텐츠 영역 흰 바탕 ② 콘텐츠 영역 스크롤 없음 — 표가 남은 높이를 채움(조회조건 줄 수에 따라 가변) ③ 권한관리 '권한' 칸을 DB 권한코드 콤보로.
> - **반입은 파일 단위**: 부록 A 의 12개 파일 = 새 파일 1개 + 전체 교체 11개. 통째로 덮어쓰기만 하면 됩니다. 부록 B = SHA-512.
> - 제4부(UI 가독성 개선)를 먼저 넣은 뒤 넣습니다(F1~F3 은 제4부 파일의 다음 판).
> - 경로 줄임: src/ = Frontend/nh-atms-web/src/

## 0. 한눈에 보기

- **비유**: 예전에는 표 높이를 화면마다 자(숫자 560·520·480)로 재서 고정했습니다. 이제는 표가 **물처럼 남은 칸을 채웁니다** — 조회조건이 2줄이면 표가 그만큼 낮아지고, 창을 키우면 표도 커집니다. 스크롤은 표 안에서만 생깁니다.
- 사용자 결정(2026-11-13)
  1. 흰 바탕 + 검색필터는 **그림자 없이 연한 테두리**(현업 화면처럼).
  2. **모든 표가 남은 높이를 채움**(높이를 숫자로 고정했던 메뉴관리·권한관리 메뉴 권한·기기모델코드도). 내용이 너무 길어 표가 최소(약 5행)보다 작아질 때만 화면 스크롤.
  3. 권한 칸 = **목록에 있는 권한코드만** 고르는 콤보(메뉴 권한 탭과 같은 목록). 새 권한코드는 메뉴 권한 탭·공통코드에서 먼저 만든다.

| 부분 | 전 | 후 |
|---|---|---|
| 콘텐츠 바탕 | 연하늘(`#f4f8fd`), 검색필터·카드는 그림자 있는 흰 카드 | **흰색**, 검색필터·카드는 연한 테두리만 |
| 표 높이 | 화면마다 숫자(560·520·480) 또는 행 수만큼 → 화면 전체가 스크롤 | **남은 높이를 채움**, 화면 스크롤 없음, 표 안에서만 스크롤 |
| 권한관리 등록·수정의 권한 칸 | 글자 입력(+ 자동완성) | **콤보**(DB 권한코드, 검색 가능) |

```text
.app-content(흰 바탕, 높이 고정)
 └ .page-frame (세로 flex, 높이 100%)
    ├ 제목 줄 · 버튼 줄 · 검색필터 · 결과 머리   ← 내용만큼(줄어들지 않음)
    └ 표 (.grid-fill = 남은 높이 전부)          ← useFillHeight 가 몸통 높이(scroll.y)를 잼
       탭 화면이면: .ant-tabs → 탭 내용 → 보이는 탭 칸 도 세로 flex 로 이어짐
       마스터·디테일이면: .master-detail → 두 카드 → 카드 몸통 도 세로 flex
```

## 1. 준비 — DB

- DB·백엔드는 바뀌지 않습니다. 권한 콤보는 기존 API `GET /api/web/auth-mgt/auth-codes`(메뉴 권한 탭과 같은 목록)를 씁니다.

## 2. 만들 파일 — 빨간줄 없이 가는 순서

> 새 파일 F4 를 F5·F6 보다 먼저 만듭니다(F5·F6 이 F4 를 부름). 나머지는 서로 부르지 않아 순서와 상관없습니다.
> 적용 전 지금 파일 11개를 따로 복사해 두면, 되돌리기는 그 파일을 다시 덮어쓰고 F4 를 지우면 끝입니다.

### 2-1. 프론트 (`Frontend/nh-atms-web/src`)

| # | 구분 | 파일 | 왜 이 순서 |
|---|---|---|---|
| F1 | 전체 교체 | `theme/global.css` | 흰 바탕용 테두리(그림자 없음) |
| F2 | 전체 교체 | `components/layout/layout.css` | 콘텐츠 흰 바탕·높이 꽉 채움 |
| F3 | 전체 교체 | `components/page/page.css` | 세로 flex 사슬 + `.grid-fill` |
| F4 | 새 | `components/grid/useFillHeight.ts` | F5·F6 이 부름 → 먼저 |
| F5 | 전체 교체 | `components/grid/DataGrid.tsx` | F4 를 씀 |
| F6 | 전체 교체 | `components/grid/EditableGrid.tsx` | F4 를 씀 |
| F7 | 전체 교체 | `components/grid/MasterDetailGrids.tsx` | `master-detail` 클래스(F3 CSS 가 씀) |
| F8 | 전체 교체 | `screens/popup/AuthUserEditModal.tsx` | 권한 콤보 |
| F9 | 전체 교체 | `screens/sample/AuthMgtParts.tsx` | 고정 높이 삭제 |
| F10 | 전체 교체 | `screens/sample/MenuMgtPage.tsx` | 〃 |
| F11 | 전체 교체 | `screens/sample/McnModelParts.tsx` | 〃 |
| F12 | 전체 교체 | `screens/sample/McnModelPage.tsx` | 〃 |
| ✅ | | `npm run check` | 타입검사·lint 오류 0 |

## 3. 가져다 쓴 공통 모듈 (새로 만들지 않음)

| 모듈 | 파일 | 이 변경에서 쓴 곳 | 쓰는 법 한 줄 |
|---|---|---|---|
| 권한코드 목록 | `src/screens/popup/AuthUserEditModal.tsx` `useAuthCodes`·`authOptions` | F8 | 메뉴 권한 탭 콤보와 같은 목록·같은 글('[A] 이름') |
| 브라우저 ResizeObserver | (표준, 새 npm 없음) | F4 | 표 칸 크기가 바뀌면 다시 잼 |

## 4. 화면별 설명

### 4-1. 표 높이 채우기 (`useFillHeight.ts`, `DataGrid.tsx`, `EditableGrid.tsx`)

- `DataGrid`·`EditableGrid` 에 **`height` 를 주지 않으면** 채우기를 시도합니다. 숫자를 주면 예전처럼 고정.
- 켜지는 조건: 표를 감싼 div 의 **부모가 세로 flex**(화면 틀 바로 아래, 탭 칸 바로 아래, 마스터·디테일 카드 몸통) + 팝업 안이 아님. 그 밖(팝업, 공통 모듈 시연 카드 안)은 예전처럼 내용 높이.
- 켜지면 div 에 `grid-fill`(남은 높이 차지, 최소 240px) → div 높이 − (머리글·페이지 줄·도구줄) = 표 몸통 높이(`scroll.y`). 창 크기·검색필터 줄 수·행 수가 바뀌면 다시 잽니다.

```tsx
const boxRef = useRef<HTMLDivElement>(null);
const fill = useFillHeight(boxRef, height === undefined);   // 높이를 안 줬을 때만
const bodyY = height ?? fill.y;                              // 표 몸통 높이
<div ref={boxRef} className={`data-grid${fill.fill ? ' grid-fill' : ''}`}> <Table scroll={{ x, y: bodyY }} … /> …
```

- **새 화면 규칙**: 목록 표에는 `height` 를 주지 않습니다. 표는 화면 틀(`PageFrame`) 바로 아래(또는 탭 칸 바로 아래)에 두고, 그 사이를 일반 `<div>` 로 감싸지 않습니다(감싸면 그 div 가 세로 flex 가 아니라 채우기가 꺼짐 — 꼭 감싸야 하면 그 div 에 `display:flex; flex-direction:column; flex:1; min-height:0`).

### 4-2. 화면 틀 · 흰 바탕 (`layout.css`, `page.css`, `global.css`)

- `.app-content` 흰 바탕, `.app-content-inner` 높이 100%, `.page-frame` 세로 flex(최소 높이 100%). 표가 아닌 칸(제목·검색필터·결과 머리)은 줄어들지 않음.
- 탭 화면: `.page-frame > .ant-tabs` → 탭 내용 → 보이는 탭 칸까지 세로 flex. 권한관리 세 탭 모두 표가 채움.
- 마스터·디테일: `.page-frame > .master-detail` 이 남은 높이, 두 카드·카드 몸통도 세로 flex(기기모델코드).
- 검색필터·카드(`.panel`)는 그림자 없이 `var(--border)` 테두리.

### 4-3. 권한 콤보 (`AuthUserEditModal.tsx` `AuthCodeSelect`)

- 부서권한·개인권한 등록/수정 팝업의 '권한' 칸 = antd `Select`(검색 가능, 폭 200px), 옵션 = `useAuthCodes()`(메뉴 권한 탭 콤보와 같은 캐시).
- 목록에 있는 것만 고릅니다. 수정 팝업에서 지금 값이 목록에 없으면(예전 데이터) 그 값도 한 줄로 보여 줘서 지워지지 않게 합니다.
- 검사: `reqSelect('권한')`('권한을 선택하세요.') + 2자 이내.

## 5. 백엔드 요약

| API | 권한 | 하는 일 |
|---|---|---|
| (변경 없음) `GET /api/web/auth-mgt/auth-codes` | QRY | 권한코드 목록 — 콤보 원천 |

## 6. 검사 · 확인

```bash
cd Frontend/nh-atms-web
```

```bash
npm run check
```

화면에서 볼 것(`npm run dev`)

1. 공지사항·메뉴관리·권한관리(세 탭)·기기모델코드에서 **화면 오른쪽 스크롤바가 없고** 표가 화면 아래 끝까지 차는지, 행이 많으면 표 안에서만 스크롤되는지.
2. 창 크기를 줄이거나(1366×768) 검색필터가 2줄인 화면(공지사항)에서 표가 그만큼 낮아지는지.
3. 콘텐츠 바탕이 흰색이고 검색필터가 연한 테두리 상자인지.
4. 권한관리 → 부서권한 [등록] → '권한' 칸이 콤보이고 메뉴 권한 탭의 권한 목록과 같은지, 검색으로 고를 수 있는지.
5. 팝업 안 표(사무소 검색 등)는 예전처럼 보이는지.
