> 저널조회 화면(`jnal.jnalList`)을 빈 상태에서 처음부터 만든다고 보고 무엇을 어떤 순서로 만드는지 정리(2026-11-24).
> - 화면 설명은 탭·팝업 단위(§4), 프론트를 자세히. 가져다 쓴 공통 모듈은 §3.
> - 부록 A 는 §2 순서대로 새 파일 = 전체 소스, 기존 파일 = diff(이 화면 관련 줄만). 부록 B = SHA-512.
> - 경로 줄임: src/ = Frontend/nh-atms-web/src/, web/ = Backend/nh-atms-api/atms-web/src/main/java/kr/co/nh/atms/web/, mapper/ = Backend/nh-atms-api/atms-web/src/main/resources/mapper/

## 0. 한눈에 보기

- 자동화기기 1대의 **전자저널(영수증 인자 내용)** 을 날짜·시간·저널구분·거래결과·검색어로 찾아 목록과 롤방식(글 이어 보기)으로 보는 **조회 전용** 화면. 비유: 기기 안 영수증 두루마리를 날짜로 펼쳐 보는 돋보기.
- 근거: As-Is 저널조회 화면 스크립트·SQL·인클루드(사진·드라이브), As-Is 캡처, 피그마 저널조회 프레임(배치). **내용 = As-Is, 배치 = 피그마, 생김새 = 이 저장소 공통 부품.**

| 탭·팝업 | 테이블 | 모양(복사 견본) | 하는 일 |
|---|---|---|---|
| 화면 — 목록 보기 | `TB_JL_JNAL`(+`_CHK`), 기준일 전 `_ILM` | 사용기록조회(조회 전용) | 조건 → 서버가 합친 전체 목록 → 20건씩 |
| 화면 — 롤방식 보기 | 같은 응답 | (새) 글 상자 | 행마다 인자 글을 '====' 줄로 이어 봄 |
| 팝업 `popup.jnalView` 명세표 | 목록 행 + `TB_JL_JNAL_CHK` | 사용기록 상세 팝업 + 탭 | 명세서 · 수표목록 · 저널 |

- 업무 규칙(서버 `web/jnal/service/JnalRules.java` 한 곳)
  1. 사무소·기번·저널일자 필수, 저널일자 **최대 2일**.
  2. 시작일 ≤ 오늘 − 5년 → `JNL_001`(2차 파기, 저널 2차 파기 조회 화면으로).
  3. 권한이 A·L 이 아니고 시작일 ≤ 오늘 − 3개월 → `JNL_002`(As-Is 는 사유 + 책임자 승인, 지금은 **막기만** — 잠정 Q-35).
  4. 권한 A·T·L(관리자 포함) = 전체 사무소에서 고름, 그 밖 = 내 사무소(서버도 덮어씀).
  5. IT부문(내 사무소 000080) 비관리자 = 조회조건·검색어 없음(As-Is 2020.09 개인정보 조회 제한).
  6. 기준일(오늘 − 3개월 + 1일) 전 저널 = ILM 테이블(잠정 Q-34). 기간이 걸치면 두 번 읽고 운영 → ILM 순.
  7. 같은 날·같은 명세표의 저널(22·33 은 00 으로 봄)은 **앞 행에 합침**(As-Is rconcat) — 인자 글 목록, 금액은 큰 값.
  8. 개인정보(계좌번호·인자 글)는 As-Is 대로 원문(사용기록에도 원문).

```text
[화면] 조회조건 ─ GET /api/web/jnal ─▶ JnalController(QRY) ─▶ JnalService
                                            JnalRules.normalize(필수·2일·5년·조회허가·사무소·IT부문)
                                            JnalRules.split → 운영 묶음 / ILM 묶음
                                              └▶ JnalMapper.selectList ×(1~2) ─▶ [TB_JL_JNAL(_ILM) + TB_JL_JNAL_CHK(_ILM) + TB_CODE_DTL '01']
                                            거래명(JnalRules.scrNm) → 합치기(JnalRules.merge)
[화면] ◀── Jnal[] (페이지 없음) ── 목록 20건씩 / 롤방식 / [상세] → 명세표 팝업 ─ GET /api/web/jnal/checks ─▶ 수표목록
```

