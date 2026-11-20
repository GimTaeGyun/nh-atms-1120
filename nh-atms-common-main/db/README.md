# db — 이 저장소가 쓰는 DB 자료

설계 §7, API 계약(`docs/07_API_계약.md`)과 같이 본다. 정본은 nh-atms `Document/2.개발문서/20_TOBE컬럼정의서.csv` · `20_TOBE엔터티목록.csv`.

```
db/
├─ check/   읽기 전용 확인 조회 — 먼저 돌린다
│   ├─ 01_exists.sql       15개 TB_ 테이블이 개발 DB 에 있나(내 스키마·다른 스키마·동의어)
│   ├─ 02_columns.sql      USER_/ALL_TAB_COLUMNS 컬럼·타입·NULL·PK 순서 목록(OWNER·동의어 포함) → db/ddl 과 비교
├─ change/  데이터 바꾸기 SQL (한 문장씩, 마지막 COMMIT)
│   └─ menu_three_level.sql   메뉴ID 3단계 이관(SMP000·SMP001~ → SMP0000·SMP0100·SMP0101~) + 메뉴관리 메뉴 행 (2026-11-12)
├─ ddl/     참고용 DDL (생성물, 손으로 안 고침, 그냥 돌리지 않음) — ddl/README.md
│   ├─ TB_*.sql            15개
│   └─ _source_columns.csv, _source_entities.csv   정본에서 뽑은 사본
│   ├─ 01_menu.sql, 02_auth_menu.sql, 03_fixed_user_auth.sql(틀), 10_sample_notice.sql(선택)
│   └─ 99_cleanup_samples.sql
└─ tools/
    ├─ gen_ddl.py          정본 CSV → ddl/ (python3 -I, 표준 라이브러리만)
    └─ test_gen_ddl.py     생성기 시험 (python3 -I -B)
```

## 하는 순서 (설계 Q10: DDL 은 참고용. 시드 폴더는 2026-11-11 삭제 — 사용자 결정)

1. `check/01_exists.sql` → 16개가 다 있는지 본다.
2. `check/02_columns.sql` → 컬럼이 `ddl/<TABLE>.sql` 과 같은지 본다. 다르면 정본에 맞추려 하지 말고 담당자·22 대장 질문으로.
3. 없는 테이블이 있으면 담당자와 상의(`ddl/` 는 그때 참고).
4. 새 화면 메뉴는 **메뉴관리 화면**(2026-11-12, 메뉴ID 자동 채번)으로, 권한은 권한관리 화면(메뉴 권한 탭)으로 넣는다. 메뉴관리 화면을 처음 쓰기 전 한 번 `change/menu_three_level.sql`(견본 메뉴 3단계 이관 + 메뉴관리 메뉴 행).
5. 아래 확인표에 적는다.

## 이 저장소가 쓰는 테이블

TB_NOTICE · TB_MCN_INFO · TB_CO_MODEL · TB_CO_CODE_MST · TB_CO_CODE · TB_SCR_MENU · TB_SCR_AUTH · TB_SCR_AUTH_MENU ·
TB_INSA · TB_OFC_REG_MGT · TB_SCRT_INST_PST · TB_NMS_SRC · TB_CO_SIDO · TB_CO_SIGUN · TB_NMS_HDAY · TB_OFC_RAW

- `TB_OFC_RAW`(사무소정보 Raw — 부서코드 `SAMUSO`·부서명 `SAMUSO_NAME`·사무소코드 `ONLINE_CODE`)는 2026-11-11 부서권한 때문에 추가. 정본 CSV 사본에 없어 `db/ddl` 에 DDL 이 없다(tobe 사진 판독본 nh-atms-server `docs/DB_TOBE_TABLES.md` 기준).

- `T_API_MESSAGE` 는 정본에 대응이 없어 `T_` 로 남는다(H-074). 없으면 백엔드가 조용히 건너뛴다(설계 Q7) — 이 폴더에서는 다루지 않는다.
- 사용기록은 2026-11-17 부터 정본 `TB_SCR_CUD_HIST`(판독본, `ddl/TB_SCR_CUD_HIST.sql`)에 남긴다 — 예전 `T_USER_ACTION_LOG` 는 쓰지 않는다. 메뉴 행 `change/use_log_menu.sql`.
- 이 폴더의 파일 이름에는 버전 번호를 붙이지 않는다(nh-atms `V1.6.x` 와 겹치지 않게).

## 적용 확인표 (개발 DB)

| 날짜 | DB(스키마) | 01_exists 결과 | 02 비교 결과 | 한 사람 |
|---|---|---|---|---|
| | | | | |
