# 프론트 퍼블리싱 외주 저장소(nh-atms-design) — 분리 · 최신화 · 재병합 절차

작성 2026-11-20. 대상: 저장소 관리자(우리). 외주업체용 안내는 디자인 레포의 `README.md`·`CONTRIBUTING.md`.

## 0. 한눈에 보기

```
nh-atms-1120 (통합, 이 저장소)                        nh-atms-design (외주, GitHub)
nh-atms-common-main/Frontend/nh-atms-web/  ──① 분리──▶  저장소 루트 (같은 파일 + 가짜 API + 안내문서)
                                           ──② 최신화─▶  sync/upstream-YYYYMMDD 브랜치 → main 에 병합
                                           ◀─③ 재병합──  허용 폴더의 변경만 patch 로 가져옴
```

| 결정 (2026-11-20 사용자) | 내용 |
|---|---|
| 가짜 API | **MSW, 디자인 레포에만** (`src/mocks/`, `main.tsx` 분기, `.env.development` `VITE_USE_MOCK=true`). 통합 레포는 2026-11-11 결정(목 삭제) 그대로 |
| 외주 수정 범위 | **모양만**: `src/screens/**`·`src/components/**`(JSX·className·라벨, props/export 유지), `src/theme/**`, 부품 CSS(**화면 전용 CSS 는 특별한 경우만** — 2026-11-20 사용자 결정, 새 화면 CSS 는 점검에서 '확인 필요' + PR 에 이유), `src/assets/**`, `public/**`, 더미 `src/mocks/data|handlers/**` |
| 분리·병합 방식 | **`git subtree split`**(공통 조상 유지) + **허용 폴더만 `git diff | git apply --directory`** |
| 강제 장치 | `scripts/check-paths.mjs` + GitHub Actions `PR 점검`(범위·로직 추가·삭제·타입검사·린트·빌드), PR 템플릿, CODEOWNERS |

핵심 원리: `git subtree split` 은 같은 원본 커밋에서 **언제나 같은 커밋 ID** 를 만든다. 그래서 디자인 레포의 `main` 과 통합 레포에서 새로 뽑은 split 브랜치가 공통 조상을 가지며, split 브랜치(= 통합 레포 현재 프론트와 똑같은 트리)를 기준으로 diff 하면 **외주가 바꾼 것만** 정확히 나온다.

---

## 1. 최초 분리 (2026-11-20 완료분 기록 — 다시 할 때도 같은 순서)

통합 레포 루트에서 (Git for Windows 의 Git Bash 도 `git subtree` 포함):

```bash
git checkout main
```
```bash
git pull
```
```bash
git subtree split --prefix=nh-atms-common-main/Frontend/nh-atms-web -b ui-split
```
```bash
git remote add design https://github.com/GimTaeGyun/nh-atms-design.git
```
```bash
git push design ui-split:main
```

그다음 디자인 레포에 "디자인 레포 전용 파일"을 커밋한다 (2026-11-20 커밋 `퍼블리싱 저장소 설정…` 으로 반영됨):

| 파일 | 내용 | 재병합 |
|---|---|---|
| `src/mocks/**` | MSW 가짜 API (`browser.ts`, `utils.ts`, `data/`, `handlers/`) — 9개 화면 + 로그인 견본의 모든 API | 제외 |
| `public/mockServiceWorker.js` | `npx msw init public --no-save` 생성물 (msw 2.15.0) | 제외 |
| `src/main.tsx` | `VITE_USE_MOCK === 'true'` 일 때만 `worker.start()` 후 렌더 | 제외 |
| `.env.development` | `VITE_USE_MOCK=true` 추가 | 제외 |
| `eslint.config.js` | `globalIgnores` 에 `public/mockServiceWorker.js` 추가 | 제외 |
| `src/screens/_template/**`, `src/mocks/data/templateList.ts` | **새 화면 견본**(2026-11-20 추가) — 외주가 복사해 새 화면을 만드는 틀. View(표시 전용)·Columns·EditModal·Page(더미 껍데기) 분리 | 제외 |
| `src/screens/registry.ts`, `screenIds.ts` 의 `// [디자인 레포 전용]` 줄 | 견본 화면 등록 1줄씩 | 3절에서 `sed` 로 삭제 |
| `README.md`, `CONTRIBUTING.md`, `.github/**`, `scripts/check-paths.mjs`, `.gitattributes`, `.editorconfig` | 외주 안내·규칙·자동 점검 | 제외 |

