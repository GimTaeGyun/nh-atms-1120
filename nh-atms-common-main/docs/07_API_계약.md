# API 계약 (프론트·백엔드·목이 똑같이 따르는 모양)

> **2026-11-10: atms-web 의 모든 API 경로 앞부분이 `/api/` → `/api/web/` 으로 바뀌었다** (WebtoB VHOST/URI 구성에 맞춤, 폐쇄망 사용자 결정). 화면은 `VITE_API_BASE=/api/web`. 이 문서의 경로는 모두 바뀐 경로로 적었다.

> 이 문서가 **응답 모양의 정본**이다. 백엔드 DTO, 프론트 `domain/*.ts` 는 여기 필드 이름·타입과 글자 하나까지 같아야 한다.
> 필드 이름 = 정본 컬럼명을 MyBatis `map-underscore-to-camel-case` 로 바꾼 camelCase(`OFC_CODE`→`ofcCode`). SQL 은 대문자로 정본 컬럼명 그대로(설계 §5.7 "SQL 작성 규칙").
> 날짜·시각은 문자열: 일자 `YYYYMMDD`, 일시 `YYYYMMDDHH24MISS`. (`TB_NOTICE.WRT_DATE` 는 DB 가 DATE 라서 SQL 에서 `TO_CHAR(WRT_DATE,'YYYYMMDD') AS WRT_DATE` 로 문자열로 내린다.)

## 0. 공통

```ts
ApiResponse<T> { success: boolean; message: string | null; code: string /* 성공 '0000' */; data: T }
PageResult<T>  { content: T[]; totalCount: number; page: number /*1부터*/; size: number; totalPages: number }
PageParams     { page?: number; size?: number; sort?: string }
BatchChanges<T>{ added: T[]; updated: T[]; deleted: T[] }
Audit          { crtDtm?: string; crtCmeno?: string; lschgDtm?: string; lsCmeno?: string }  // 감사 4컬럼(읽기 전용)
```

- 조회 응답 `message:null`. 쓰기 응답은 코드로 문구(`S002` 등록, `S003` 수정, `S004` 삭제, `S005` {0}건 삭제, `S001` 저장).
- 요청 헤더 `X-Screen-Key`(화면키), `X-Screen-Name`(encodeURIComponent) — 사용기록(§12)·권한 검사용.
- 권한 부족 403 `SYS_403`, 고정 사용자 문제 401 `ME_001`, 입력 오류 400 `VAL_*`, 없음 404.
- 견본 표의 "권한" 칸은 서버도 검사한다: 조회 GET 도 `@RequireMenuAuth(need=QRY)`, 쓰기는 REG·UPD·DEL(일괄 저장은 서비스에서 종류별). 공통 조회 `/api/web/lookup/*`·`/api/web/me`·`/api/web/menu/my` 는 검사하지 않는다.
- 입력 길이: 바이트 상한 칸(공지 제목·내용, 모델명)은 ASCII 1·그 밖 2바이트로 센다(잠정 Q-07, 서버 `DbBytes`·프론트 `utils/rules` `byteLength`/`maxBytes` 한 곳).

## 1. 나 · 메뉴

| 메서드 경로 | 응답 data |
|---|---|
| `GET /api/me` | `Me` |
| `GET /api/menu/my` | `MenuNode[]` (트리, 권한 붙음) |

```ts
Me {
  perNo: string; name: string;                 // TB_INSA.PER_NO, NAME
  samuso: string | null; samusoName: string | null;   // TB_INSA.SAMUSO, SAMUSO_NAME
  deptCode: string | null; ofcCode: string | null;    // TB_SCR_AUTH.DEPT_CODE, OFC_CODE (채택된 1행)
  kanm: string | null;                          // TB_OFC_REG_MGT.KANM (ofcCode 의 사무소명)
  auth: string | null;                          // TB_SCR_AUTH.AUTH
  adminYn: 'Y' | 'N';                           // 잠정: AUTH='A' 이면 Y
}
MenuNode {
  menuId: string; parentMenuId: string; menuNm: string;
  menuType: 'G' | 'M';                          // G 그룹, M 화면
  menuPath: string | null;                      // 화면 경로 '/sample/notice' (화면키 연결, 잠정 Q-05)
  sortNo: number; useYn: 'Y' | 'N';
  qryRolYn: 'Y' | 'N'; regRolYn: 'Y' | 'N'; updRolYn: 'Y' | 'N'; delRolYn: 'Y' | 'N'; exeRolYn: 'Y' | 'N';
  children: MenuNode[];
}
```

- 채택 규칙(`CurrentUserLoader.pickEffective`, 2026-11-11 사용자 결정): **개인권한 우선, 없으면 부서권한.**
  - 개인권한 = `TB_SCR_AUTH` 에서 `PER_NO = 내 개인번호`. 부서권한 = `PER_NO = '1'` 이고 `DEPT_CODE`·`OFC_CODE` 가 내 `TB_OFC_RAW`(`TB_INSA.SAMUSO = TB_OFC_RAW.SAMUSO`)의 `SAMUSO`·`ONLINE_CODE` 와 둘 다 같은 행.
  - 유효 = `APV_STS='Y'` 이고 `DUE_DATE` 가 비었거나 지금 이후. 유효한 개인권한이 있으면 그것만(부서와 섞지 않음), 없으면 유효한 부서권한. 같은 종류 여러 행이면 `AUTH` 오름차순 첫 행(잠정 Q-11).
- 메뉴 권한: 그룹(G)은 자식 중 하나라도 조회 가능하면 보임. 관리자(`adminYn='Y'`)는 5개 모두 Y.

## 2. 공통 조회 (`/api/web/lookup/*`, 모두 GET, 로그인 사용자면 누구나)

| 경로 | 파라미터 | 응답 data | 테이블 |
|---|---|---|---|
| `/api/web/lookup/offices` | `q?` (코드·사무소명 부분일치) | `Office[]` | TB_OFC_REG_MGT ⟕ TB_CO_SIDO |
| `/api/web/lookup/offices/{ofcCode}` | | `Office` (없으면 404 `LKP_404`) | 〃 |
| `/api/web/lookup/persons` | `ofcCode?`, `q?` (성명·개인번호) — 둘 다 없으면 400 `LKP_001` | `Person[]` (최대 200) | TB_INSA |
| `/api/web/lookup/persons/{perNo}` | | `Person` (없으면 404 `E404`) | 〃 |
| `/api/web/lookup/vendors` | | `Vendor[]` | 설계 Q6 (아래) |
| `/api/web/lookup/models` | `vendCode?`, `mcnType?` | `Model[]` | TB_CO_MODEL |
| `/api/web/lookup/srcs` | `vendCode?` | `Src[]` (`SRC` 순) | TB_NMS_SRC |
| `/api/web/lookup/machines` | `ofcCode` 필수(`V001_20`), `vendCode?`, `q?`(기번) | `Machine[]` | TB_MCN_INFO ⟕ TB_SCRT_INST_PST ⟕ TB_NMS_SRC |
| `/api/web/lookup/corners` | `ofcCode` 필수 | `Corner[]` | TB_SCRT_INST_PST |
| `/api/web/lookup/codes/{codeType}` | | `Code[]` (코드구분 `USED_YN`≠'N', `CODE` 순. `codeType`=`CODE_SECT`, `desc1~3`=`DATA1~3`, `sortOrd`=null — 잠정 Q-29) | TB_CODE_DTL ⋈ TB_CODE_MASTER (2026-11-16, 예전 TB_CO_CODE) |
| `/api/web/lookup/distinct/{target}` | | `DistinctValue[]` (값 오름차순) | 허용 목록(아래), 그 밖은 400 `LKP_002` |
| `/api/web/lookup/sido` | | `Sido[]` | TB_CO_SIDO |
| `/api/web/lookup/sigun` | `sidoCode` 필수 | `Sigun[]` | TB_CO_SIGUN |
| `/api/web/lookup/holidays` | `from`, `to` (YYYYMMDD, 최대 366일) | `Holiday[]` | TB_NMS_HDAY |