## 1. 준비 — DB

- 테이블 4개는 **정본 CSV 행**으로 만든 참고 DDL(`db/ddl/TB_JL_JNAL*.sql`). 개발 DB 에 있는지 `db/check/01_exists.sql`·`02_columns.sql` 로 먼저 본다.
- **행이 있어야 화면에 나온다**: 업체 수집 모듈(atms-journal)은 지금 `T_JOURNAL` 에 쓴다 → `TB_JL_JNAL` 에 행이 없으면 늘 0건(Q-39). 거래명은 `TB_CODE_DTL` 코드구분 `'01'` 행이 있어야 보인다.
- 메뉴: **메뉴관리 화면**에서 저널 대·중메뉴 아래에 소메뉴를 만들고 메뉴경로 `/jnal/jnal-list` 를 넣는다(SQL 파일 없음, 2026-11-24 사용자 결정). 권한은 권한관리 → 메뉴 권한 탭에서 '조회'.

## 2. 만들 파일 — 빨간줄 없이 가는 순서

| # | 구분 | 파일 | 왜 이 순서 |
|---|---|---|---|
| D1~D3 | diff | `db/ddl/_source_columns.csv` · `_source_entities.csv` · `db/tools/gen_ddl.py` | 정본 행 → `python3 -I db/tools/gen_ddl.py` |
| D4~D7 | 새(생성물) | `db/ddl/TB_JL_JNAL*.sql` 4개 | D1~D3 의 결과 ✅ `gen_ddl.py --check` |
| B1 | diff | `api-messages.tsv` | 서비스가 쓰는 오류 코드 JNL_001·002 |
| B2~B5 | 새 | `web/jnal/dto/` JnalSearchDto → Jnal → JnalCheck → JnalCheckKey | 아무것도 부르지 않음 |
| B6 | 새 | `web/jnal/service/JnalRules.java` | DTO 만 씀 |
| B7~B8 | 새 | `web/jnal/mapper/JnalMapper.java` → `mapper/jnal/JnalMapper.xml` | DTO 다음 |
| B9~B10 | 새 | `JnalService` → `JnalController` | 규칙·매퍼 다음 |
| B11~B12 | 새 | 시험 2개 | ✅ `gradlew.bat build --offline` |
| F1~F4 | diff | `utils/date.ts` · `utils/format.ts` · `components/search/fields.tsx` · `optionFields.tsx` | 공통 먼저(F1 → F3 순서) |
| F5~F6 | 새 | `domain/jnal.ts` → `api/endpoints/jnal.ts` | 화면이 부름 |
| F7 | diff | `screens/screenIds.ts` | 팝업이 화면키를 씀 |
| F8~F11 | 새 | `JnalListParts` → `JnalListColumns` → `popup/JnalViewModal` → `JnalListPage` | 불리는 쪽 먼저 |
| F12 | diff | `screens/registry.ts` | F11 다음(먼저 넣으면 빨간줄) ✅ `npm run check` |

## 3. 가져다 쓴 공통 모듈

| 부품 | 어디에 | 비고 |
|---|---|---|
| `PageFrame`·`SearchForm`·`ResultHeader`·`EchoBar`·`DataGrid`(화면 페이징 20)·`ActionBar` | 화면 틀 | 사용기록조회와 같은 뼈대 |
| `officeField`·`mcnField`·`optionField`·`rangeField(maxDays 2)` | 조회조건 | 화면 로컬 칸 없음 |
| `timeRangeField({ hourly: true })` | 저널시간 | **[이번에 공통에 추가]** 시작 HH:00:00 / 끝 HH:59:59 |
| `keywordTypeFields({ optionsBy })` | 조회조건 + 검색어 | **[이번에 공통에 추가]** 저널구분에 따라 선택지 바뀜 |
| `fmtAcctNo` | 계좌번호 칸·명세서 | **[이번에 공통에 추가]** 은행코드·자릿수별 하이픈 |
| `stackCol`·`dateCol`·`timeCol`·`centerCol`·`textCol` | 목록 컬럼 | 계좌 칸 둘째 줄 = 수표 |
| `DetailModal`·`DetailGrid`·`.mono-block`·`.page-note` | 명세표 팝업 | 피그마 명세서 모양 |
| `useSearch`·`usePermission`·`useOfficeLabel` | 화면 | 페이지 없는 조회 |
| 서버 `OfficeCode`·`Dates`·`CurrentUser`·`@RequireMenuAuth`·사용기록 자동(R) | 백엔드 | 메서드 이름 `select…` |