`package.json` 은 바꾸지 않았다(`msw` 는 이미 devDependencies 에 있음). 운영 빌드(`npm run build`)에는 가짜 API 가 들어가지 않는다(확인함).

### 1-1. 보안 체크리스트 (분리·최신화 때마다)

| # | 확인 | 방법 / 2026-11-20 결과 |
|---|---|---|
| 1 | **디자인 레포가 Private** 인가 | GitHub → Settings → General → Danger Zone → Change visibility. **2026-11-20 확인 시 Public 이었음 → Private 전환 필요** |
| 2 | 백엔드·DB·배포킷 파일이 없는가 | split 은 `nh-atms-common-main/Frontend/nh-atms-web` 아래만 담는다. `git ls-tree -r --name-only ui-split` 로 확인 (205개, 모두 프론트) |
| 3 | `.env` 실파일·키·비밀번호가 없는가 | `.gitignore` 가 `.env`, `.env.*.local` 제외. 아래 grep 결과 0건 |
| 4 | 내부 IP·서버 주소가 없는가 | 아래 grep. `.env.development` 의 `DEV_PROXY_TARGET` 은 `http://localhost:8080` 만 (폐쇄망 WebtoB 주소를 적은 적이 있으면 그 커밋 이력까지 넘어감 주의) |
| 5 | npm 레지스트리가 공개 레지스트리인가 | `package-lock.json` 의 `resolved` 가 모두 `https://registry.npmjs.org` (471건) |
| 6 | **커밋 메시지·작성자**에 내부 정보가 없는가 | split 은 프론트 폴더를 건드린 커밋의 **메시지·작성자 이메일까지** 그대로 옮긴다. 아래 `git log` 로 확인. 2026-11-20 은 `first commit` 1개(작성자 `unknown <xorbsdut@naver.com>`, 바꾸면 공통 조상이 끊겨 그대로 둠). 디자인 레포 전용 커밋의 작성자는 임시 메일 `nh-atms-admin <nh-atms-admin@example.com>`(사용자 결정) |
| 7 | 주석의 내부 정보 수준이 괜찮은가 | 주석에 테이블명(`TB_NOTICE` 등)·내부 결정 이력이 있다. 주석을 지우면 재병합 diff 가 커지므로 **지우지 않는 대신 Private + 외주 계약상 비밀유지**로 관리 |
| 8 | 외주 계정 권한이 최소인가 | Settings → Collaborators: 외주 계정은 **Write**(Admin·Maintain 아님) |

검사 명령 (통합 레포 루트, split 브랜치를 만든 뒤):

```bash
git grep -nIE "16\.8[89]\.[0-9]+\.[0-9]+|10\.248\.[0-9.]+|192\.168\.[0-9.]+|BEGIN [A-Z ]*KEY|password\s*[:=]\s*['\"][^'\"]+" ui-split -- . ':!package-lock.json'
```
```bash
git log --format='%h %an <%ae> %s' design/main..ui-split
```
첫 번째는 결과가 없어야 하고, 두 번째로 넘어갈 커밋의 메시지·작성자를 눈으로 확인한다(최초 분리 때는 `design/main` 대신 `ui-split` 만).

### 1-2. GitHub 설정 (관리자가 웹에서)

| 설정 | 위치 | 비고 |
|---|---|---|
| Private 전환 | Settings → General → Danger Zone | 필수 |
| 외주 계정 초대 (Write) | Settings → Collaborators | |
| Actions 허용 | Settings → Actions → General → Allow all actions | PR 자동 점검용. Private 저장소는 무료 플랜 월 2,000분 |
| main 보호 | Settings → Branches(또는 Rules) → `main`: PR 필수, `PR 점검` 통과 필수, Code Owners 검토 필수 | **Private 저장소의 브랜치 보호는 GitHub 유료 플랜(Pro/Team) 필요.** 무료면 설정 불가 → 규칙(README)·자동 점검·관리자만 병합으로 운영 |
| 병합 방식 | Settings → General → Pull Requests: **Squash merge 만** 허용 권장 (Merge commit 은 sync 용으로 관리자만 사용) | |

---

## 2. 외주 진행 중 최신화 (통합 → 디자인)

우리 쪽에서 프론트를 바꿨을 때(새 화면, API 연동 변경 등) 외주 작업 기반을 맞춰 준다. **재병합 직전에도 반드시 한 번** 한다.

