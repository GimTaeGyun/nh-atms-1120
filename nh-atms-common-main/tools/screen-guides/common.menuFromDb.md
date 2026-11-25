> 새 화면이 아니라 **공통 구조 변경**입니다(2026-11-12): 프론트에 하드코딩된 메뉴 구조를 걷어내고 메뉴를 DB(`TB_SCR_MENU`)·메뉴관리 화면으로만 관리합니다.
> - 형식은 화면 개발 가이드와 같습니다. 부록 A 는 §2 순서대로 새 파일 = 전체 소스, 기존 파일 = diff(이 변경 줄만), 삭제 = 파일 삭제. 부록 B = SHA-512.
> - 경로 줄임: src/ = Frontend/nh-atms-web/src/

## 0. 한눈에 보기

- **무엇이 바뀌나** — 비유: 예전에는 건물 안내판(상단 메뉴)을 **페인트로 벽에 그려** 놓아서(코드) 층이 늘면 벽을 다시 칠해야(소스 수정·배포) 했습니다. 이제는 **전광판**(DB)이라 메뉴관리 화면에서 바꾸면 바로 바뀝니다.
- 사용자 결정(2026-11-12)
  1. 메뉴는 **DB 또는 메뉴관리 화면으로만** 관리한다 — 프론트의 대분류 표(`screens/l1.ts`)·내장 메뉴(`screens/menuSeed.ts`)·메뉴 출처 설정(`VITE_MENU_SOURCE`) 삭제.
  2. 상단 대메뉴·아이콘 줄·사이드 패널·**탭 제목·브레드크럼**까지 DB 메뉴로 그린다.
  3. `TB_SCR_MENU` 에 아이콘 컬럼이 없어 **대메뉴 아이콘은 모두 같은 기본 아이콘**(아이콘 줄 글자 = 메뉴명 앞 2글자).
  4. 메뉴를 못 받았거나 보이는 메뉴가 없으면 **빈 메뉴 + 안내 + [다시 시도]**. 홈·URL 직접 입력으로는 화면이 열린다(권한은 서버가 검사).
  5. 홈 카드(견본 바로가기)는 손대지 않는다 — 곧 걷어내고 대시보드 화면을 넣을 예정.

| 부분 | 전 (코드) | 후 (DB) |
|---|---|---|
| 상단 메뉴·아이콘 줄 | `l1.ts` `L1_DEFS`(홈·견본) — `MENU_NM` 이 표의 `name` 과 같아야 보임 | '홈'(고정) + 내 메뉴의 대메뉴 전부(SORT_NO 순) |
| 사이드 패널 | 고른 대분류 slug → 이름으로 대메뉴 찾기 | 고른 대메뉴ID 의 중메뉴(폴더)·소메뉴(화면) |
| 메뉴에 있지만 화면 코드가 없음 | 안 보임 | '준비 중'(흐림, 안 열림, 경로 툴팁) |
| 메뉴를 못 받음 | 개발 모드면 내장 메뉴(`menuSeed.ts`) | 안내 + [다시 시도] |
| 탭 제목·브레드크럼 | `registry.ts` 의 제목·대분류·그룹 | DB 메뉴명 · 대메뉴 > 중메뉴 > 메뉴명 (메뉴에 없는 화면만 `registry` 이름) |
| `registry.ts` 한 줄 | `def(키, 제목, 'sample', '', 화면)` | `def(키, 이름, 화면)` — 화면키 → 화면 파일 연결만 |

```text
[DB TB_SCR_MENU + TB_SCR_AUTH_MENU] ─▶ GET /api/web/menu/my ─▶ useNavStore.menus(toTree)
     ├─▶ topItems → AppHeader(상단 메뉴) · IconRail(아이콘 줄)      activeTop = 대메뉴ID(홈 = HOME_TOP)
     ├─▶ SidePanel(topId) → 중메뉴 폴더 · 소메뉴(화면키 있으면 열림 / 없으면 '준비 중')
     └─▶ findMenuByScreen → 탭 제목(navigation·ScreenHost) · 브레드크럼(PageFrame) · 화면 검색(GlobalSearch)
```

