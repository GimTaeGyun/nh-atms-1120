-- 코드관리 메뉴 행 — '견본 화면'(SMP0100) 아래 소메뉴 '코드관리'(경로 /sample/code-mgt, 화면키 sample.codeMgt)
-- 2026-11-16 사용자 결정: 코드관리 화면(TB_CODE_MASTER · TB_CODE_DTL). 메뉴관리 화면에서 등록해도 같다(이 SQL 은 같은 일을 한 번에).
-- 권한: 관리자(A)는 메뉴 권한 없이도 모두 허용. 다른 권한코드는 권한관리 → 메뉴 권한 탭에서 조회·등록·수정·삭제를 체크한다.
-- 폐쇄망 개발 DB 에서 처음 확인 필요. menu_three_level.sql 다음에, 한 문장씩 실행하고 마지막에 COMMIT. 다시 돌려도 같은 결과(NOT EXISTS 조건).
-- <내 개인번호> 는 실행하는 사람의 개인번호(9자 이내)로 바꾼다.

-- 0) 지금 상태 보기 (읽기만) — 견본 화면 아래 메뉴와 /sample/code-mgt 경로가 이미 있는지
SELECT MENU_ID, PARENT_MENU_ID, MENU_NM, MENU_TYPE, MENU_PATH, SORT_NO, USE_YN FROM TB_SCR_MENU WHERE PARENT_MENU_ID = 'SMP0100' OR MENU_PATH = '/sample/code-mgt' ORDER BY MENU_ID;

-- 1) 소메뉴 — 번호 = 같은 상위 안 MAX + 1, 순서 = 마지막 + 1 (같은 경로가 이미 있으면 넣지 않음)
INSERT INTO TB_SCR_MENU (MENU_ID, PARENT_MENU_ID, MENU_NM, MENU_TYPE, MENU_PATH, SORT_NO, USE_YN, CRT_DTM, CRT_CMENO, LSCHG_DTM, LS_CMENO)
SELECT 'SMP01' || LPAD(X.N + 1, 2, '0'), 'SMP0100', '코드관리', 'M', '/sample/code-mgt', X.S + 1, 'Y',
       TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), '<내 개인번호>', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), '<내 개인번호>'
  FROM (SELECT NVL(MAX(TO_NUMBER(SUBSTR(MENU_ID, 6, 2))), 0) AS N, NVL(MAX(SORT_NO), 0) AS S
          FROM TB_SCR_MENU
         WHERE PARENT_MENU_ID = 'SMP0100'
           AND REGEXP_LIKE(MENU_ID, '^SMP01[0-9]{2}$')) X
 WHERE NOT EXISTS (SELECT 1 FROM TB_SCR_MENU WHERE MENU_PATH = '/sample/code-mgt');

-- 2) 확인
SELECT MENU_ID, PARENT_MENU_ID, MENU_NM, MENU_TYPE, MENU_PATH, SORT_NO, USE_YN FROM TB_SCR_MENU WHERE MENU_PATH = '/sample/code-mgt';

COMMIT;