```ts
Office  { ofcCode: string; kanm: string | null; sidoCode: string | null; sidoNm: string | null }
Person  { perNo: string; name: string; jikeubName: string | null; samuso: string | null; samusoName: string | null }
Vendor  { vendCode: string; vendNm: string | null }
Model   { vendCode: string; mcnType: string; modelCode: string; modelNm: string | null }
Src     { src: string; srcNm: string | null; vendCode: string | null; komType: string | null }   // 기종(TB_NMS_SRC)
Machine { ofcCode: string; mcnNo: string; instPstCode: string | null; instPstNm: string | null;
          src: string | null; srcNm: string | null; komType: string | null; vendCode: string | null; useYn: 'Y' | 'N' }
Corner  { ofcCode: string; instPstCode: string; instPstNm: string | null; useYn: 'Y' | 'N' }
Code    { codeType: string; code: string; codeNm: string | null; desc1: string | null; desc2: string | null; desc3: string | null; sortOrd: number | null; useYn: 'Y' | 'N' }
DistinctValue { value: string }
Sido    { sidoCode: string; sidoNm: string | null }
Sigun   { sidoCode: string; sigunCode: string; sigunNm: string | null }
Holiday { hdayDt: string; dayType: number | null; hdayYn: string | null; closeYn: string | null }
```

- **기기사(Q6, 잠정)**: 설정 `atms.lookup.vendor-code-type` 이 있으면 `TB_CODE_DTL`(2026-11-16, 예전 `TB_CO_CODE`) 의 그 코드구분(`CODE→vendCode`, `CODE_NM→vendNm`). 없으면(기본) `TB_CO_MODEL`·`TB_MCN_INFO` 의 `DISTINCT VEND_CODE`, `vendNm=null` → 화면은 코드만 표시.
- **distinct 허용 목록** (target → 테이블.컬럼): `mcn-kom-type`→TB_MCN_INFO.KOM_TYPE, `mcn-op-type`→TB_MCN_INFO.OP_TYPE, `mcn-inout-type`→TB_MCN_INFO.INOUT_TYPE, `mcn-cnr-pst-spec`→TB_MCN_INFO.CNR_PST_SPEC_CODE, `mcn-vend-code`→TB_MCN_INFO.VEND_CODE, `model-mcn-type`→TB_CO_MODEL.MCN_TYPE. 매퍼는 target 마다 `<select id>` 를 따로 둔다(`${}` 금지).
- 인원 잠정 규칙(`LookupRules` 한 곳): 사무소 종속은 `TRIM(SAMUSO) = #{ofcCode}`, 퇴직자 제외 `RETIRE_DATE IS NULL OR RETIRE_DATE > 오늘`.

## 3. 공지사항 (`sample.notice`, 화면 경로 `/sample/notice`) — 견본이자 실제 화면(2026-11-13)

| 메서드 경로 | 요청 | 응답 data | 권한 | 문구 |
|---|---|---|---|---|
| `GET /api/notice` | `NoticeSearch` | `PageResult<Notice>` (`text`·앞뒤 글은 null) | QRY | 범위 값 틀림 400 `VAL_003` |
| `GET /api/notice/{no}` | `hit?='Y'` 이면 조회수+1 | `Notice`(앞뒤 글 포함) | QRY | 없음·볼 수 없음 404 `NTC_001`(조회수도 안 오름) |
| `POST /api/notice` | `NoticeSave` | `Notice` | REG | `S002` / 관리자가 아닌데 전체·다른 사무소 403 `NTC_003` |
| `PUT /api/notice/{no}` | `NoticeSave` | `Notice` | UPD | `S003` / 없음·볼 수 없음 `NTC_001` / 남의 글 403 `NTC_002` / `NTC_003` |
| `POST /api/notice/delete` | `{ nos: number[] }` (1~100건) | `{ count: number }` | DEL | `S005`({0}=count) / 빈 목록 400 `VAL_001` / 지운 건 0 이면 404 `NTC_001` / 남의 글이 하나라도 있으면 아무것도 안 지우고 403 `NTC_002` |

```ts
NoticeSearch extends PageParams { fromDate?: string; toDate?: string; ofcCode?: string; perNo?: string;
                                  field?: 'ALL' | 'SUBJECT' | 'TEXT' | 'NAME'; keyword?: string;
                                  scope?: 'COMMON' | 'OFC' }
Notice extends Audit { no: number; ofcCode: string | null /*null = 전체 공지*/; kanm: string | null; subject: string | null; text: string | null;
                       visit: number | null; name: string | null; perNo: string | null; wrtDate: string | null;
                       prevNo: number | null; prevSubject: string | null; nextNo: number | null; nextSubject: string | null /*상세에서만*/ }
NoticeSave { subject: string /*필수 ≤200바이트*/; text: string /*필수 ≤4000바이트*/; ofcCode?: string /*6자리, 비우면 전체 공지(관리자만)*/ }
```
- **공지 범위·권한 (2026-11-13 사용자 결정)** — 규칙 한 곳: 서버 `web/notice/service/NoticeRules.java`(`canSee`·`canModify`·`canWriteTo`), 프론트 `screens/sample/NoticeParts.tsx`(`canEditNotice`·`canDeleteNotice`, 버튼 흐림용).
  - `OFC_CODE` 가 빈 공지 = **전체 공지**(모든 사무소에 보임, 잠정 Q-24). 사무소 공지 = 그 사무소 사람 + 관리자에게만 보인다(목록·건수·상세·앞뒤 글 모두, 매퍼 `visible`).
  - 관리자 = `CurrentUser.isAdmin()`(권한 'A', 잠정 Q-09), 내 사무소 = `CurrentUser.ofcCode()`(권한 사무소).
  - 조회 `scope`: 비움 = 볼 수 있는 공지 모두(관리자 = 모든 공지, 그 밖 = 전체 공지 + 내 사무소), `COMMON` = 전체 공지만, `OFC` = 사무소 공지만. `ofcCode` 는 **관리자 + `scope=OFC`** 일 때만 쓰고 그 밖은 버린다. 서버가 쓰는 보이는 범위(`restrict`·`myOfc`)는 요청 값과 상관없이 로그인 사용자로 덮어쓴다.
  - 등록·수정: 관리자가 아니면 `ofcCode` = 내 사무소여야 한다(비움·다른 사무소 403 `NTC_003`). 수정·삭제: 관리자가 아니면 본인 글(`PER_NO` = 나)만(403 `NTC_002`) — 화면 권한 UPD·DEL 검사(`@RequireMenuAuth`)는 그 앞에서 따로.
  - 앞뒤 글: 이전 = 번호가 바로 작은 것, 다음 = 바로 큰 것(볼 수 있는 공지 안에서, 조회조건은 보지 않음).
- 화면: 새 글 N = 작성일이 오늘부터 3일 안(`NoticeParts.NEW_DAYS`, 새 컬럼 없음). 권한이 조회뿐이면 화면 제목 옆 '읽기 전용', 관리자가 아니면 '본인이 쓴 글만 수정·삭제'.
- 등록: `NO = NVL(MAX(NO),0)+1`(잠정 Q-02), `NAME`·`PER_NO` = 고정 사용자, `WRT_DATE = SYSDATE`, `VISIT = 0`.
- 기간 검색: `WRT_DATE >= TO_DATE(#{fromDate},'YYYYMMDD') AND WRT_DATE < TO_DATE(#{toDate},'YYYYMMDD') + 1`. 정렬 `NO DESC`.

## 4. 견본 — 기기정보 (`sample.mcnInfo`, `/sample/mcn-info`)

