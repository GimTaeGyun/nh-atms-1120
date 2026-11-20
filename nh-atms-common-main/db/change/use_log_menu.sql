-- 사용기록조회 메뉴 행 — '견본 화면'(SMP0100) 아래 소메뉴 '사용기록조회'(경로 /sample/use-log, 화면키 sample.useLog)
-- 2026-11-17 사용자 결정: 사용기록조회 화면(TB_SCR_CUD_HIST). 메뉴관리 화면에서 등록해도 같다(이 SQL 은 같은 일을 한 번에).
-- 권한: 관리자(A)는 메뉴 권한 없이도 모두 허용(모든 사무소 기록). 다른 권한코드는 권한관리 → 메뉴 권한 탭에서 '조회'만 체크한다(내 사무소 기록만 보임).
-- 기록 테이블 TB_SCR_CUD_HIST 가 없으면 기록이 남지 않을 뿐 업무는 그대로 된다(서버 로그 '[사용기록] 저장 실패') — db/check 로 먼저 확인.
-- 폐쇄망 개발 DB 에서 처음 확인 필요. menu_three_level.sql·code_mgt_menu.sql 다음에, 한 문장씩 실행하고 마지막에 COMMIT. 다시 돌려도 같은 결과(NOT EXISTS 조건).
-- <내 개인번호> 는 실행하는 사람의 개인번호(9자 이내)로 바꾼다.

-- 0) 지금 상태 보기 (읽기만) — 견본 화면 아래 메뉴와 /sample/use-log 경로가 이미 있는지
SELECT MENU_ID, PARENT_MENU_ID, MENU_NM, MENU_TYPE, MENU_PATH, SORT_NO, USE_YN FROM TB_SCR_MENU WHERE PARENT_MENU_ID = 'SMP0100' OR MENU_PATH = '/sample/use-log' ORDER BY MENU_ID;

-- 1) 소메뉴 — 번호 = 같은 상위 안 MAX + 1, 순서 = 마지막 + 1 (같은 경로가 이미 있으면 넣지 않음)
INSERT INTO TB_SCR_MENU (MENU_ID, PARENT_MENU_ID, MENU_NM, MENU_TYPE, MENU_PATH, SORT_NO, USE_YN, CRT_DTM, CRT_CMENO, LSCHG_DTM, LS_CMENO)
SELECT 'SMP01' || LPAD(X.N + 1, 2, '0'), 'SMP0100', '사용기록조회', 'M', '/sample/use-log', X.S + 1, 'Y',
       TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), '<내 개인번호>', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), '<내 개인번호>'
  FROM (SELECT NVL(MAX(TO_NUMBER(SUBSTR(MENU_ID, 6, 2))), 0) AS N, NVL(MAX(SORT_NO), 0) AS S
          FROM TB_SCR_MENU
         WHERE PARENT_MENU_ID = 'SMP0100'
           AND REGEXP_LIKE(MENU_ID, '^SMP01[0-9]{2}$')) X
 WHERE NOT EXISTS (SELECT 1 FROM TB_SCR_MENU WHERE MENU_PATH = '/sample/use-log');

-- 2) 확인
SELECT MENU_ID, PARENT_MENU_ID, MENU_NM, MENU_TYPE, MENU_PATH, SORT_NO, USE_YN FROM TB_SCR_MENU WHERE MENU_PATH = '/sample/use-log';

COMMIT;
