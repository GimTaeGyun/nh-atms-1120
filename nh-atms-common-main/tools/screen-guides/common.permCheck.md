> 반입 전 마지막 권한 점검(2026-11-17): 화면별 권한 접근을 서버·화면 코드로 다시 대조하고, 권한관리에서 바꾼 권한이 **로그인 사용자별 메뉴에 정확히 반영되는지** 확인했다. 찾은 틈 하나를 고치고 자동 시험 2종을 더했다.
> - 반입: diff 2개 + 새 시험 1개 + 시험 diff 1개. 제10부까지 넣은 뒤 넣습니다.
> - 경로 줄임: src/ = Frontend/nh-atms-web/src/, web/ = Backend/nh-atms-api/atms-web/src/main/java/kr/co/nh/atms/web/

## 0. 한눈에 보기

- 사용자 결정(2026-11-17): **권한관리 수정은 지금처럼 메뉴 권한 플래그를 따른다**(A 전용으로 바꾸지 않음, 잠정 Q-21 그대로). 관리자(A)는 플래그 없이도 모두 허용.
- 결과: 서버·화면 권한은 화면마다 맞았다. **틈 1개** — 권한관리에서 저장한 뒤 *내* 왼쪽 메뉴·버튼 권한이 새로 고침 전까지 옛 값이었다 → 저장 성공 때 내 정보·내 메뉴를 조용히 다시 읽게 고침.

| 화면 | 조회(QRY) | 등록(REG) | 수정(UPD) | 삭제(DEL) | 그 밖 |
|---|---|---|---|---|---|
| 대시보드 `main.dashboard` | API 5개 | — | — | — | 범위는 레벨(A·T·L) 규칙 `DashboardRules` |
| 공지사항 `sample.notice` | 목록·상세 | 등록 | 수정 | 삭제 | + 관리자 아니면 본인 글·내 사무소만(403 NTC_002·003) |
| 기기정보 `sample.mcnInfo` | 목록·상세 | 등록 | 수정 | 삭제 | |
| 기기모델코드 `sample.mcnModel` | 목록 | 일괄 저장 안 추가분 | 일괄 저장 안 수정분 | 일괄 저장 안 삭제분 | 일괄 저장은 서비스가 종류별 검사 |
| 권한관리 `sample.authMgt` | 목록·상세·부서·권한코드·메뉴 권한 | 사용자 권한 등록 / 메뉴 권한 새 행 | 사용자 권한 수정 / 메뉴 권한 바꿈 | 사용자 권한 삭제 | 메뉴 권한 저장은 서비스가 종류별 검사 |
| 메뉴관리 `sample.menuMgt` | 목록 | 등록 | 수정 | 삭제 | |
| 코드관리 `sample.codeMgt` | 코드구분·상세코드 목록·1건 | 등록 | 수정 | 삭제 | |
| 사용기록조회 `sample.useLog` | 목록·상세 | — | — | — | + 관리자 아니면 내 사무소 기록만 |
| (누구나) | 공통 콤보 `/lookup/*` · 내 정보 `/me` · 내 메뉴 `/menu/my` · 로그인 `/auth/*` · 엑셀 기록 `/use-log/excel` | | | | 로그인 사용자면 누구나 |

- 화면 버튼은 모두 `ActionBar`·`AuthButton`·`FormModalFooter`(+`usePermission`)로 흐려진다 — 화면에 권한 없는 맨 버튼 없음(공통 모듈 견본 화면 제외).
- 메뉴 보임(서버 `MenuService` + `mapper/menu/MenuMapper.xml`): 메뉴 `USE_YN='Y'` + 내 권한코드의 **사용 중인**(`TB_SCR_AUTH_MENU.USE_YN='Y'`) 행의 조회 플래그 = Y 인 화면만, 보이는 화면이 없는 중·대메뉴는 숨김. 관리자 = 모두.
- 권한 바꾼 뒤 반영: 서버는 사용자 권한 쓰기 뒤 사용자 캐시를 비우고(`CurrentUserLoader.evictAll`), 메뉴 권한은 요청마다 다시 읽는다. **다른 사용자**는 다음 화면 열기(앱 시작)부터, **나**는 이번 수정으로 저장 즉시.

## 1. 준비 — DB

- 없음(SQL 변경 없음).

## 2. 만들 파일 — 빨간줄 없이 가는 순서

| # | 구분 | 파일 | 왜 이 순서 |
|---|---|---|---|
| F1 | diff | `src/screens/popup/AuthUserEditModal.tsx` | `useAfterAuthChange` 를 여기서 만듦(F2 가 부름) |
| F2 | diff | `src/screens/sample/AuthMgtParts.tsx` | F1 다음 ✅ `npm run check` |
| B1 | 새 | `MenuAuthCoverageTest.java` | 서로 독립 |
| B2 | diff | `MenuServiceTest.java` | ✅ `gradlew.bat build --offline` |

## 3. 가져다 쓴 공통 모듈

| 부품 | 파일 | 무엇 |
|---|---|---|
| `useNavStore.fetchMenus(true)` | `src/stores/useNavStore.ts` | 내 메뉴 다시 읽기(메뉴관리와 같은 방식) |
| `meApi.get` + `useAuthStore.setMe` | `src/api/endpoints/me.ts`, `src/stores/useAuthStore.ts` | 내 정보를 조용히 다시 읽기 — `loadMe(true)` 는 앱 전체를 '불러오는 중'으로 바꿔 열린 탭이 닫혀서 쓰지 않음 |

## 4. 화면별 설명

### 4-1. 권한관리 (부서권한·개인권한·메뉴 권한 탭, 등록·수정 팝업)

- 저장·삭제 성공 → 목록 다시 읽기 + 내 정보·내 메뉴 다시 읽기. 내 권한코드의 메뉴 권한에서 '사용'을 끄면, 저장 즉시 왼쪽 메뉴에서 그 화면이 사라지고 버튼도 흐려진다.
- 권한은 예전처럼 메뉴 권한 플래그로(사용자 결정 — A 전용 아님).

## 5. 백엔드 요약 (시험만)

- `MenuAuthCoverageTest`: 모든 컨트롤러 API 가 `@RequireMenuAuth` 를 갖고 필요 플래그가 메서드 이름과 같은지(select = QRY · insert = REG · update = UPD · delete = DEL), 아니면 예외 목록(누구나 5종 · 서비스에서 검사하는 일괄 저장 2개)에 있는지. 컨트롤러마다 화면키 하나. 새 API 에서 권한 표시를 빠뜨리면 빌드가 실패한다.
- `MenuServiceTest`: 대 → 중 → 소 3단계에서 조회권한 없는 가지 전체 숨김. ('사용 해제' 행을 SQL 글자로 확인하던 시험은 2026-11-18 삭제 — 폐쇄망 `MenuMapper.xml` 과 글자 모양이 달라 실패, 사용자 결정. 그 규칙은 SQL 자체와 데모로 확인)

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

1. 관리자로 권한관리 → 메뉴 권한 탭 → 권한 L 조회 → 공지사항 '사용' 끄기 → [저장].
2. L 권한 사용자로 열기 → 왼쪽 메뉴에 공지사항이 없는지(대시보드 바로가기 공지사항은 흐림).
3. 다시 켜고 저장 → L 사용자 메뉴에 다시 보이는지.
4. 내 권한코드의 메뉴 권한을 바꾸면 새로 고침 없이 내 왼쪽 메뉴가 바로 바뀌는지.