| 메서드 경로 | 요청 | 응답 data | 권한 | 문구 |
|---|---|---|---|---|
| `GET /api/mcn-info` | `McnInfoSearch` (`ofcCode` 필수 `V001_20`) | `PageResult<McnInfo>` | QRY | — |
| `GET /api/mcn-info/{ofcCode}/{mcnNo}` | | `McnInfo` | QRY | 없음 404 `MCN_001` |
| `POST /api/mcn-info` | `McnInfoSave` | `McnInfo` | REG | `S002` / 중복 400 `MCN_002` |
| `PUT /api/mcn-info/{ofcCode}/{mcnNo}` | `McnInfoSave`(키 무시) | `McnInfo` | UPD | `S003` / 없음 404 `MCN_001` |
| `DELETE /api/mcn-info/{ofcCode}/{mcnNo}` | | `null` | DEL | `S004` (USE_YN='N', 논리 삭제) / 없음 404 `MCN_001` |

```ts
McnInfoSearch extends PageParams { ofcCode: string; mcnNo?: string; vendCode?: string; komType?: string;
                                   opType?: string; inoutType?: string; useYn?: 'Y' | 'N' }
McnInfo extends Audit { ofcCode: string; kanm: string | null; mcnNo: string; instPstCode: string | null; instPstNm: string | null;
                        src: string | null; srcNm: string | null; komType: string | null; vendCode: string | null; vendNm: string | null;
                        opType: string | null; inoutType: string | null; useYn: 'Y' | 'N'; cnrPstSpecCode: string | null }
McnInfoSave { ofcCode: string; mcnNo: string /*숫자 3자리*/; instPstCode?: string; src?: string; komType?: string; vendCode?: string;
              opType?: string; inoutType?: string; useYn: 'Y' | 'N'; cnrPstSpecCode?: string }
```
- 정렬 `OFC_CODE, MCN_NO`. 키 비교는 `OFC_CODE = CAST(#{ofcCode} AS CHAR(6))`, `MCN_NO = CAST(#{mcnNo} AS CHAR(3))`.

## 5. 견본 — 기기모델코드 (`sample.mcnModel`, `/sample/mcn-model`)

| 메서드 경로 | 요청 | 응답 data | 권한 | 문구 |
|---|---|---|---|---|
| `GET /api/mcn-model` | `vendCode?`, `mcnType?`, `q?`(모델코드·모델명) | `McnModel[]` | QRY | — |
| `POST /api/mcn-model/batch` | `BatchChanges<McnModel>` | `{ added: number; updated: number; deleted: number }` | added→REG, updated→UPD, deleted→DEL (있는 종류만 검사) | `S001` / 중복 400 `MDL_002`({0}) / 없음 404 `MDL_001`({0}) |

```ts
McnModel extends Audit { vendCode: string; vendNm?: string | null; mcnType: string; modelCode: string; modelNm: string | null }
```
- 키 3개(`VEND_CODE+MCN_TYPE+MODEL_CODE`)는 수정 불가, 수정은 `MODEL_NM` 만. 처리 순서 삭제 → 수정 → 추가, 한 트랜잭션.

## 6. 응답코드 (이 저장소 TSV 에 추가하는 것)

| 코드 | 구분 | 문구 |
|---|---|---|
| `ME_001` | E | 고정 사용자 개인번호가 없거나 인사정보에 없습니다. 설정 atms.fixed-user.per-no 를 확인하세요. |
| `LKP_001` | E | 사무소 또는 검색어를 입력하세요. |
| `LKP_002` | E | 조회할 수 없는 항목입니다. ({0}) |
| `LKP_404` | E | 사무소를 찾을 수 없습니다. ({0}) |
| `MCN_001` | E | 기기정보를 찾을 수 없습니다. ({0}-{1}) |
| `MCN_002` | E | 이미 등록된 기기입니다. ({0}-{1}) |
| `MDL_001` | E | 모델코드를 찾을 수 없습니다. ({0}) |
| `MDL_002` | E | 이미 등록된 모델코드입니다. ({0}) |
| `ME_002` | E | 로그인이 필요합니다. 로그인 화면에서 다시 로그인하세요. |
| `LGN_001` | E | 개인번호 또는 비밀번호를 확인하세요. |
| `S012` · `S013` | S | 로그인되었습니다. · 로그아웃되었습니다. |
| `ATH_001` | E | 사용자 권한을 찾을 수 없습니다. ({0}) |
| `ATH_002` | E | 이미 등록된 사용자 권한입니다. ({0}) |
| `ATH_003` | E | 메뉴 권한을 찾을 수 없습니다. ({0}) |
| `ATH_004` | E | 부서를 찾을 수 없습니다. ({0}) |
| `ATH_005` | E | 부서의 사무소코드가 없습니다. ({0}) |
| `ATH_006` | E | 소속 부서를 찾을 수 없습니다. ({0}) |
| `MNU_004` | E | 메뉴를 찾을 수 없습니다. ({0}) |
| `MNU_005` | E | 이미 쓰는 대메뉴 접두어입니다. ({0}) |
| `MNU_006` | E | 메뉴 번호(01~99)를 다 써서 더 등록할 수 없습니다. ({0}) |
| `MNU_007` | E | 상위 메뉴로 고를 수 없는 메뉴입니다. ({0}) |
| `MNU_008` | E | 같은 메뉴경로가 이미 있습니다. ({0}) |
| `MNU_009` | E | 같은 메뉴ID 가 방금 등록되었습니다. 다시 저장하세요. ({0}) |
| `NTC_002` | E | 본인이 쓴 공지만 수정·삭제할 수 있습니다. (403) |
| `NTC_003` | E | 전체 공지와 다른 사무소 공지는 관리자만 등록할 수 있습니다. (403) |
| `DSH_001` | E | 조회할 수 없는 사무소입니다. ({0}) (403, 대시보드 범위 밖) |
| `JNL_001` | E | 2차 파기 대상 저널은 조회할 수 없습니다. {0} 이후 기간으로 조회하시고, 2차 파기 대상은 저널 2차 파기 조회 화면을 이용하세요. |
| `JNL_002` | E | 1차 파기 대상({0} 이전) 저널은 조회사유 등록과 책임자 승인이 필요합니다. 지금은 권한 A·L 만 조회할 수 있습니다. (잠정 Q-35) |
| `CDM_001` | E | 코드구분을 찾을 수 없습니다. ({0}) (404) |
| `CDM_002` | E | 상세코드가 있는 코드구분은 삭제할 수 없습니다. ({0}) |
| `CDM_003` | E | 이미 있는 코드구분입니다. ({0}) |
| `CDM_004` | E | 상세코드를 찾을 수 없습니다. ({0}) (404) |
| `CDM_005` | E | 이미 있는 코드입니다. ({0}) |
| `ULG_001` | E | 사용기록을 찾을 수 없습니다. (404, 없거나 다른 사무소 기록) |
| `CRYPTO_REQUIRED` · `CRYPTO_FORMAT` · `CRYPTO_KID` · `CRYPTO_DECRYPT` · `CRYPTO_EXPIRED` · `CRYPTO_REPLAY` | E | 암호화 오류 6종 (문구는 TSV, nh-atms-server 암호화 모듈과 같음) |

원본에서 그대로 쓰는 코드: `0000`, `S001~S005`, `SYS_401/403/404/405/500`, `VAL_001~004`, `V001_20`, `E404`, `NTC_001`, `MNU_001`(하위 메뉴가 있어 삭제 불가 — 메뉴관리).
단, `SYS_401` 은 코드만 원본이고 문구는 "사용자 확인이 필요합니다." 로 바꿨다(로그인 화면이 없어 "로그인" 말을 쓰지 않음, TSV 주석 참고).

## 7. 로그인 견본 · 민감필드 암호화 (`/api/web/auth/*`, 화면 `/login`) — 2026-11-09

- 목적: 비밀번호를 **node-forge 로 암호화해 보내는 로그인 화면** 견본. 원본: nh-atms-server NH-ATMS 암호화 모듈(2026-11-03).
- 기본은 지금처럼 고정 사용자다. 설정 `atms.login.enabled=true` 일 때만 로그인 세션이 화면 사용자가 된다.
- `/api/web/auth/**` 는 사용자 확인 전에 부른다 — SecurityConfig 허용, FixedUserFilter 제외, 권한 플래그 없음.

