# 폐쇄망 PC 에서 고쳐 FileZilla 로 올려 적용하기 (2026-11-09)

> 전제: 폐쇄망 PC 에 **반입한 `Common-ATMS-main.zip` 을 푼 폴더**가 그대로 있다 (2026-11-09 오전 판 — `deploy-war.sh` md5 `0f26acf3bbd1d153c6cd48c1d95f13d8`).
> 그 뒤 GitHub 에서 바뀐 것(포트 없는 재기동, 메뉴 0~18 안전장치, 새 앱에 NH-ATMS 모듈 5개 포함)을 **새로 반입하지 않고** PC 에서 직접 고쳐 올린다.
> 서버 명령은 사진 보고 한 줄씩. `sudo`·`su`·`ssh` 는 치지 않는다.
> 경로 표기: PC 는 `Common-ATMS-main\…` (zip 푼 폴더), 서버는 `/home/atmswas/…`.

## 한눈에 보기

| 단계 | 고칠 파일 (PC) | 올릴 곳 (FileZilla) | 적용 |
|---|---|---|---|
| 0 | — | — | 편집기·FileZilla 설정, 서버 상태 조회 |
| 1 | `deploy\jeus\deploy-war.local.sh` (새 파일 35줄) | ws01 `/home/atmswas/deploy/` | 2-4 |
| 2 | `deploy\jeus\deploy-war.sh` (149번 줄 위에 1줄) | ws01 `/home/atmswas/atms/deploy/jeus/` | 2-4 |
| 3 | `deploy-war.env` (새로 또는 서버 것 받아서) | ws01 `/home/atmswas/deploy/` | 2-4 |
| 4 | — (서버 명령만) | — | 암호화 키·로그 폴더·앱 폴더 |
| 5 | `deploy\webtob\http.m.atms-jsv.txt` (registration-id 받은 뒤) | WebtoB 담당에게 전달 (wb01 `~/deploy/kit/deploy/webtob/` 은 보관용) | WebtoB 담당 |
| 6 | 내부 Gitea 사본 (앱 소스) — 모듈 5개용 공통 보강(GlobalIdGenerator·jeus-common.yml), 개인번호 | FileZilla 아님 → `git push` | Jenkins |
| 7 | — | — | 첫 배포·확인 |

---

## 0. 준비

### 0-1. 편집기 (Notepad++ 기준)

- 메뉴 **편집 → 줄 끝 문자 변환 → Unix (LF)**, **인코딩 → UTF-8** (BOM 없음). `.sh`·`.env`·`.yml`·`.gradle`·`.java` 모두.
- 메모장으로 고쳤다면 서버에서 `sed -i 's/\r$//' <파일>` 로 CR 을 지운다 (각 단계에 들어 있음).

### 0-2. FileZilla

- **전송 → 전송 유형 → 바이너리**. 접속 계정 `atmswas`.
- 덮어쓰기 묻는 창이 나오면 **덮어쓰기**. 올리기 전에 서버 쪽 원본을 PC 로 한 번 내려받아 두면 되돌리기 쉽다.

### 0-3. 서버 상태 조회 (ws01, 읽기만 — 결과를 사진으로)

```bash
md5sum ~/atms/deploy/jeus/deploy-war.sh
```

```bash
ls -l ~/deploy/
```

```bash
ls -ld ~/c*
```

```bash
ls -l /home/atmswas/keys
```

```bash
ls -ld /WASlogs/jeusATMSlogs/nbatmsltws01/app
```

```bash
ps -u jeusatms -o args= | grep -o 'obj_name=[^ ]*' | sort
```

```bash
podman ps --format '{{.Names}} {{.Status}}'
```

| 결과 | 정상 | 아니면 |
|---|---|---|
| md5 | `0f26acf3bbd1d153c6cd48c1d95f13d8` | 다른 값(2026-11-09 관찰 `d047fc0a…` = 10-26 전후 옛 판, 반입 zip 의 배포킷을 아직 안 올린 상태) → **1-0** 으로 PC 반입본 배포킷을 먼저 올린다 |
| `~/deploy/` | `deploy-war.env` 있으면 3단계에서 받아서 고침, 없으면 새로 | — |
| `~/c*` | `catms cajou cabat cfile cifin cifex` (`jeus` 그룹, `drwxr-s---`) | 옛 이름(`catms-journal` 등)이면 INFRA_MANUAL 10-2, 없으면 첫 배포 때 자동 생성 |
| keys | `field-k202611.key.pem`·`.pub.pem` (`atmswas jeus`, `-rw-r-----`) | 없으면 4단계 |
| 로그 폴더 | 있음, atmswas 가 읽을 수 있음 | 없으면 WAS 담당 (INFRA_MANUAL 10-4) |
| obj_name | `nbatmsltws01_` + `atms01 ajou01 abat01 file01 ifin01 ifex01` | 2026-11-09 관찰: `abat01` 없음 → WAS 담당 기동 요청 (DB 준비 전이면 실패해도 정상, 3단계 `HEALTH_SKIP`) |
| podman | `atms-gitea`·`atms-jenkins` Up | INFRA_MANUAL T 표 |

---

## 1-0. (서버 `deploy-war.sh` md5 가 다를 때) 반입본 배포킷 먼저 올리기