```bash
git checkout main
```
```bash
git pull
```
```bash
git subtree split --prefix=nh-atms-common-main/Frontend/nh-atms-web -b ui-split
```
(이미 `ui-split` 이 있으면 먼저 `git branch -D ui-split`)
```bash
git fetch design main
```
```bash
git merge-base --is-ancestor ui-split design/main && echo "이미 최신 — 할 일 없음"
```
최신이 아니면 1-1 보안 검사 두 명령을 실행한 뒤:
```bash
git push design ui-split:refs/heads/sync/upstream-20261201
```
GitHub 에서 `sync/upstream-20261201` → `main` PR 을 만들고 **"Create a merge commit"** 으로 병합한다(Squash 금지 — 공통 조상이 끊긴다). `sync/` 브랜치는 범위 점검을 건너뛰고 타입검사·빌드만 한다.
충돌이 나면 디자인 레포 쪽에서 해결한다(외주 변경과 우리 변경이 같은 줄). 병합 후 외주에게 "main 을 내 브랜치에 merge" 하라고 알린다(README 5절).

---

## 3. 재병합 (디자인 → 통합)

전제: 외주 PR 이 모두 디자인 레포 `main` 에 병합됐고, **2절 최신화를 방금 끝냈다.**

```bash
git checkout main
```
```bash
git pull
```
```bash
git branch -D ui-split
```
(`not found` 오류는 무시)
```bash
git subtree split --prefix=nh-atms-common-main/Frontend/nh-atms-web -b ui-split
```
```bash
git fetch design main
```
```bash
git merge-base --is-ancestor ui-split design/main && echo OK
```
`OK` 가 안 나오면 **멈춘다** — 2절 최신화가 안 된 것이다(그대로 진행하면 우리 쪽 최근 변경을 되돌리는 patch 가 만들어진다).

```bash
git diff ui-split design/main -- src/components src/screens src/theme src/assets public ':!public/mockServiceWorker.js' ':!src/screens/_template' > ui.patch
```
```bash
git apply --index --directory=nh-atms-common-main/Frontend/nh-atms-web ui.patch
```
견본 화면 등록 줄(`[디자인 레포 전용]` 표시)을 지운다:
```bash
sed -i '/\[디자인 레포 전용\]/d' nh-atms-common-main/Frontend/nh-atms-web/src/screens/registry.ts nh-atms-common-main/Frontend/nh-atms-web/src/screens/screenIds.ts
```
```bash
git add -A nh-atms-common-main/Frontend/nh-atms-web/src/screens
```
```bash
git status
```

| 확인 | 명령 / 기준 |
|---|---|
| 들어온 파일이 허용 폴더뿐인가 | `git status` — `src/components`, `src/screens`, `src/theme`, `src/assets`, `public` 아래만 |
| 로직 줄이 섞이지 않았나 | `git diff --cached -- '*.ts' '*.tsx'` 에서 `@/api`, `@/stores`, `useQuery`, `useMutation`, `onClick` 처리 내용 변경을 눈으로 확인 |
| 견본 줄이 남지 않았나 | `git grep -n "디자인 레포 전용\|_template" -- nh-atms-common-main/Frontend/nh-atms-web/src` → 결과 없어야 함 |
| 새 화면 Page 가 더미를 쓰는가 | `git grep -n "@/mocks" -- nh-atms-common-main/Frontend/nh-atms-web/src` → 나온 `*Page.tsx` 는 실제 API 로 다시 쓴다: `api/endpoints/<area>.ts`·`domain/<area>.ts` 추가 → Page 에서 `usePagedSearch`·`useMutation` 결과를 View props(`rows`·`total`·`searched`·`loading`·`pagination`·`onSearch`·`onSave`(mutateAsync)·`onDelete`)에 연결. View·Columns·EditModal 은 그대로 둔다(Columns 의 `interface` 는 domain 타입으로 바꿔도 됨). 바꾸기 전엔 타입검사가 실패하므로 실수로 배포될 일은 없다 |
| 빌드 | `cd nh-atms-common-main/Frontend/nh-atms-web` → `npm run check` → `npm run build` |
| 화면 | `npm run dev` + 내 PC 백엔드로 바뀐 화면을 직접 확인 |

문제없으면 커밋·푸시하고 `ui.patch` 는 지운다.