| 메서드 · 경로 | 요청 | 응답 data | 성공 문구 | 오류 |
|---|---|---|---|---|
| GET `/api/web/auth/public-key` | — | `PublicKeyInfo` | — | — |
| POST `/api/web/auth/login` | `LoginRequest` (JSON) | `LoginResult` | `S012` | 400 `LGN_001`(검증 실패) · `VAL_001`(빈 값) · `CRYPTO_*` |
| POST `/api/web/auth/logout` | — | `null` | `S013` | — |

```ts
interface PublicKeyInfo { enabled: boolean; serverTime: number; kid?: string; alg?: string; publicKey?: string }   // enabled=false 면 평문 전송
interface LoginRequest { perNo: string; password: string }   // password = ENC1 암호문 (서버 암호화가 꺼져 있으면 평문)
interface LoginResult { perNo: string; name: string; loginEnabled: boolean }   // 비밀번호는 응답에 없음
```

- 암호문 형식: `ENC1.<kid>.<b64 RSA-OAEP(SHA-256, MGF1 SHA-256)으로 감싼 AES 키>.<b64 IV 12바이트>.<b64 AES-256-GCM 암호문+태그>`
  - 속 평문: `{"v": 값, "ts": 서버 기준 epoch 초, "n": 1회용 난수}` → 서버가 시각(±`max-skew-seconds`, 기본 120초)과 재사용을 막는다.
  - 비유: 편지(값)를 일회용 자물쇠(AES 키)로 잠그고, 그 열쇠만 서버 공개키 봉투(RSA)에 넣어 보낸다. 봉투는 서버만 연다.
- 서버: 요청 DTO 필드에 `@EncryptedField` (`atms-common/security/crypto`) → 본문을 읽을 때 평문으로 바뀐다. 컨트롤러·서비스는 평문만 본다.
- 화면: `src/utils/fieldCrypto.ts` `encryptField(value)` — 공개키는 `loginApi.publicKey()` 로 한 번 받아 둔다. `CRYPTO_*` 오류면 `handleFieldCryptoError` 가 다시 받게 한다.
- 검증: `web/login/service/LoginVerifier` — 지금 구현 `InsaLoginVerifier` 는 개인번호가 `TB_INSA` 에 있고 비밀번호가 비어 있지 않으면 성공 (**잠정 Q-17**, 비밀번호 원천 없음).
- 성공하면 옛 세션을 버리고 새 HTTP 세션에 개인번호(`atms.login.perNo`)를 넣는다(세션 고정 방지).
- 설정 (`atms-web` yml)

| 키 | 기본 | local | jeus |
|---|---|---|---|
| `atms.login.enabled` | `false` | — | — |
| `crypto.field.enabled` | `false` | `true` | `true` |
| `crypto.field.kid` | `dev` | `local` | `k202611` |
| `crypto.field.generate-if-missing` | `false` | `true` (기동 때 임시 키, 재기동마다 바뀜) | — |
| `crypto.field.private-key-path` / `public-key-path` | — | — | `/home/atmswas/keys/field-k202611.key.pem` / `.pub.pem` (**없으면 기동 실패**) |
| `crypto.field.required` | `false` | `false` | `false` (화면 배포 확인 후 `true` → 평문 거부) |

- 서버 키 만들기(개발서버 ws01, atmswas — nh-atms-server `docs/POST_HANDOVER_EDITS.md` 0-2 와 같은 키):

```bash
mkdir -p /home/atmswas/keys
```

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out /home/atmswas/keys/field-k202611.key.pem
```

```bash
openssl pkey -in /home/atmswas/keys/field-k202611.key.pem -pubout -out /home/atmswas/keys/field-k202611.pub.pem
```

```bash
chgrp jeus /home/atmswas/keys /home/atmswas/keys/field-k202611.key.pem /home/atmswas/keys/field-k202611.pub.pem
```

```bash
chmod 2750 /home/atmswas/keys
```

```bash
chmod 640 /home/atmswas/keys/field-k202611.key.pem /home/atmswas/keys/field-k202611.pub.pem
```


## 8. 권한관리 (`sample.authMgt`, 화면 `/sample/auth-mgt`) — 2026-11-11

- AS-IS "시스템 사용권한부여" 화면 구성(조회조건 개인사번 / 권한목록: 순번·부서정보·사무소정보·권한·등록정보 / 등록·엑셀·조회) + 메뉴별 권한. 화면은 탭 2개.
- 권한은 이 화면 기준(`sample.authMgt`). 메뉴 권한 일괄 저장만 서비스에서 종류별로 검사한다(기기모델코드 견본과 같음).
- 쓰기가 끝나면 서버가 고정 사용자 캐시(`CurrentUserLoader`, 1분)를 비운다 — 내 권한을 바꾸면 바로 반영.

### 8-1. 부서권한 · 개인권한 (TB_SCR_AUTH, 키 `DEPT_CODE+OFC_CODE+PER_NO`)

- 2026-11-11: `PER_NO = '1'` 이면 **부서권한**, 그 밖은 **개인권한**. 화면은 AS-IS 처럼 탭 "부서권한"(권한목록 부서별)·"개인권한"(개인별)으로 나눠 `kind` 로 조회한다.

| 메서드 경로 | 요청 | 응답 data | 권한 | 문구 |
|---|---|---|---|---|
| `GET /api/web/auth-mgt/users` | `AuthUserSearch` | `PageResult<AuthUser>` | QRY | — / 사무소 형식 400 `VAL_003` |
| `GET /api/web/auth-mgt/users/{deptCode}/{ofcCode}/{perNo}` | | `AuthUser` | QRY | 없음 404 `ATH_001`({0}=키) |
| `POST /api/web/auth-mgt/users` | `AuthUserSave` | `AuthUser` | REG | `S002` / 중복 400 `ATH_002` / 부서권한: 부서 없음 400 `ATH_004` / 개인권한: 인사정보 없음 400 `E404`, 소속 부서 없음 400 `ATH_006` / 부서의 사무소코드 없음 400 `ATH_005` |
| `GET /api/web/auth-mgt/depts` | `q?`(부서코드·부서명) | `AuthDept[]` (최대 200, 부서코드 순) | QRY | — |
| `GET /api/web/auth-mgt/person-dept/{perNo}` | | `AuthDept` | QRY | 소속 부서 없음 400 `ATH_006` |
| `PUT /api/web/auth-mgt/users/{deptCode}/{ofcCode}/{perNo}` | `AuthUserSave`(키 무시) | `AuthUser` | UPD | `S003` / 없음 404 `ATH_001` |
| `POST /api/web/auth-mgt/users/delete` | `{ keys: AuthUserKey[] }` (1~100건) | `{ count: number }` | DEL | `S005`({0}=count) / 빈 목록 400 `VAL_001` / 지운 건 0 이면 404 `ATH_001` |

```ts
AuthUserSearch extends PageParams { kind?: 'DEPT' | 'PERSON' /*비우면 둘 다, 그 밖 400 VAL_003*/; ofcCode?: string; auth?: string; apvSts?: string;
                                    q?: string /*개인번호·성명·부서코드·부서명 부분일치*/ }
AuthDept { samuso: string /*TB_OFC_RAW.SAMUSO = DEPT_CODE*/; samusoName: string | null; onlineCode: string | null /*= OFC_CODE*/ }
AuthUser extends Audit { deptCode: string; deptNm: string | null; ofcCode: string; kanm: string | null; perNo: string; name: string | null;
                         auth: string; authNm: string | null; apvSts: string | null; memo: string | null; dueDate: string | null;
                         insDate: string | null; insId: string | null }