## 1. 준비 — DB

| 테이블 | 쓰는 컬럼 | 읽기/쓰기 | 비고 |
|---|---|---|---|
| `TB_SCR_MENU` | 전부(백엔드 `MenuService` 가 읽음) | 읽기 | 이 변경으로 백엔드·SQL 은 바뀌지 않음 |
| `TB_SCR_AUTH_MENU` | 권한 플래그 | 읽기 | 조회 권한이 있는 화면·그 화면을 가진 그룹만 내려옴 |

- **제2부의 이관 SQL(`db/change/menu_three_level.sql`)을 먼저** 돌려 둡니다. 이제 내장 메뉴가 없으므로 개발 DB 에 메뉴 행이 없으면 사이드 패널이 비어 "보이는 메뉴가 없습니다" 가 뜹니다.
- 메뉴가 안 보이면: ① 메뉴관리 화면에 그 메뉴가 '사용'인지 ② 권한관리 → 메뉴 권한 탭에서 내 권한코드에 '조회'·'사용' 체크가 있는지 ③ 그룹(대·중메뉴)은 보이는 화면이 하나라도 있어야 보입니다(서버 `MenuService` 규칙).

## 2. 만들 파일 — 빨간줄 없이 가는 순서

> **원칙: 불리는 파일을 먼저, 부르는 파일을 나중에.** 단, 이번에는 메뉴 상태(F1)의 이름을 바꾸므로(`activeL1` → `activeTop` 등) **F1 을 저장한 뒤 F11 까지는 부르는 쪽 파일에 빨간줄이 잠깐 남습니다**. 그 파일을 차례로 고치면 사라지고, F13 이후에는 지운 파일(F16·F17)만 빨간줄이 남는데 지우면 끝납니다. 확인은 마지막 ✅ 한 번.
> - **새** = 부록 A 의 전체 소스로 새 파일. **diff** = `-` 줄 지우고 `+` 줄 넣기. **삭제** = 파일을 지움.

### 2-1. 프론트 (`Frontend/nh-atms-web`)

| # | 구분 | 파일 | 왜 이 순서 |
|---|---|---|---|
| F1 | diff | `src/stores/useNavStore.ts` | 메뉴 상태가 모두의 원천 → 먼저(여기서 `menuSeed`·`l1`·`env.menuSource` 를 더 이상 부르지 않음) |
| F2 | 새 | `src/components/layout/topItems.ts` | F1 의 `HOME_TOP`·`topNodes` 를 씀. 헤더·아이콘 줄·패널이 부름 |
| F3 | diff | `src/components/layout/AppHeader.tsx` | F1·F2 를 씀 |
| F4 | diff | `src/components/layout/IconRail.tsx` | 〃 |
| F5 | diff | `src/components/layout/SidePanel.tsx` | F1·F2 를 씀(`topId` 로 받음) |
| F6 | diff | `src/components/layout/AppShell.tsx` | F5 의 `topId` 를 넘김 → F5 다음 |
| F7 | diff | `src/components/layout/SettingsDrawer.tsx` | F1 에서 없앤 `menuSource` 를 안 씀 |
| F8 | diff | `src/components/layout/GlobalSearch.tsx` | F2 아이콘 · F1 `flattenScreens` |
| F9 | diff | `src/components/page/PageFrame.tsx` | F1 `findMenuByScreen`(l1·folder) — `l1Label` 을 안 씀 |
| F10 | diff | `src/screens/navigation.ts` | F1 `setActiveTop`·`topId`, F12 의 새 `screenPath(키)` 와도 맞음 |
| F11 | diff | `src/components/layout/ScreenHost.tsx` | 〃 (`parseScreenPath` 결과에서 slug 를 안 씀) |
| F12 | diff | `src/screens/registry.ts` | 이제 `l1.ts`·`L1Slug` 를 부르는 곳이 여기·F13 만 남음 |
| F13 | diff | `src/types/screen.ts` | `L1Slug`·`L1Def` 삭제(F12 가 더 이상 안 씀) |
| F14 | diff | `src/config/env.ts` | `menuSource` 삭제(F1·F7 이 더 이상 안 씀) |
| F15 | diff | `src/vite-env.d.ts` | 〃 |
| F16 | 삭제 | `src/screens/l1.ts` | 부르는 곳이 모두 없어진 뒤 |
| F17 | 삭제 | `src/screens/menuSeed.ts` | 〃 |
| F18 | diff | `.env.development` | 설정 줄 정리(빨간줄과 무관) |
| F19 | diff | `.env.production` | 〃 |
| F20 | diff | `.env.example` | 〃 |
| ✅ | | `npm run check` | 타입검사·lint 오류 0 |