## 4. 탭·팝업별 설명 (프론트)

### 4-1. 조회조건 (`JnalListParts.tsx` `useJnalFields`)
- 1행 사무소 · 거래결과, 2행 기번 · 저널구분, **필터 펼침** 안에 저널일자 · 조회조건 · 검색어, 저널시간(피그마 순서).
- 사무소: A·T·L(또는 관리자) = 고름(기본 내 사무소), 그 밖 = 내 사무소 고정. 기번은 사무소 종속 콤보(필수).
- 거래결과: 전체·정상·불능·취소정상·취소불능·확인요망. 저널구분: 전체·일반·수표·개국·합계·운영(As-Is 캡처 글자).
- 조회조건: 전체·수표 = 계좌번호·수표번호·명세표일련번호, 일반 = 계좌번호·명세표일련번호, 개국·합계·운영 = 명세표일련번호. 명세표는 `0001-0050`(한쪽만 써도 됨, 번호 하나 = 그 번호).
- IT부문 비관리자는 조회조건·검색어 칸이 아예 없다.

### 4-2. 목록 보기 (`JnalListColumns.tsx`)
- 11칸: 순번 · 저널일자 · 저널시간 · 저널구분(거래·개국·합계·운영) · 거래결과(운영은 빈칸) · 거래명 · 명세표 일련번호 · 계좌/카드번호/수표번호(하이픈, 수표 있으면 둘째 줄 `(수표앞8[n건])`) · 거래금액(개국·운영 빈칸) · 운영내용(운영 저널의 후인자, 잠정 Q-37) · 상세([상세] 버튼, 운영 제외).
- 위에 에코바(사무소 · 기번 · 조회일시), 결과 머리줄 오른쪽 [목록 보기 | 롤방식 보기].

### 4-3. 롤방식 보기
- 지금 결과 전체의 인자 글(전인자 + 후인자, 합친 것 모두)을 '====' 줄로 이어 붙인 읽기 전용 글 상자(남은 높이 채움, 잠정 Q-37).

### 4-4. 명세표 팝업 (`popup/JnalViewModal.tsx`)
- [명세서] 제목줄 = 거래명, 사무소 · 거래일 · 취급점_기번_일련번호 · 매체 · 개설은행(코드 그대로, Q-38) · 계좌/카드번호 · 거래금액(₩) · 영수증 글(후인자) · 빨간 안내 2줄.
- [수표목록] 탭을 열 때 `GET /api/web/jnal/checks` — 일련번호 · 수표번호.
- [저널] 인자 글 전부(고정폭). 이미지 탭은 없음(저널이미지 권한·열람기록 화면과 함께 나중).

## 5. 백엔드 요약

- `JnalController` — `selectList`·`selectChecks`, 둘 다 QRY. 사용기록 R 은 자동.
- `JnalService` — `JnalRules.normalize` → `split` → 묶음마다 `selectList` → `scrNm` → `merge`. 수표목록은 `normalizeKey`(같은 사무소·기간 규칙) 후 `selectChecks`.
- `JnalMapper.xml` — As-Is SQL 이식. 바뀐 점: `#{}`·`CAST(… AS CHAR(n))`, 인덱스 힌트·스키마 접두어 뺌, 수표 묶음은 조회조건이 수표번호일 때만 거름, 수표번호 검색 = 수표 묶음에 걸린 행(Q-36).

## 6. 검사

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

- 화면: 사무소·기번 고르고 [조회] → 목록·롤방식·[상세] 팝업 3탭. 3개월 넘은 날짜를 권한 T 로 조회하면 `JNL_002` 문구, 5년 넘으면 `JNL_001`.