서버 배포킷이 반입 zip 보다 옛 판이면 [ROLLOUT_PLAN.md](ROLLOUT_PLAN.md) **B** 그대로: 옛 파일을 남겨 두고 PC `Common-ATMS-main\` 의 `bin`·`conf`·`deploy`·`docs`·`README.md` 를 `/home/atmswas/atms/` 에 덮어쓴 뒤 CR 제거·`chmod`·`bash -n`.

```bash
cp -p ~/atms/deploy/jeus/deploy-war.sh ~/deploy/deploy-war.sh.before-kit
```

```bash
cp -p ~/atms/deploy/jeus/swap-deploy.sh ~/deploy/swap-deploy.sh.before-kit
```

(업로드·ROLLOUT B 명령 후)

```bash
md5sum ~/atms/deploy/jeus/deploy-war.sh
```

→ `0f26acf3bbd1d153c6cd48c1d95f13d8` 이면 아래 1~3 을 그대로. `conf/*.env`·`~/deploy/*`·`infra/`·`release/` 는 zip 에 없어 그대로 남는다. 배포 키(`authorized_keys`) 경로도 같아 다시 등록하지 않는다.

## 1. `deploy-war.local.sh` 새로 만들기 (PC)

PC 에서 `Common-ATMS-main\deploy\jeus\deploy-war.local.sh` 를 새로 만들고 아래 35줄을 그대로 넣는다 (GitHub 같은 이름 파일과 같음, 2026-11-10 판: 앱 로그와 함께 JEUS 서버 로그 `<서버>/JeusServer.log` 도 보고, 실패하면 두 로그 끝 40줄을 출력).
반입본 `deploy-war.sh` 의 일부 함수(정지·기동 확인, 메뉴 넣기)를 덮어써서 **포트 없이 프로세스 + 앱 로그로** 확인하고, **재컴파일 전 jeusRstart 에는 메뉴를 넣지 않게** 한다.

```bash
# ~/deploy/deploy-war.local.sh : 포트 없이 재기동 확인 (서버에서 직접 고칠 때, 반입본 deploy-war.sh 에 한 줄로 읽힘)
JL="${JEUS_LOG_DIR:-/WASlogs/jeusATMSlogs/nbatmsltws01/app}"
JS="${JEUS_SRV_LOG_DIR:-/WASlogs/jeusATMSlogs/nbatmsltws01}"
DENY_MENUS="3 17 18"
proc_up() { ps -u jeusatms -o args= 2>/dev/null | grep -Eq -- "obj_name=$(cfg SERVER "$1")( |$)"; }
server_up() { proc_up "$1"; }
health_code() { if proc_up "$1"; then echo 200; else echo 000; fi; }
fsize() { wc -c 2>/dev/null < "$1" || echo 0; }
newpart() { local o="$2"; [[ $(fsize "$1") -lt $o ]] && o=0; tail -c +$(( o + 1 )) "$1" 2>/dev/null || true; }
show_logs() {
  local f; for f in "${JL}/$1.log" "${JS}/$(cfg SERVER "$1")/JeusServer.log"; do
    echo "----- $f (끝 40줄)"; tail -n 40 "$f" 2>/dev/null || echo "(없음 또는 읽기 불가)"
  done; echo "-----"
}
jeus_menu() {
  local n; for n in "$(cfg STOP_MENU "$m")" "$(cfg BOOT_MENU "$m")"; do
    [[ "$n" =~ ^[0-9]{1,2}$ ]] || die "메뉴 번호 오류: $n"
    [[ " ${DENY_MENUS} " == *" $n "* ]] && die "쓰지 않는 메뉴: $n"
  done
  [[ "${JEUS_RSTART}" -nt "${JEUS_RSTART%/*}/jeusmenu.c" ]] || die "jeusRstart 재컴파일 전 (jeusmenu.c 가 더 새로움) → 메뉴 안 넣음"
  grep -aq -- " $(cfg SERVER "$m")'" "${JEUS_RSTART}" || die "jeusRstart 에 $(cfg SERVER "$m") 없음 → 메뉴 안 넣음"
  OFF=$(fsize "${JL}/${m}.log"); OFF2=$(fsize "${JS}/$(cfg SERVER "$m")/JeusServer.log")
  log "jeusRstart 메뉴 $1"
  printf '%s\n' "$1" | "${JEUS_RSTART}" 9>&- || true
}
wait_ready() {
  local i r
  [[ " ${HEALTH_SKIP} " == *" $1 "* ]] && return 0
  for (( i = 0; i <= BOOT_WAIT; i += 10 )); do
    r=$( { newpart "${JL}/$1.log" "${OFF:-0}"; newpart "${JS}/$(cfg SERVER "$1")/JeusServer.log" "${OFF2:-0}"; } | grep -oE 'APPLICATION FAILED TO START|Application run failed|Started Atms[A-Za-z]+Application' | tail -1 || true)
    case "$r" in Started*) log "기동 확인: $1"; return 0 ;; ?*) log "기동 실패: $1 ($r)"; show_logs "$1"; return 1 ;; esac
    sleep 10
  done
  log "기동 확인 실패 (${BOOT_WAIT}초): $1 (앱 로그·JEUS 서버 로그에 Started 없음)"; show_logs "$1"; return 1
}
```

- 주의해서 볼 글자: `" $(cfg SERVER "$m")'"` — 큰따옴표 안에 **공백**, 끝에 **작은따옴표 `'`** 하나 (실행 파일 안의 `'boot nbatmsltws01_xxx'` 문자열을 찾는다)
- `9>&-` (숫자 9, 꺾쇠, &, 빼기), `-nt` (newer than)
- 들여쓰기는 공백 2칸 (탭이어도 동작은 같음)

**올릴 곳:** ws01 `/home/atmswas/deploy/deploy-war.local.sh`  (`atms` 아래가 아니라 **`deploy`** 폴더)

## 2. `deploy-war.sh` 에 한 줄 넣기 (PC)

PC `Common-ATMS-main\deploy\jeus\deploy-war.sh` 를 연다. **149번 줄**이 `lock() {` 이다 (Notepad++ `Ctrl+G` → 149).
그 **바로 위(148번 빈 줄 다음)** 에 아래 한 줄을 새 줄로 넣는다 → 넣은 줄이 149번, `lock() {` 이 150번이 된다.

```bash
if [ -f "${DEPLOY_HOME}/deploy-war.local.sh" ]; then . "${DEPLOY_HOME}/deploy-war.local.sh"; fi
```

넣은 뒤 모양:
```
147	[[ -x "${SWAP}" ]] || die "swap-deploy.sh 없음 또는 실행 권한 없음: ${SWAP}"
148	
149	if [ -f "${DEPLOY_HOME}/deploy-war.local.sh" ]; then . "${DEPLOY_HOME}/deploy-war.local.sh"; fi
150	lock() {
```

- 다른 줄은 고치지 않는다. 이 자리여야 하는 이유: 함수 정의가 모두 끝난 뒤라 local.sh 의 함수가 이긴다 (env 파일은 77번 줄에서 너무 일찍 읽혀 함수를 덮을 수 없다)
- 거부 메뉴(83번 줄 `DENY_MENUS="3 7 8"`)는 고치지 않아도 된다 — local.sh 가 `3 17 18` 로 덮어쓴다

**올릴 곳:** ws01 `/home/atmswas/atms/deploy/jeus/deploy-war.sh` (덮어쓰기)

## 3. `deploy-war.env` (PC)

- 0-3 에서 `~/deploy/deploy-war.env` 가 **있었으면**: FileZilla 로 PC 에 내려받아 고친다 (같은 이름 줄이 있으면 그 줄을 고치고, 없으면 끝에 추가)
- **없었으면**: PC 에서 새 파일 `deploy-war.env` 를 만든다

넣을 내용 (`=` 앞뒤 공백 없이):
```
SERVER_web=nbatmsltws01_atms01
SERVER_journal=nbatmsltws01_ajou01
SERVER_batch=nbatmsltws01_abat01
SERVER_file=nbatmsltws01_file01
SERVER_ifin=nbatmsltws01_ifin01
SERVER_ifex=nbatmsltws01_ifex01
STOP_MENU_web=6
BOOT_MENU_web=5
STOP_MENU_journal=8
BOOT_MENU_journal=7
STOP_MENU_batch=10
BOOT_MENU_batch=9
STOP_MENU_file=12
BOOT_MENU_file=11
STOP_MENU_ifin=14
BOOT_MENU_ifin=13
STOP_MENU_ifex=16
BOOT_MENU_ifex=15
HEALTH_SKIP="atms-batch"
```

- 메뉴 번호는 `jeusmenu.c`(2026-11-09 13:52) 기준. **실행 파일 `jeusRstart` 가 재컴파일되기 전에는 1단계 안전장치가 메뉴를 넣지 않는다** (지금 실행 파일의 7·8 = WebtoB 기동·정지) → 미리 넣어 둬도 안전
- `HEALTH_SKIP="atms-batch"`: abat01 이 안 떠 있고 batch 는 DB(`T_` 테이블·Spring Batch 메타)가 없으면 기동 실패 → DB 준비 후 이 줄 삭제
- 예전 이름 `JEUS_STOP_MENU`·`JEUS_BOOT_MENU`·`JEUS_URL` 줄이 있으면 지운다 (경고만 나오지만 헷갈림)
- `PORT_` 로 시작하는 줄은 넣지 않는다 (포트를 모름)

**올릴 곳:** ws01 `/home/atmswas/deploy/deploy-war.env`

### 1~3 서버 적용 (ws01, atmswas)

```bash
sed -i 's/\r$//' ~/atms/deploy/jeus/deploy-war.sh ~/deploy/deploy-war.local.sh ~/deploy/deploy-war.env
```

```bash
chmod 700 ~/atms/deploy/jeus/deploy-war.sh
```

```bash
chmod 600 ~/deploy/deploy-war.local.sh ~/deploy/deploy-war.env
```

```bash
bash -n ~/atms/deploy/jeus/deploy-war.sh && bash -n ~/deploy/deploy-war.local.sh && bash -n ~/deploy/deploy-war.env && echo OK
```

```bash
grep -n -B1 '^lock() {' ~/atms/deploy/jeus/deploy-war.sh
```

```bash
~/atms/deploy/jeus/deploy-war.sh status
```

- `OK` 가 나와야 한다. `grep` 은 `149:if [ -f …` 와 `150-lock() {` 두 줄
- `status`: 모듈마다 `nbatmsltws01_…` 이름, `응답 200`(프로세스 있음) / `000`(없음 — 지금은 batch), `재기동 메뉴 6/5` …
- 재기동 시험은 **재컴파일 확인 뒤** (아래 명령에 6개 이름이 모두 나오면):

```bash
grep -a -o 'nbatmsltws01_[a-z0-9]*' /JEUS/jeusATMS/jeusMENU/jeusRstart | sort | uniq -c
```

```bash
~/atms/deploy/jeus/deploy-war.sh restart atms-if-external
```

- 첫 시험은 화면과 무관한 `atms-if-external`(ifex01) 하나로. 앱 배포 전이라 로그에 `Started` 가 없으면 "기동 확인 실패 (600초)" 가 정상이다 — 이때는 `ps` 에 `obj_name=nbatmsltws01_ifex01` 이 다시 보이는지만 본다
- 재컴파일 전에 치면 `jeusRstart 재컴파일 전 … 메뉴 안 넣음` 으로 아무것도 하지 않고 끝난다 (정상)

되돌리기: `~/deploy/deploy-war.local.sh` 를 지우면 149번 줄은 아무 일도 하지 않는다. 다음 배포킷을 반입해 `deploy-war.sh` 를 덮으면(그 판에는 같은 기능이 들어 있음) `deploy-war.local.sh` 는 지운다.

---

## 3-1. (2026-11-09 사용자 결정) local.sh·env 를 배포킷 폴더 `~/atms/deploy/jeus/` 에 두는 경우

PC `deploy\jeus\deploy-war.sh` 두 줄의 `${DEPLOY_HOME}` 을 `${HERE}`(= 스크립트가 있는 폴더)로 바꾼다:

| 줄 | 바꾼 뒤 |
|---|---|
| 77 | `if [[ -f "${HERE}/deploy-war.env" ]]; then . "${HERE}/deploy-war.env"; fi` |
| 149 | `if [ -f "${HERE}/deploy-war.local.sh" ]; then . "${HERE}/deploy-war.local.sh"; fi` |

- 두 파일은 PC `Common-ATMS-main\deploy\jeus\` 에 두고 `deploy` 폴더째 `/home/atmswas/atms/` 에 올린다. 옛 `~/deploy/deploy-war.env` 는 읽히지 않으므로 `.old` 로 이름을 바꿔 둔다
- 주의: 나중에 GitHub 새 배포킷 zip 을 올리면 `deploy-war.sh` 가 `~/deploy/deploy-war.env` 를 읽는 판으로 바뀐다 → 그때 env 를 `~/deploy/` 로 옮기고 local.sh 는 지운다

## 4. 서버 명령만 (ws01, atmswas — 0-3 결과에 따라)

| 0-3 결과 | 할 일 |
|---|---|
| keys 없음 | [POST_HANDOVER_EDITS.md](POST_HANDOVER_EDITS.md) **0-2** 명령 그대로 (없으면 atms01 기동 실패). 개인키는 PC 로 옮기거나 사진 찍지 않는다 |
| 로그 폴더 없음 | WAS 담당에 `/WASlogs/jeusATMSlogs/nbatmsltws01/app` (jeusatms 쓰기, jeus 그룹 읽기) 요청 — 없으면 기동 확인을 프로세스만으로 함 |
| 옛 앱 폴더 이름 | [INFRA_MANUAL.md](INFRA_MANUAL.md) **10-2** |
| npm 캐시 미합침 | [ROLLOUT_PLAN.md](ROLLOUT_PLAN.md) **D** |

---

## 5. WebtoB `http.m` 블록 (PC → WebtoB 담당)

PC `Common-ATMS-main\deploy\webtob\http.m.atms-jsv.txt` — **WAS 담당에게 서버 6개의 WebtoB registration-id 를 받은 뒤** 고친다 (`domain.xml` 이 이제 atmswas 로 읽히지 않음).

| 줄 | 지금 값 | 바꿀 값 |
|---|---|---|
| 36~41 (`*SERVER`) 맨 앞 이름 | `atms01` `ajou01` `abat01` `file01` `ifin01` `ifex01` | 각 서버의 registration-id |
| 44~56 (`*URI`) `SvrName = …` | 같은 6개 이름 | 위와 같은 값으로 (한 이름이 여러 줄에 나옴 — Notepad++ `Ctrl+H` "모두 바꾸기"는 **단어 단위** 체크) |
| 26 `nbatmsltwb01` | 노드 이름 | WebtoB 담당이 다른 노드 이름을 쓰면 |
| 33·36~41 `atms_jsvg` | 새 JSV 그룹 | 기존 그룹을 쓰기로 했으면 그 이름 (33번 `*SVRGROUP` 줄은 담당이 추가하지 않음) |

- 이 파일은 **붙여 넣을 내용**이다. 우리 계정으로는 `http.m` 을 읽지도 쓰지도 못하므로 PC 파일을 WebtoB 담당에게 그대로 전달한다
- 보관용으로 wb01 `/home/atmswas/deploy/kit/deploy/webtob/http.m.atms-jsv.txt` 에도 올려 둔다 (**`~/docs` 아래에는 절대 두지 않는다** — 9000 으로 내려받아짐)
- 적용 확인 (ws01): `ss -tn | grep ':19000'` 에 ESTAB, 앱 배포 뒤 `curl -s -o /dev/null -w "%{http_code}\n" http://16.88.122.112:9000/v3/api-docs` → `200`

---

## 6. 앱 소스 — 내부 Gitea 사본 (PC, FileZilla 아님)

> 앱 소스는 서버에 직접 올리지 않는다. 내부 Gitea `NH-ATMS/nh-atms` 에 push → Jenkins 가 빌드·배포.
> 아직 Gitea 를 새 앱으로 바꾸지 않았다면 [PRE_CARRYIN_CHECK.md](PRE_CARRYIN_CHECK.md) **4장**의 "복사" 단계에서 `Common-ATMS-main\nh-atms-common-main\` 안의 것을 복사한 **직후, commit 전에** 6-1~6-3 을 한다.
> 이미 바꿨다면 Gitea clone 폴더에서 6-1~6-3 을 하고 새 commit 으로 push.
> 아래 `<사본>` = Gitea clone 폴더 (맨 위에 `Backend`·`Frontend` 가 있는 곳).

### 6-1. NH-ATMS 업무 모듈 5개 — 사용자가 폐쇄망에서 넣은 구조 그대로 (2026-11-10 결정)

> 2026-11-10 사용자 결정: 5개 모듈은 **새 앱의 `atms-common` 하나를 함께 쓴다** (각 `build.gradle` 은 원본 그대로 `implementation project(':atms-common')`).
> 예전에 이 절에 있던 `atms-common-nh` 방식(공통 모듈을 따로 복사)은 **쓰지 않는다**. GitHub 저장소도 같은 구조로 바꿨다.
> 5개 모듈이 쓰는데 새 `atms-common` 에 없던 것 2가지만 보태면 된다 (아래 ①·②). 배포킷(Jenkins·`deploy-war.sh`)은 고칠 것이 없다.

이미 되어 있어야 하는 것 (사용자가 폐쇄망에서 한 작업):
- `<사본>\Backend\nh-atms-api\` 에 `atms-journal`·`atms-batch`·`atms-file`·`atms-if-internal`·`atms-if-external` 폴더 (NH-ATMS 에서 복사, `build`·`.gradle`·`bin`·`out` 폴더는 지움)
- `settings.gradle` 끝에 `include 'atms-journal'` … `include 'atms-if-external'` 5줄
- 5개 `build.gradle` 8번 줄 = `implementation project(':atms-common')` (원본 그대로)

① **`GlobalIdGenerator` 복사** — if-external 컴파일 오류(`package kr.co.nh.atms.common.util does not exist`)의 원인
- 원본: `Common-ATMS-main\NH-ATMS\Backend\nh-atms-api\atms-common\src\main\java\kr\co\nh\atms\common\util\GlobalIdGenerator.java`
- 붙여 넣을 곳: `<사본>\Backend\nh-atms-api\atms-common\src\main\java\kr\co\nh\atms\common\util\` (`util` 폴더가 없으면 만든다). 파일 내용은 고치지 않는다

② **`<사본>\Backend\nh-atms-api\atms-common\src\main\resources\jeus-common.yml`** — `data-dir: /home/atmswas/data` 줄 **바로 아래**(같은 `jeus:` 안, 공백 2칸 들여쓰기)에 추가. journal·file 의 `application-jeus.yml` 이 이 값들을 읽는다 (없으면 서버에서 `Could not resolve placeholder 'jeus.url.ifin'` 등으로 기동 실패):
```yaml
  webtob-url: http://16.88.122.112:9000
  url:
    web:     ${jeus.webtob-url}
    journal: ${jeus.webtob-url}
    batch:   ${jeus.webtob-url}
    file:    ${jeus.webtob-url}
    ifin:    ${jeus.webtob-url}
    ifex:    ${jeus.webtob-url}
  agent-entry:
    host: CHANGE_ME_L4_VIP
    journal-port: 8081
    file-port: 8083
    schema: http
```
- 들여쓰기는 **스페이스**만 (탭 금지). `webtob-url`·`url`·`agent-entry` 는 `host:`·`log-dir:`·`data-dir:` 와 같은 칸
- `jwt:` 는 넣지 않는다 (새 공통에는 JWT 가 없고, 5개 모듈 코드도 JWT 를 쓰지 않음)

③ (선택) `<사본>\Backend\nh-atms-api\atms-web\src\test\java\kr\co\nh\atms\common\message\ApiMessageCodeGuardTest.java` **74번 줄** — PC 에서 `gradlew build`/`test` 를 돌릴 때만 필요 (Jenkins 는 `bootWar` 만 하므로 영향 없음):
```
            for (Path mod : mods.filter(p -> p.getFileName().toString().startsWith("atms-")).toList()) {
```
→
```
            for (Path mod : mods.filter(p -> java.util.Set.of("atms-common", "atms-web").contains(p.getFileName().toString())).toList()) {
```
- 이유: 이 검사는 새 앱 규칙(문구는 메시지 코드로)을 보는데 5개 모듈은 원본 규칙(한글 문구 직접)이다

알아 둘 것 (공통 하나를 같이 쓰는 결과, 2026-11-10 이 저장소에서 시험):
- 6개 WAR 빌드·테스트 통과. jeus 프로파일 기동: web·journal·file·if-internal·if-external 성공(`/v3/api-docs` 200), batch 는 DB 없으면 실패(설계대로)
- 보안 설정은 새 공통 것이 적용된다: journal(`/api/journal`·`/api/agent`·`.do`)·file(`/api/file`·`/atmf`)·ifin·ifex 경로는 각 모듈 자체 설정으로 허용 그대로. **batch 의 `/api/batch/**` 는 401** (원본도 NH-ATMS JWT 가 필요했고 그 화면이 없으므로 당장 영향 없음 — 배치 스케줄러는 그대로 돈다)

### 6-2. 서버용 설정값 (내부 Gitea 에만 — GitHub 에는 올리지 않음)

| 파일 | 줄 | 지금 | 바꿀 값 |
|---|---|---|---|
| `Backend\nh-atms-api\atms-web\src\main\resources\application-jeus.yml` | 25 | `    per-no: CHANGE_ME` | `    per-no: <화면 고정 사용자 개인번호>` — WAS 담당이 atms01 JVM 옵션 `-Datms.fixed-user.per-no=…` 를 넣어 주면 고치지 않아도 됨. 둘 다 없으면 화면 `/api/**` 전부 401 `ME_001` |
| `Backend\nh-atms-api\atms-common\src\main\resources\jeus-common.yml` | 6-1 ② 의 `host: CHANGE_ME_L4_VIP` | | **지금은 그대로** — L4 VIP 를 받으면 (에이전트 연결 단계) |

- JNDI 이름이 `dsATMS01` 이 아니면 같은 `jeus-common.yml` 의 `jndi-name` 한 곳만 바꾼다 (POST_HANDOVER_EDITS G)

### 6-3. PC 에서 빌드 확인 (선택, 권장)

`<사본>\Backend\nh-atms-api` 에서 (IntelliJ Gradle 창 또는 cmd):
```
gradlew.bat bootWar -Pjeus --offline
```
- `BUILD SUCCESSFUL`, 그리고 `atms-web`·`atms-journal`·`atms-batch`·`atms-file`·`atms-if-internal`·`atms-if-external` 각 `build\libs\` 에 `.war` (6개)
- `package kr.co.nh.atms.common.util does not exist` → 6-1 ① 파일 위치(폴더 `util`)

### 6-4. push

PRE_CARRYIN_CHECK 4장 순서대로 `git add -A` → `git commit -m "업무 모듈 5개용 공통 보강 (GlobalIdGenerator, jeus-common)"` → `git push`. 웹훅으로 두 잡이 `build-only` 로 돈다.
- 백엔드 잡 보관 파일이 **WAR 6개**여야 한다 (예전 2개 → 6개)

---

## 7. 첫 배포 · 확인

| # | 할 일 | 확인 (ws01) |
|---|---|---|
| 1 | 백엔드 잡 `MODE=build-deploy`, `MODULE=atms-web` | 콘솔 `교체 완료: atms-web`. 재컴파일 전이면 재기동은 UNSTABLE(노란색) → WAS 담당에 atms01 재기동 요청 |
| 2 | 재기동 뒤 | `grep -E 'Started|APPLICATION FAILED' /WASlogs/jeusATMSlogs/nbatmsltws01/app/atms-web.log \| tail -3` 에 `Started AtmsWebApplication` |
| 3 | WebtoB 적용 뒤 | `curl -s -o /dev/null -w "%{http_code}\n" http://16.88.122.112:9000/v3/api-docs` → `200`, `curl -s http://16.88.122.112:9000/api/web/me` 에 이름 |
| 4 | 나머지 모듈 `MODULE=` 하나씩 (journal → file → if-internal → if-external, batch 는 DB 준비 뒤) | 각 `<모듈>.log` 에 `Started Atms…Application` |
| 5 | 화면 잡 `MODE=build-deploy` | PC 브라우저 `http://16.88.122.112:9000` (PC→9000 방화벽 필요) |

- 로그에 `Started` 가 없고 모든 주소 404 → JEUS Jakarta 미지원 의심 (INFRA_MANUAL 10-5)
- `[field-crypto] … 키 파일이 없습니다` → 4단계 keys
- `NameNotFoundException … dsATMS01` → WAS 담당 (T63)
- `ME_001` → 6-2 개인번호

## 되돌리기 한눈에

| 고친 것 | 되돌리기 |
|---|---|
| `deploy-war.local.sh` | 서버에서 `rm ~/deploy/deploy-war.local.sh` (149번 줄은 파일이 없으면 아무 일도 안 함) |
| `deploy-war.sh` | PC 의 반입 원본(149번 줄 넣기 전)을 다시 올리고 `chmod 700` |
| `deploy-war.env` | 0-2 에서 받아 둔 원본을 다시 올림 |
| 앱 소스 | PC 에서 `git revert <커밋>` → push → 재배포 |

## 7. Jenkins 백엔드 잡 스크립트 맞추기 (2026-11-10)

2026-11-10 확인: 잡에 붙어 있던 것은 10-23 오후 판(`restart --rollback` 있음, `if (rc == 4)` 없음). 기준을 하나로 만들기 위해 **반입 zip 판(10-27, 183줄)을 통째로 붙여 넣고** 아래 4곳만 고친다.

1. PC `Common-ATMS-main\deploy\jenkins\pipelines\backend.Jenkinsfile` 를 메모장/VS Code 로 연다 (183줄인지 확인)
2. 고칠 곳 (줄 번호는 이 파일 기준)

| 줄 | 지금 | 바꾼 뒤 |
|---|---|---|
| 38 | `    timeout(time: 60, unit: 'MINUTES')` | `    timeout(time: 120, unit: 'MINUTES')` |
| 46 | `choices: ['build-only', 'build-deploy', …` | `choices: ['build-deploy', 'build-only', …` (앞 두 개 순서만) |
| 168 | `echo "WAR ${n}개 교체 완료 (재기동 안 함). 반영: MODE=restart"` | 그대로 두고 **다음 줄에** `                  exit 5` 추가 |
| 174 | `          if (rc == 4) {` | **그 위에** 아래 블록 A 추가, 그리고 176 줄 `} else if (rc != 0) {` **앞에** 블록 B 추가 |

블록 A (174 줄 위):
```
          if (rc != 0 && params.MODE != 'status') {
            sshagent(credentials: ['ws01-deploy-key']) {
              sh(returnStatus: true, script: 'echo "===== ws01 상태 (실패·미반영 진단)"; ssh -o BatchMode=yes -o StrictHostKeyChecking=yes -o ConnectTimeout=10 "${WS01_SSH}" status')
            }
          }
```
블록 B (`} else if (rc != 0) {` 앞):
```
          } else if (rc == 5) {
            unstable('WAR 교체만 함 (RESTART 꺼짐) — 서버에는 아직 반영 안 됨. MODE=restart 로 반영')
```
3. 고친 파일 전체를 복사 → Jenkins `nh-atms-backend` → 구성 → Pipeline script 내용을 **모두 지우고** 붙여 넣기 → 저장
4. "Build with Parameters" 로 한 번 실행 (`MODE=build-deploy`, `MODULE=all`, **`RESTART` 체크**) — 이 실행 뒤부터 기본값(build-deploy)이 적용된다
5. 확인: 콘솔에 `=== atms-…war` 6번 → `jeusRstart 메뉴 …` → `기동 확인`/`기동 실패`. 끝 색: 초록(반영 성공) / 노랑(반영 안 됨) / 빨강(실패, 로그 40줄이 위에 찍힘)

## 8. 다음 반입 때 적용할 것 (2026-11-10 사용자 결정: 그때까지 서버는 지금 상태 유지)

지금 서버·Jenkins 상태 (2026-11-10 기준):
- ws01 `~/atms/deploy/jeus/deploy-war.sh` = 반입본(10-27) + 77·149번 줄 `${HERE}` 수정, `deploy-war.local.sh` = **28줄 판**, `deploy-war.env` = 같은 폴더
- Jenkins `nh-atms-backend` = 반입본(10-27) + 7장 4곳 수정 (rc 5·ws01 status)

다음 반입 zip 으로 적용할 것:

| 대상 | 내용 | 적용 뒤 정리 |
|---|---|---|
| `deploy/jeus/deploy-war.sh` | 포트 없는 확인·메뉴 안전장치·JEUS 서버 로그 확인·실패 시 로그 40줄 출력이 **내장**된 판 | `~/atms/deploy/jeus/deploy-war.local.sh` 삭제, `deploy-war.env` 를 `~/deploy/` 로 옮김(새 판은 `~/deploy/deploy-war.env` 를 읽음), 77·149 수정은 새 판에 필요 없음 |
| `deploy/jeus/deploy-war.local.sh` 35줄 판 | 새 `deploy-war.sh` 를 반입하지 못할 때만 대신 씀 (28줄 판 교체) | — |
| `deploy/jenkins/pipelines/backend.Jenkinsfile` | 7장 4곳 수정 + **9장 `MODULE=auto`(바뀐 모듈만 배포)** 가 들어간 판 — 2026-11-10 사용자 결정: 9장은 잡에 손으로 넣지 않고 이 반입 때 통째로 적용. 그때까지 push 는 6개 모두 배포·재기동, 한 모듈만은 Build with Parameters 에서 `MODULE` 선택 | 배포킷 갱신 ⑤ 대로 잡에 통째로 붙여 넣기 → 한 번 실행 |
| `deploy/jeus/deploy-war.env.example`, `docs/*` | 서버 이름(`nbatmsltws01_*`)·메뉴 0~18 반영 | — |

## 9. 백엔드 잡: 바뀐 모듈만 배포 (2026-11-10 — **다음 반입 때 적용**, 사용자 결정. 아래 표는 손으로 넣을 때 참고용)

원인: 웹훅(push) 빌드는 기본값 `MODULE=all` 로 돌아 atms-web 만 고쳐도 WAR 6개 교체 + 6개 서버 재기동.
고친 뒤: 기본값 `MODULE=auto` — 직전 성공 빌드 커밋과 비교해 `Backend/nh-atms-api/atms-<모듈>/` 이 바뀐 모듈만 빌드·교체·재기동.
`atms-common`·루트 `build.gradle`·`settings.gradle`·`gradle/` 등 모듈 폴더 밖이 바뀌면 6개 전부(공통을 함께 쓰므로). 바뀐 모듈이 없으면 빌드·배포 생략.

잡 → 구성 → Pipeline script 에서 **Ctrl+F 로 찾아 바꾼다** (줄 번호 대신 찾을 글자 기준 — 지금 잡 = 반입본 10-27 + 7장 4곳). 고치기 전 스크립트 전체를 PC 메모장에 복사해 둔다.

| # | 찾을 글자 | 바꿀 내용 |
|---|---|---|
| 1 | `choice(name: 'MODULE', choices: ['all',` | `choice(name: 'MODULE', choices: ['auto', 'all',` (맨 앞에 `'auto', ` 추가) |
| 2 | `        checkout([$class: 'GitSCM',` (소스 stage) | 아래 블록 A (checkout 4줄 전체를 바꿈) |
| 3 | `when { expression { params.MODE == 'build-only' \|\| params.MODE == 'build-deploy' } }` (**2곳**: 빌드·묶기) | `when { expression { (params.MODE == 'build-only' \|\| params.MODE == 'build-deploy') && env.TARGETS } }` |
| 4 | `if [ "${MODULE}" = "all" ]; then TASK="bootWar"; else TASK=":${MODULE}:bootWar"; fi` | `TASK=""; for m in ${TARGETS}; do TASK="${TASK} :${m}:bootWar"; done` |
| 5 | `m="$(basename "$(dirname "$(dirname "$(dirname "$w")")")")"` (묶기) | 그 **다음 줄에** `            case " ${TARGETS} " in *" ${m} "*) ;; *) continue ;; esac` 추가 |
| 6 | `when { expression { params.MODE != 'build-only' } }` (배포) | `when { expression { params.MODE != 'build-only' && (params.MODE != 'build-deploy' \|\| env.TARGETS) } }` |
| 7 | `if (params.MODE == 'rollback' && params.MODULE == 'all') {` 로 시작하는 줄 | 그 줄을 아래 블록 B 2줄로 |
| 8 | `if [ "${MODULE}" = "all" ]; then ${SSH} "${WS01_SSH}" status;` | 앞부분만 `if [ "${MODULE}" = "all" ] \|\| [ "${MODULE}" = "auto" ]; then ${SSH} "${WS01_SSH}" status;` |
| 9 | `case "$f" in "${MODULE}"-[0-9]*) ;; *) [ "${MODULE}" = "all" ] \|\| continue ;; esac` | `case " ${TARGETS} " in *" ${f%%-[0-9]*} "*) ;; *) continue ;; esac` |

블록 A (소스 stage 의 `checkout([...])` 4줄 대신, `steps {` 와 그 닫는 `}` 사이):
```
        script {
          def scmVars = checkout([$class: 'GitSCM',
            branches: [[name: "*/${env.SRC_BRANCH}"]],
            userRemoteConfigs: [[url: env.GITEA_REPO, credentialsId: 'gitea-jenkins-bot']],
            extensions: [[$class: 'PathRestriction', includedRegions: 'Backend/.*', excludedRegions: '']]])
          def ALL = 'atms-web atms-journal atms-batch atms-file atms-if-internal atms-if-external'
          def base = scmVars.GIT_PREVIOUS_SUCCESSFUL_COMMIT ?: ''
          if (params.MODULE != 'auto') {
            env.TARGETS = (params.MODULE == 'all') ? ALL : params.MODULE
          } else if (!(params.MODE in ['build-deploy', 'build-only'])) {
            env.TARGETS = ALL
          } else if (!base) {
            env.TARGETS = ALL
          } else {
            def mods = []
            def every = false
            sh(returnStdout: true, script: "git diff --name-only ${base} HEAD -- Backend/nh-atms-api").trim().split('\n').each { f ->
              def p = f.split('/')
              if (!f) { return }
              if (p.size() > 3 && ALL.split(' ').contains(p[2])) { mods << p[2] } else { every = true }
            }
            env.TARGETS = every ? ALL : mods.unique().join(' ')
          }
          echo "대상 모듈: ${env.TARGETS ?: '(없음 — 바뀐 모듈 없음, 빌드·배포 생략)'}  (비교 기준 ${base ?: '없음'})"
        }
```
블록 B:
```
          if (params.MODE == 'rollback' && params.MODULE in ['all', 'auto']) { error 'rollback 은 MODULE 을 하나 고르세요' }
          if (params.MODE == 'restart' && params.MODULE == 'auto') { error 'restart 는 MODULE 을 all 또는 하나로 고르세요' }
```

저장 뒤:
- Build with Parameters 로 한 번 실행해 매개변수 등록 (`MODE=status`, `MODULE=all` 이면 배포 없이 등록만)
- 이후 push: 콘솔 앞부분에 `대상 모듈: atms-web  (비교 기준 <커밋>)` → 그 모듈만 빌드·교체·재기동
- "Scripts not permitted" 가 뜨면 Jenkins 관리 → In-process Script Approval → Approve (split·contains 등)
- 배포가 실패한 뒤 다른 빌드가 성공하면 비교 기준이 넘어가므로, 실패한 모듈은 `MODULE=<그 모듈>` 로 다시 배포한다