- 내 PC 의 `.env.development.local` 에 `VITE_MENU_SOURCE` 가 남아 있어도 해는 없습니다(아무도 읽지 않음). 지워도 됩니다.
- 백엔드는 바뀌지 않습니다.

## 3. 가져다 쓴 공통 모듈 (새로 만들지 않음)

| 모듈 | 파일 | 이 변경에서 쓴 곳 | 쓰는 법 한 줄 |
|---|---|---|---|
| 메뉴 API | `src/api/endpoints/menu.ts` `menuApi.my` | F1 | 내 메뉴 트리(계약 §1 `MenuNode`) |
| 메뉴 아이콘 | `src/components/layout/icons.tsx` `MenuIcon`·`ICONS` | F2~F5·F8 | 이름으로 아이콘(`HomeOutlined`·`AppstoreOutlined`) |
| 경로 → 화면키 | `src/screens/registry.ts` `parseScreenPath` | F1 `toTree` | 메뉴의 `MENU_PATH` → 화면키(잠정 Q-05) |
| 화면 열기 | `src/screens/navigation.ts` `useOpenScreen` | 패널·검색·즐겨찾기 | 화면키 하나로 탭 열기 + 이동 |

## 4. 화면별 설명

### 4-1. 상단 메뉴 · 아이콘 줄 (`topItems.ts`, `AppHeader.tsx`, `IconRail.tsx`)

- 칸 = `'홈'`(고정, 메뉴 테이블에 없음) + `topNodes(menus)` — 서버가 내려 준 최상위 그룹(대메뉴) 전부, DB `SORT_NO` 순.

```ts
export const topItems = (menus: MenuTreeNode[]): TopItem[] => [
  { id: HOME_TOP, label: '홈', railLabel: '홈', icon: HOME_ICON },
  ...topNodes(menus).map((n) => ({ id: n.menuId, label: n.menuNm, railLabel: n.menuNm.slice(0, 2), icon: TOP_MENU_ICON })),
];
```

- 칸을 누르면 `setActiveTop(대메뉴ID)` — 사이드 패널이 그 대메뉴로 바뀜. 홈은 `/home` 으로 이동. 사이드바를 접었으면 아이콘 줄에 마우스를 올려 플라이아웃.
- 왜: 대메뉴를 메뉴관리에서 넣으면 **코드 수정 없이** 상단에 나타나야 해서. 아이콘 컬럼이 없어 아이콘은 하나(결정 3).

### 4-2. 사이드 패널 (`SidePanel.tsx`)

- 구성(위 → 아래): 즐겨찾기 상자 → 제목(대메뉴 이름, 홈이면 '홈') → 중메뉴 폴더(접기·펴기) 안에 소메뉴 → 나·환경설정.
- 소메뉴 한 칸(`Item`): 화면키가 있으면 `Leaf`(누르면 열림, ★ 즐겨찾기), 없으면 `Pending`(흐림·안 열림, 툴팁 '화면 준비 중 (경로)').
  - 화면키가 없는 경우 = DB 에는 메뉴가 있지만 `screenIds.ts`·`registry.ts` 에 그 화면이 아직 없음(개발 전) 또는 `MENU_PATH` 오타.