```bash
git commit -m "UI 퍼블리싱 반영 (nh-atms-design main 기준)"
```

### 3-1. 문제가 생기면

| 증상 | 원인 / 대처 |
|---|---|
| `merge-base --is-ancestor` 실패 | 2절 최신화를 하지 않았거나, sync PR 을 Squash 로 병합했다. 2절을 다시 하고 Merge commit 으로 병합 |
| `git apply` 가 `patch does not apply` | `ui-split` 을 새로 만들지 않았거나(이전 split 사용) 통합 레포에 커밋 안 한 프론트 변경이 있다. `git stash` → `ui-split` 재생성 → 다시 |
| 허용 폴더 밖 변경이 필요했던 PR | 디자인 레포에서 외주가 Issue 로 요청한 내용 → 통합 레포에서 우리가 직접 구현 |
| 외주가 규칙을 어긴 PR 이 이미 병합됨 | 디자인 레포에서 되돌리기(Revert) PR → 재병합 |

---

## 4. 재병합에서 제외되는 것 (요약)

`src/mocks/**`, `public/mockServiceWorker.js`, `src/screens/_template/**`, registry·screenIds 의 `[디자인 레포 전용]` 줄(sed), `src/main.tsx`, `.env*`, `eslint.config.js`, `package*.json`, `vite.config.ts`, `tsconfig*`, `index.html`, `README.md`, `CONTRIBUTING.md`, `.github/**`, `scripts/**`, `.gitattributes`, `.editorconfig` — 3절 diff 의 경로 지정이 이들을 자동으로 뺀다.

## 5. 검증 기록 (2026-11-20)

- 디자인 레포에서 `npm ci` → `npm run dev`: 백엔드 없이 홈·대시보드·견본 8개·로그인 화면 표시, [조회] 시 목록 자료 표시, API 4xx/5xx 0건 (Playwright). `mock=empty`/`error` 스위치 동작.
- `npm run check`(타입검사·린트), `npm run build` 통과. 빌드 결과물에 MSW 미포함.
- `scripts/check-paths.mjs`: CSS·라벨 변경 → 통과 / `src/api` 수정·부품 삭제·`useQuery` 추가 → 실패(종료코드 1).
- 재병합 모의 시험: 통합 레포에서 `http.ts` timeout·공지 제목 변경 + 디자인 레포에서 CSS·`DataGrid` 문구·새 부품·더미 추가 → 2절 sync(공통 조상 `c80046e`로 자동 병합) → 3절 재병합 결과 외주 변경 3개 파일만 반영, 우리 변경 유지, `src/mocks`·`main.tsx` 미반영.
- 콘솔의 `Accessing element.ref was removed in React 19`, 빌드의 `vendor-antd` 1100kB 경고는 기존부터 있던 것(외주 README FAQ 에 기재).
- (같은 날 추가) 새 화면 견본 `_template`: 브라우저에서 조회 57건·2페이지 이동·등록·삭제 동작, 콘솔 오류 0. 외주가 견본을 복사해 `mcn.mcnStatus` 화면을 만든 모의 시험 → `check-paths` 통과(Page 의 `@/mocks` 는 확인 필요), 3절 재병합 결과 `screens/mcn/` 5개 파일 + registry·screenIds 의 새 화면 1줄씩만 반영, 견본 폴더·견본 줄은 제외, 통합 레포 타입검사 오류는 `McnStatusPage.tsx` 의 `@/mocks` import 1건뿐(우리가 API 로 다시 쓸 곳).

## 6. 왜 MSW 를 유지하나 (2026-11-20 검토)

외주는 MSW 를 만들거나 고치지 않는다(새 화면은 Page 에서 더미를 직접 넘김). 그래도 디자인 레포에 MSW 가 필요한 이유:
1. 앱 시작 때 `/me`(사용자)·`/menu/my`(메뉴)를 받아야 화면 틀이 열린다 — 없으면 `MeGate` 에서 멈춤.
2. 공통 부품 일부가 내부에서 API 를 부른다(사무소 선택 `officeField` → `/lookup/offices`, 공통코드 `CodeSelect` → `/lookup/codes`, 사무소·사람·기기 검색 팝업 등). 새 화면이 이 부품을 쓰려면 응답이 있어야 한다.
3. 기존 9개 화면이 실제처럼 동작해야 외주가 모양·부품 사용법을 참고할 수 있다.