AuthUserKey { deptCode: string; ofcCode: string; perNo: string }
AuthUserSave { deptCode?: string /*부서권한 등록 때 필수*/; ofcCode?: string /*등록 때 무시*/; perNo?: string /*'1' = 부서권한, 그 밖 = 개인권한*/; auth: string /*1~2자*/; apvSts: string /*'Y'|'N'*/;
               dueDate?: string /*14자리, 비우면 무기한*/; memo?: string /*≤100바이트*/ }
```
- 키 정리: 부서·사무소는 숫자 1~6자리 → 6자리(형식 오류 400 `VAL_003`(필드명)), 비면 400 `VAL_001`(필드명). 비교는 `CAST(#{v} AS CHAR(6))`.
- 등록 키는 서버가 `TB_OFC_RAW` 로 채운다 — 부서권한: `DEPT_CODE` = 고른 부서, `OFC_CODE` = 그 부서의 `ONLINE_CODE`, `PER_NO` = '1' / 개인권한: `DEPT_CODE`·`OFC_CODE` = 그 사람(`TB_INSA.SAMUSO`)의 `TB_OFC_RAW` `SAMUSO`·`ONLINE_CODE`.
- 등록: `INS_DATE` = 지금(14자리), `INS_ID` = 고정 사용자 개인번호. 수정은 `AUTH`·`APV_STS`·`DUE_DATE`·`MEMO` 만. 삭제는 물리 삭제.
- 표시: 성명 `TB_INSA.NAME`, 사무소명 `TB_OFC_REG_MGT.KANM`, 부서명 `TB_OFC_RAW.SAMUSO_NAME`(`TB_OFC_RAW.SAMUSO = DEPT_CODE`), 권한명은 설정 `atms.auth-mgt.auth-code-type` 이 있을 때만 `TB_CODE_DTL.CODE_NM`(잠정 Q-20, 2026-11-16 예전 `TB_CO_CODE`). 승인상태 값 'Y'/'N'(잠정 Q-18).
- 정렬 `OFC_CODE, PER_NO, DEPT_CODE`.

### 8-2. 권한코드 · 메뉴 권한 (TB_SCR_AUTH_MENU, 키 `AUTH+MENU_ID`)

- 화면(2026-11-11 사용자 결정): 상단 조회조건의 **권한 콤보**(필수, 권한코드 목록) → [조회] → 아래 표에 **메뉴 전체**. '사용' 체크 = 그 권한이 그 메뉴를 가짐. 행추가·행삭제 없음.

| 메서드 경로 | 요청 | 응답 data | 권한 | 문구 |
|---|---|---|---|---|
| `GET /api/web/auth-mgt/auth-codes` | | `AuthCode[]` (`AUTH` 순) | QRY | — |
| `GET /api/web/auth-mgt/menus` | `auth` (1~2자, 필수) | `AuthMenu[]` (트리 순서) | QRY | 400 `VAL_001`(auth) |
| `POST /api/web/auth-mgt/menus/batch` | `BatchChanges<AuthMenu>` (`added`·`deleted` 는 빈 배열 — 아니면 400 `VAL_001`) | `{ inserted: number; updated: number }` | updated: 행 있음→UPD, 없음→REG (있는 종류만) | `S009` / 메뉴 없음 404 `ATH_003`({0}='권한-메뉴ID') / 플래그 400 `VAL_001`(필드명) |

```ts
AuthCode { auth: string; authNm: string | null; userCount: number | null; menuCount: number | null }
AuthMenu extends Audit { auth: string; menuId: string; parentMenuId: string | null; menuNm: string | null; menuType: string | null;
                         menuPath: string | null; sortNo: number | null; menuUseYn: 'Y' | 'N' | null; lvl: number | null;
                         mappedYn: 'Y' | 'N';   // TB_SCR_AUTH_MENU 에 행이 있나(화면에는 안 보임 — REG/UPD 구분용)
                         qryRolYn: 'Y' | 'N'; regRolYn: 'Y' | 'N'; updRolYn: 'Y' | 'N'; delRolYn: 'Y' | 'N'; exeRolYn: 'Y' | 'N'; useYn: 'Y' | 'N' }
```
- 권한코드 목록 = `TB_SCR_AUTH` ∪ `TB_SCR_AUTH_MENU` 에 있는 `AUTH` (+ 설정 코드구분의 2자 이내 코드). 새 권한코드는 부서권한·개인권한 탭에서 먼저 부여하면 나타난다.
- 메뉴 목록 = `TB_SCR_MENU` 전체 ⟕ 이 권한의 행. 행이 없는 메뉴는 `mappedYn='N'`, 플래그·`useYn` 모두 'N'(화면에 전부 미체크).
- 트리 순서·깊이는 서버가 만든다(`AuthMgtService.flattenTree`: 부모 다음 자식, 형제는 `SORT_NO, MENU_ID`, 부모가 목록에 없으면 최상위).
- 저장 = 바꾼 행만, PK(`AUTH`+`MENU_ID`)로 있으면 UPDATE, 없으면 INSERT, 한 트랜잭션. **행 삭제 없음** — 사용 해제는 `USE_YN='N'` 으로 UPDATE(행은 남음). 사용을 안 켜고 다른 권한만 켜도 그대로 `USE_YN='N'` 으로 저장(사용자 결정). 읽기 전용 칸(메뉴명 등)은 서버가 무시한다.

## 9. 메뉴관리 (`sample.menuMgt`, 화면 `/sample/menu-mgt`) — 2026-11-12

- 목록 + 팝업 등록·수정(공지사항 견본 모양). 팝업의 상위 고르기 = **대 → 중 연쇄 콤보**(2026-11-12 사용자 결정).
- 메뉴 3단계: **대메뉴**(상단) → **중메뉴**(좌측) → **소메뉴**(좌측 아래, 실제 화면).
- **메뉴ID = 영문 대문자 3 + 중메뉴 번호 2 + 소메뉴 번호 2** (7자, 2026-11-12 사용자 결정, `MenuMgtRules`)

| 예 | 레벨 | MENU_TYPE | PARENT_MENU_ID | MENU_PATH |
|---|---|---|---|---|
| `SYS0000` | 1 대메뉴 | G | `'0'` | 없음 |
| `SYS0300` | 2 중메뉴 | G | `SYS0000` | 없음 |
| `SYS0312` | 3 소메뉴 | M | `SYS0300` | 필수 `/{slug}/{kebab}` |

- 번호는 **같은 상위 안 MAX + 1**(지운 번호는 다시 쓰지 않음, 01~99). 보이는 순서는 `SORT_NO` 로 따로.
- 레벨은 새 컬럼 없이 **메뉴ID 모양**으로 판단한다. 모양이 다른 예전 ID 는 `menuLevel` null("규칙 밖") — 수정·삭제는 되지만 상위로 고를 수 없다.
- 상위는 메뉴ID 에 들어 있어 **수정에서 못 바꾼다**(PK). 옮기려면 새로 등록하고 예전 메뉴는 미사용.
- 개발 DB 의 예전 견본 메뉴(SMP000·SMP001~)는 `db/change/menu_three_level.sql` 로 SMP0000·SMP0100('견본 화면')·SMP0101~ 로 옮긴다.

| 메서드 경로 | 요청 | 응답 data | 권한 | 문구 |
|---|---|---|---|---|
| `GET /api/web/menu-mgt/menus` | `MenuMgtSearch` | `MenuMgtRow[]` (트리 순서 전체, 페이징 없음) | QRY | — |
| `POST /api/web/menu-mgt/menus` | `MenuMgtSave` | `MenuMgtRow` | REG | `S002` / 입력 400 `VAL_001`(필드명) / 상위 없음 400 `MNU_004` / 접두어 겹침 400 `MNU_005` / 번호 다 씀 400 `MNU_006` / 상위 모양 400 `MNU_007` / 경로 겹침 400 `MNU_008` / 동시 등록 400 `MNU_009` |
| `PUT /api/web/menu-mgt/menus/{menuId}` | `MenuMgtSave`(menuNm·menuPath·sortNo·useYn 만 씀) | `MenuMgtRow` | UPD | `S003` / 없음 404 `MNU_004` / 경로 겹침 400 `MNU_008` |
| `POST /api/web/menu-mgt/menus/delete` | `{ menuIds: string[] }` (1~100건) | `{ count: number; authCount: number }` | DEL | `S005`({0}=count) / 하위가 남음 400 `MNU_001` / 지운 건 0 이면 404 `MNU_004` |

```ts
MenuMgtSearch { topMenuId?: string /*그 대메뉴와 아래*/; menuLevel?: 1 | 2 | 3; useYn?: 'Y' | 'N'; q?: string /*메뉴ID·메뉴명·경로 부분일치*/ }
MenuMgtRow extends Audit { menuId: string; parentMenuId: string; menuNm: string; menuType: 'G' | 'M'; menuPath: string | null; sortNo: number; useYn: 'Y' | 'N';
                           menuLevel: 1 | 2 | 3 | null /*메뉴ID 모양*/; childCount?: number | null; parentPath?: string | null /*'시스템관리 › 공통코드'*/ }
MenuMgtSave { menuLevel?: 1 | 2 | 3 /*등록 필수*/; parentMenuId?: string /*중 = 대메뉴ID, 소 = 중메뉴ID*/; prefix?: string /*대메뉴 등록: 영문 대문자 3자*/;
              menuNm: string /*≤100바이트*/; menuPath?: string /*소메뉴만, '/{slug}/{kebab}' ≤300자*/; sortNo: number /*0~99999*/; useYn: 'Y' | 'N' }
```
- 목록은 메뉴 전체를 트리 순서(부모 다음 자식, 형제는 `SORT_NO, MENU_ID`)로 펴고 레벨·하위 수·상위 경로를 붙인 뒤 조건으로 거른다(`MenuMgtService.search`). 등록 팝업의 대·중메뉴 콤보도 조건 없는 이 목록을 쓴다.
- 대메뉴 등록: `MENU_ID` = 접두어 + `0000`, 접두어로 시작하는 메뉴ID 가 하나라도 있으면 400 `MNU_005`.
- 중·소메뉴 등록: 상위가 `TB_SCR_MENU` 에 있어야 하고(없으면 `MNU_004`), 모양이 맞아야 한다(중의 상위 `SSS0000`, 소의 상위 `SSS0100`, 아니면 `MNU_007`). 99 를 넘으면 `MNU_006`. 넣는 순간 같은 ID 가 생기면(동시 등록) `MNU_009`.
- 메뉴경로: 소메뉴(화면 M)만, `^/[a-z][a-z0-9]*/[a-z0-9]+(-[a-z0-9]+)*$`, 다른 메뉴와 겹치면 `MNU_008`. 대·중메뉴는 보내도 NULL 로 저장.
- 삭제: 물리 삭제. 고른 목록 밖에 하위가 남으면 400 `MNU_001`. 그 메뉴들의 `TB_SCR_AUTH_MENU` 행을 먼저 지우고 메뉴를 지운다(한 트랜잭션, 2026-11-12 사용자 결정).
- 화면은 쓰기가 끝나면 목록과 왼쪽 메뉴(`GET /api/web/menu/my`)를 다시 읽는다. 프론트에 메뉴 표가 없어(2026-11-12) 새 대메뉴도 상단에 바로 나타난다.

## 10. 대시보드 (`main.dashboard`, 화면 `/main/dashboard` = 홈 탭) — 2026-11-16

| 메서드 경로 | 요청 | 응답 data | 권한 | 문구 |
|---|---|---|---|---|
| `GET /api/web/dashboard/scope` | — | `DashScope` | QRY | — |
| `GET /api/web/dashboard/offices` | `region` | `DashOffice[]` | QRY | 범위 밖 본부 403 `DSH_001` |
| `GET /api/web/dashboard/summary` | `region?`, `ofcCode?`(둘 다 없으면 전국) | `DashSummary` | QRY | 범위 밖 사무소 403 `DSH_001` |
| `GET /api/web/dashboard/notices` | — | `Notice[]`(최신 6건, `text` null) | QRY | — |
| `GET /api/web/dashboard/notices/{no}` | — | `Notice`(조회수 +1, 앞뒤 글) | QRY | 없음·볼 수 없음 404 `NTC_001` |

```ts
DashLevel = 'CENTRAL' | 'REGION' | 'BRANCH'; DashStatus = 'OK' | 'ERR' | 'FIX' | 'OFF'
DashOffice { ofcCode: string; kanm: string | null; regionCode: string | null /*JUNG_MOJUM*/ }
DashScope { level: DashLevel; myOffice: DashOffice | null; regions: DashOffice[]; defaultRegion: string | null; defaultOfcCode: string | null;
            regionEditable: boolean; officeEditable: boolean }
DashMachine { mgrOfcCode: string; mgrOfcNm: string | null; parentOfcCode: string; parentOfcNm: string | null; instPstNm: string | null; mcnNo: string;
              srcNm: string | null; vendNm: string | null; modelNm: string | null; status: DashStatus; statusDetail: string | null; address: string | null; mgmtVendCode: string | null }
DashSummary { title: string; titleKind: 'BRANCH' | 'REGION' | 'ALL'; titleCode: string | null; baseDtm: string /*14자리*/; total: number;
              dealDate: string /*전 영업일 YYYYMMDD*/; dealAvg: number; statusCount: Record<DashStatus, number>; machines: DashMachine[] }
```
- **권한**: 대시보드를 메뉴로 등록(2026-11-16 사용자 결정) — 메뉴 행 `db/change/dashboard_menu.sql`(대메뉴 `DSB0000` → `DSB0100` → `DSB0101` `/main/dashboard`, 권한 A·T·L 조회). 공지 2개 API 도 이 화면 권한(공지사항 메뉴 권한이 없어도 대시보드에서는 보임, 보이는 범위 규칙은 §3 과 같음 — 서버가 `NoticeService` 를 그대로 부름).
- **레벨**(규칙 한 곳 `web/dashboard/service/DashboardRules.java`, 잠정 Q-25): 로그인 권한코드(개인권한, 없으면 부서권한) A = 중앙본부, T = 영업본부, L·그 밖 = 영업점. 대응표는 설정 `atms.dashboard.level.central`(기본 `A`)·`atms.dashboard.level.region`(기본 `T`).
- **내 사무소** = 인사 소속(`TB_INSA.SAMUSO` → `TB_OFC_RAW.ONLINE_CODE`). 영업본부 사용자의 본부 = 내 사무소, 본부 소속 영업점 = `TB_OFC_REG_MGT.JUNG_MOJUM` 이 그 본부인 사무소. 지역 구분 목록(중앙본부) = 다른 사무소의 `JUNG_MOJUM` 으로 쓰인 사무소.
- **범위 검사**: 중앙본부 = 모두, 영업본부 = 지역은 내 본부로 고정(요청 값 무시)·사무소는 소속만, 영업점 = 내 사무소로 고정(다른 사무소면 403 `DSH_001`).
- **기기** = 사용 중 기기(`TB_MCN_INFO.USE_YN='Y'`) 중 그 코너(`TB_SCRT_INST_PST.USE_YN='Y'`)도 사용 중인 것. 범위 = 관리점(`M.OFC_CODE`) 또는 관리모점(`NVL(코너 BLT_OLD_OFC_CODE, 관리점)`)이 고른 사무소(본부면 소속 사무소 + 본부 자신).
- **⑦ 운영 대수** = 기기 수, `baseDtm` = 조회 시각. **③ 일평균 거래량** = 전 영업일(토·일·`TB_NMS_HDAY` 휴일 제외) `TB_OP_DEAL_OFCSUM` 현금지급+수표지급+현금입금+수표입금 건수 합 ÷ 운영 대수(반올림, 0대면 0).
- **⑤ 기기상태**(AS-IS 6000 을 4개로, 잠정 Q-26): `TB_OP_ATM_STAT` — 통신단절 = `POWER_OFF='Y'` → 보수중 = `USE_STOP='Y'` → 장애 = `ERR_MODE='Y'` → 정상(상태 행이 없어도 정상). 장애면 `statusDetail` = 최근 장애명(`TB_EE_MCN_ERR_HIST.ERR_NM`).
- **표 컬럼**(잠정 Q-27): 모델명 = `KOM_TYPE`(기기에 모델 컬럼 없음), 제조사 = 기기 `VEND_CODE` 이름(설정 코드구분, Q6), 주소 = 관리모점(없으면 관리점) 사무소 `ADDRESS`, 관리업체 = 코너 `VEND_CODE`(일괄용역이면 `IAL_SEV_VEND_CODE`) 코드.
- `TB_OP_ATM_STAT`·`TB_OP_DEAL_OFCSUM`·`TB_EE_MCN_ERR_HIST` 는 사진 판독본 행으로 참고 DDL 을 만들었다(`db/ddl`, 판독메모) — 폐쇄망 개발 DB 확인 필요(`db/check`).

## 11. 코드관리 (`sample.codeMgt`, 화면 `/sample/code-mgt`) — 2026-11-16

> 왼쪽 코드구분(TB_CODE_MASTER) · 오른쪽 상세코드(TB_CODE_DTL), 등록·수정·상세 = 팝업. 코드구분은 마스터·상세 모두 `VARCHAR2(10)`(판독 2자리 정정, 사용자 확인). 상세의 `CODE_SECT` 로 마스터를 찾는다(물리 FK 없음). 권한 = 메뉴 권한 플래그, 엑셀 없음.

| 메서드 경로 | 요청 | 응답 data | 권한 | 문구 |
|---|---|---|---|---|
| `GET /api/web/code-mgt/masters` | `q?`(코드구분·코드명 부분일치), `usedYn?` | `CodeMaster[]`(코드구분 순, 페이징 없음) | QRY | — |
| `GET /api/web/code-mgt/masters/{codeSect}` | — | `CodeMaster` | QRY | 없음 404 `CDM_001` |
| `POST /api/web/code-mgt/masters` | `CodeMasterSave` | `CodeMaster` | REG | `S002`, 겹침 400 `CDM_003` |
| `PUT /api/web/code-mgt/masters/{codeSect}` | `CodeMasterSave`(codeSect 무시) | `CodeMaster` | UPD | `S003` |
| `POST /api/web/code-mgt/masters/delete` | `{ keys: string[] }` 1~100 | `{ count }` | DEL | `S005`, 상세가 남으면 400 `CDM_002`(그 코드구분들) |
| `GET /api/web/code-mgt/masters/{codeSect}/codes` | — | `CodeDetail[]`(코드 순) | QRY | 코드구분 없음 404 `CDM_001` |
| `GET /api/web/code-mgt/masters/{codeSect}/codes/{code}` | — | `CodeDetail` | QRY | 없음 404 `CDM_004` |
| `POST /api/web/code-mgt/masters/{codeSect}/codes` | `CodeDetailSave` | `CodeDetail` | REG | `S002`, 겹침 400 `CDM_005` |
| `PUT /api/web/code-mgt/masters/{codeSect}/codes/{code}` | `CodeDetailSave`(code 무시) | `CodeDetail` | UPD | `S003` |
| `POST /api/web/code-mgt/masters/{codeSect}/codes/delete` | `{ keys: string[] }` 1~100 | `{ count }` | DEL | `S005` |

```ts
interface CodeMaster extends Audit {
  codeSect: string; codeSectNm: string;          // 화면 '코드명' = CODE_SECT_NM
  desc1..desc5: string | null;                   // 상세 표 칸 제목과 연결하지 않음(데이터1~5 고정, 2026-11-16)
  taskSect: string | null; prcSect: string | null; // 값 목록 미정 — 직접 입력(잠정 Q-28)
  usedYn: 'Y' | 'N'; detailCount: number;
}
interface CodeDetail extends Audit {
  codeSect: string; code: string; codeNm: string;
  data1, data2, data3: string | null;
  data4: number | null;                          // NUMBER(4) -9999~9999
  data5: string | null;                          // CHAR(1)   — 표에 데이터1~5 모두(2026-11-16)
}
interface CodeMasterSave { codeSect?; codeSectNm; desc1~5?; taskSect?; prcSect?; usedYn }
interface CodeDetailSave { code?; codeNm; data1~3?; data4?; data5? }
```

- 검사(서버 `web/codemgt/service/CodeMgtRules.java` · 프론트 `screens/sample/CodeMgtColumns.tsx` 길이 상수): 코드구분·코드 = 공백 없이 1~10자(대소문자 그대로), 코드구분명 100바이트, 설명 100바이트, 업무구분 10바이트, 처리구분 1자, 코드명 80바이트, 데이터1~3 80바이트, 데이터4 -9999~9999, 데이터5 1자, 사용여부 Y/N. 어기면 400 `VAL_001`(필드명).


## 12. 사용기록조회 (`sample.useLog`, 화면 `/sample/use-log`) — 2026-11-17

> 화면에서 한 조회·등록·수정·삭제·엑셀을 **서버가 자동으로** `TB_SCR_CUD_HIST`(사용자기록이력)에 남기고, 이 화면에서 읽는다. 기록 규칙은 공통 `web/common/uselog/`(아래 12-1), 화면 API 는 `web/uselog/`.

### 12-1. 기록 규칙 (사용자 결정 2026-11-17 — 화면 코드에서 할 일 없음)

- **대상 = 컨트롤러 메서드 이름 접두어**(이름이 곧 정책): `select*` → `R` 조회, `insert*` → `C` 등록, `update*` → `U` 수정, `delete*` → `D` 삭제. 엑셀 = `E`(아래 엑셀 기록 API). 정본 주석은 `C·U·D` 뿐 — `R·E` 는 잠정(Q-30).
  - 접두어가 아닌 컨트롤러 API 메서드는 빌드가 막는다(`CrudLogNamingGuardTest`) — 이름을 바꾸거나 `@ExcludeCrudLog` 를 붙인다.
  - 기록하지 않는 것(`@ExcludeCrudLog`): 공통 조회 `/lookup/*`, 내 정보 `/me`, 내 메뉴 `/menu/my`, 로그인·공개키 `/auth/*`, 대시보드 `/dashboard/*`. 사용자가 없는 요청도 기록하지 않는다. 입력 검사(@Valid) 실패는 컨트롤러 전이라 기록되지 않는다.
- **한 행**: 이력번호(시각 14 + 서버번호 1 + 초당 일련 4 = 19자리, 설정 `atms.uselog.node`, 잠정 Q-31) · 부서·사무소 = 로그인 사용자의 권한 부서·사무소 · 개인번호 · 메뉴ID(메서드의 `@RequireMenuAuth` 화면키 우선, 없으면 `X-Screen-Key` → 메뉴경로 → `TB_SCR_MENU`) · 수행작업 · 성공여부(예외면 `N`) · URI · 파라미터 · 실행 SQL.
- **파라미터(PARAMS, 500바이트)** = 머리 `ip=<IP> auth=<권한코드> [err=<오류코드>]` + 공백 + 인자 JSON `{"query":{…},"body":…}`. IP·권한은 컬럼이 없어 여기에 적는다. 비밀번호·토큰 류 키의 값만 `***`, 나머지 원문. 500바이트(한글 2) 넘으면 표시 없이 자른다(Q-32).
- **실행 SQL(SQL_TEXT, CLOB)** = 컨트롤러가 실행되는 동안의 모든 SQL(조회·등록·수정·삭제)을 실제 값을 끼운 글로, 자르지 않음. 앞에 `/* 매퍼.문장id */`. 권한 검사·현재 사용자 같은 공통 매퍼(`web.common.*`)는 뺀다.
- 기록은 업무와 다른 트랜잭션(업무가 롤백돼도 남음). 기록이 실패해도 업무 응답은 그대로(서버 로그 `[사용기록] 저장 실패`). 원래 예외는 감싸지 않고 그대로.
- 보관기간·자동 삭제 없음(현업 질문 Q-33).

### 12-2. API

| 메서드 경로 | 요청 | 응답 data | 권한 | 문구 |
|---|---|---|---|---|
| `GET /api/web/use-log` | `UseLogSearch` | `PageResult<UseLog>`(최근 순, `params`·`sqlText` 는 null) | QRY | 기간 없음 400 `VAL_002`, 형식 `VAL_003`, 순서·31일 넘음 `VAL_001` |
| `GET /api/web/use-log/{histId}` | — | `UseLog`(`params`·`sqlText` 포함) | QRY | 없거나 다른 사무소 404 `ULG_001` |
| `POST /api/web/use-log/excel` | `UseLogExcelRequest` | `null` | 로그인 사용자 누구나(메뉴 권한 없음) | — (문구 없음, 실패해도 정상 응답) |

```ts
type CudType = 'R' | 'C' | 'U' | 'D' | 'E';
interface UseLogSearch extends PageParams {
  fromDate: string; toDate: string;   // YYYYMMDD 필수, 최대 31일(양끝 포함)
  cudType?: CudType; succYn?: 'Y' | 'N';
  ofcCode?: string;                   // 관리자만 — 그 밖은 서버가 내 권한 사무소로 덮어씀
  perNo?: string; menuId?: string;
  ip?: string;                        // 완전일치(숫자·점·콜론만)
}
interface UseLog {
  histId: string;                     // 19자리 → 글자(JSON 숫자 정밀도)
  crtDtm: string | null;              // 일시 YYYYMMDDHH24MISS
  deptCode, ofcCode, kanm, perNo, name, menuId, menuNm: string | null;   // 이름은 LEFT JOIN(TB_OFC_REG_MGT·TB_INSA·TB_SCR_MENU)
  cudType: CudType | null; succYn: 'Y' | 'N' | null; uri: string | null;
  ip, auth, err: string | null;       // PARAMS 머리에서
  params, sqlText: string | null;     // 상세에서만
}
interface UseLogExcelRequest { fileName: string; rowCount: number }
```

- 보이는 범위(서버 `web/uselog/service/UseLogRules.java`): 관리자 = 모든 사무소(사무소 칸으로 거름), 그 밖 = 내 권한 사무소 기록만(권한 사무소가 없으면 0건). 상세도 같은 규칙(다른 사무소면 404).
- 엑셀 기록: 프론트 공통 `utils/excel.ts downloadExcel` 이 내려받기 직전에 부른다(사유 없음, 2026-11-17 사용자 결정). 화면은 `X-Screen-Key` 헤더로 안다. 실패해도 내려받기는 계속.

## 13. 저널조회 (`jnal.jnalList`, 화면 `/jnal/jnal-list`) — 2026-11-24

> 자동화기기 1대의 전자저널(`TB_JL_JNAL`, 기준일 전은 `TB_JL_JNAL_ILM`)을 조회한다. 조회만 — 메뉴 권한 QRY, 사용기록 R 자동. 규칙 한 곳 = 서버 `web/jnal/service/JnalRules.java`(As-Is 저널조회 화면·인클루드 근거).

| 메서드 경로 | 요청 | 응답 data | 권한 | 문구 |
|---|---|---|---|---|
| `GET /api/web/jnal` | `JnalSearch` | `Jnal[]`(페이지 없음, 최근 순, 같은 명세표는 합친 한 행) | QRY | 필수 없음 `VAL_002`, 형식·조회조건 `VAL_003`, 순서·2일 넘음 `VAL_001`, 5년 `JNL_001`, 3개월 조회 허가 `JNL_002` |
| `GET /api/web/jnal/checks` | `JnalCheckKey` | `JnalCheck[]`(일련번호 순) | QRY | 목록과 같은 사무소·기간 규칙 |

```ts
interface JnalSearch {
  ofcCode: string;                    // 필수. 권한 A·T·L(관리자 포함)만 고름, 그 밖은 서버가 내 사무소로 덮어씀
  mcnNo: string;                      // 필수(3자리로 채움)
  fromDate: string; toDate: string;   // 저널일자 YYYYMMDD 필수, 최대 2일(양끝 포함)
  fromTime?: string; toTime?: string; // HHmm(비면 0000~2359) — 서버가 시작 00초·끝 59초를 붙임
  jnalType?: '22' | '33' | '11' | '44' | '55';          // 일반·수표·개국·합계·운영(비면 전체)
  jrnlProcRest?: '01' | '02' | '97' | '98' | '99';      // 정상·불능·취소정상·취소불능·확인요망
  field?: 'ACCT' | 'CHK' | 'DTLS';    // 계좌번호·수표번호·명세표일련번호 — 저널구분에 없는 값은 VAL_003(전체·33 = 셋, 22 = 계좌·명세표, 11·44·55 = 명세표)
  keyword?: string;                   // 계좌·수표 = 부분일치(영문·숫자·-), 명세표 = '0001-0050' · '-0050' · '0001-' · 번호 하나(그 번호만)
}
interface Jnal {
  jnalDt: string; jnalTime: string | null; jnalSeqno: string; ofcCode: string; mcnNo: string;
  jnalType: string | null;            // 00·22·33 거래, 11 개국, 44 합계, 55 운영
  jrnlProcRest: string | null;        // 01 정상·02 불능·97 취소정상·98 취소불능·99 확인요망·77 고객조작취소
  dealDate: string | null; scrNo: string | null;
  scrNm: string | null;               // 55 = 기기운영관리, 9927·9937·9947·9957 = 타행이체, 9929·9939·9949·9959 = 타행이체조회, 그 밖 TB_CODE_DTL(CODE_SECT '01') 코드명
  dtlsSeqno: string | null; acctNo: string | null; estbBankCode: string | null;
  dealAmt: number | null;             // 합친 행 = 가장 큰 금액
  mdnType: string | null;
  chkCnt: number | null; chkNo: string | null; chkNos: string | null;   // TB_JL_JNAL_CHK 묶음(14자리씩 ',' 로 이음)
  bprProcStat: string | null; faceImgYn: string | null; acptImgYn: string | null; cardImgYn: string | null;
  bfoComt: string | null; aftComt: string | null;                        // 이 행 자신의 전인자·후인자
  rolComts: string[];                 // 롤방식 글 — '전인자\n후인자' 를 합친 저널마다 하나
  dtlComts: string[];                 // 후인자만 — 합친 저널마다 하나
  ilm: boolean;                       // ILM 테이블에서 읽은 행
}
interface JnalCheckKey { jnalDt: string; ofcCode: string; mcnNo: string; jnalSeqno: string }
interface JnalCheck { seqno: string | null; chkNo: string | null }
```

- 합치기(As-Is `rconcat`): SQL 순서(저널일자·시간·일련번호 내림차순)대로 돌며 저널구분이 55 가 아니고 같은 날·같은 명세표(22·33 은 00 으로 봄) 안 순번이 1 이 아니면 바로 앞에 남긴 행에 합친다. 운영 묶음·ILM 묶음은 따로 합친 뒤 운영 → ILM 순.
- 기준일: ILM = 오늘 − 3개월 + 1일 전(잠정 Q-34). 2차 파기 = 시작일 ≤ 오늘 − 5년. 조회 허가 = 권한 A·L 이 아니면 시작일 ≤ 오늘 − 3개월 막음(As-Is 는 사유 + 책임자 승인 + 기록, 잠정 Q-35).
- IT부문(내 사무소 `000080`)이고 관리자가 아니면 `field`·`keyword` 를 무시한다(As-Is 2020.09 개인정보 조회 제한). 계좌번호 등은 As-Is 대로 원문(사용기록에도 원문, 2026-11-24 사용자 결정).