- 안내(`NoMenus`): 메뉴를 받았는데 대메뉴가 하나도 없거나, 고른 대메뉴 아래가 비었을 때 "보이는 메뉴가 없습니다. 메뉴관리(메뉴)·권한관리(메뉴 권한)를 확인하세요." + [다시 시도](`fetchMenus(true)`). 받기 실패(서버 오류)는 오류 문구 + [다시 시도](원래 있던 것).

### 4-3. 탭 제목 · 브레드크럼 · 화면 검색 (`navigation.ts`, `ScreenHost.tsx`, `PageFrame.tsx`, `GlobalSearch.tsx`)

- `findMenuByScreen(menus, 화면키)` 가 `{ menuNm, l1(대메뉴명), folder(중메뉴명), topId }` 를 줍니다.
- 탭 제목 = `menu?.menuNm ?? def?.title ?? 화면키` — DB 이름이 먼저, 메뉴에 없는 화면(홈·팝업·URL 로 연 화면)만 `registry` 이름.
- 브레드크럼 = `[l1, folder, 이름]` — 메뉴에 없으면 이름만.
- 상단 강조 = 그 화면의 `topId`(메뉴에 없는 화면이면 그대로, 홈이면 홈).
- 화면 검색 결과의 경로 줄 = `대메뉴 › 중메뉴 › 메뉴명`.

### 4-4. `registry.ts` · `types/screen.ts` · 설정

- `def(화면키, 이름, () => import('./sample/XxxPage'))` — 남은 역할은 **화면키 → 화면 파일**(코드가 있어야 열리므로)과 메뉴에 없을 때 쓸 이름뿐.
- URL 은 그대로 `/{slug}/{kebab}` = 메뉴의 `MENU_PATH`. slug 는 화면키 앞부분일 뿐 메뉴 구조와 무관 → 새 slug 를 코드 어디에 등록할 필요 없음(예전 `L1Slug`·`isL1Slug` 검사 삭제).
- `VITE_MENU_SOURCE`·`env.menuSource` 삭제 — 개발·운영 모두 서버 메뉴만.

## 5. 백엔드 요약

| API | 권한 | 하는 일 |
|---|---|---|
| `GET /api/web/menu/my` | 로그인 사용자(세션) | 바뀌지 않음 — `TB_SCR_MENU`(사용 Y) + 내 권한의 `TB_SCR_AUTH_MENU`. 화면은 조회 권한이 있어야, 그룹은 보이는 하위가 있어야 내려옴(관리자는 전부) |

## 6. 검사 · 확인

```bash
cd Frontend/nh-atms-web
```

```bash
npm run check
```

화면에서 눌러 볼 순서(제2부 이관 SQL 을 돌린 뒤, 실제 백엔드로 `npm run dev`)

1. 상단에 '홈' + DB 대메뉴(예: 견본)가 보이는지. 메뉴관리에서 새 대메뉴 + 그 아래 화면을 넣고 권한을 주면 **새로고침 없이**(저장 직후) 상단에 생기는지.
2. 대메뉴를 누르면 사이드 패널에 중메뉴 폴더·소메뉴가 보이는지. 화면 코드가 없는 소메뉴는 흐린 '준비 중'인지.
3. 메뉴관리 화면을 열면 탭 제목·브레드크럼이 DB 이름(견본 > 견본 화면 > 메뉴관리)인지. 메뉴관리에서 메뉴명을 바꾸면 다음에 열 때 바뀐 이름인지.
4. 권한관리 → 메뉴 권한에서 내 권한의 체크를 모두 끄면(관리자가 아닌 계정) "보이는 메뉴가 없습니다" + [다시 시도] 가 뜨는지.
