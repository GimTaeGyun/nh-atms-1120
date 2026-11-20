-- =====================================================================
-- TB_SCR_CUD_HIST  사용자기록이력
-- ---------------------------------------------------------------------
-- 생성물: db/tools/gen_ddl.py 가 만든 파일입니다. 손으로 고치지 마세요.
--         정본 CSV 를 고친 뒤 python3 -I db/tools/gen_ddl.py 로 다시 만듭니다.
-- 원본  : nh-atms Document/2.개발문서/20_TOBE컬럼정의서.csv (사본 db/ddl/_source_columns.csv)
--         이 테이블 컬럼 행 14개, 순번 9401~9414, 원본페이지 엑셀 사진(2026-11-17)
--         20_TOBE엔터티목록.csv 순번 913, 원본페이지 엑셀 사진(2026-11-17)
-- 주제영역: 기기관리 / 자동화기기관리_기기관리
-- 정의    : 화면에서 한 조회·등록·수정·삭제·엑셀 기록을 관리한다
-- 업무규칙: 화면 요청마다 생성한다(사용기록, 2026-11-17)
-- 비고    : 판독본(정본 CSV 대조 필요) — 정의·규칙 글은 이 저장소가 적은 것
-- PK      : HIST_ID
-- FK      : 만들지 않는다(원본 FK 전부 N, 21지침 §6)
-- 주의    : 참고용 DDL 이다. 개발 DB 에 같은 이름 테이블이 이미 있을 수 있으니 db/check 로 먼저 확인(설계 Q10).
-- 경고    :
--   * 판독메모 HIST_ID: 테이블 전체가 판독본(2026-11-17 사용자 제공 엑셀 '본문(컬럼 목록)' 사진) 행 — 정본 CSV 대조 필요 · 시퀀스 이름 없음 → 앱이 시각14+서버1+일련4 로 만든다(잠정 Q-31)
--   * 판독메모 CUD_TYPE: R(조회)·E(엑셀)은 잠정으로 더 쓴다(2026-11-17 사용자 결정, Q-30)
--   * 판독메모 URI: 사진에는 VARCHAR(200) — Oracle 에서 VARCHAR 는 VARCHAR2 와 같아 VARCHAR2 로 적음(Q-32)
--   * 판독메모 PARAMS: 사진에는 VARCHAR(500) — VARCHAR2 로 적음(Q-32). IP·권한·오류코드를 머리에 함께 적는다(2026-11-17 사용자 결정)
-- =====================================================================
SET DEFINE OFF

CREATE TABLE TB_SCR_CUD_HIST (
    HIST_ID    NUMBER(19)     NOT NULL,
    DEPT_CODE  CHAR(6),
    OFC_CODE   CHAR(6),
    PER_NO     VARCHAR2(9),
    MENU_ID    VARCHAR2(20),
    CUD_TYPE   CHAR(1),
    SUCC_YN    CHAR(1),
    URI        VARCHAR2(200),
    PARAMS     VARCHAR2(500),
    SQL_TEXT   CLOB,
    CRT_DTM    CHAR(14),
    CRT_CMENO  VARCHAR2(9),
    LSCHG_DTM  CHAR(14),
    LS_CMENO   VARCHAR2(9),
    CONSTRAINT PK_TB_SCR_CUD_HIST PRIMARY KEY (HIST_ID)
);

COMMENT ON TABLE TB_SCR_CUD_HIST IS '사용자기록이력 - 화면에서 한 조회·등록·수정·삭제·엑셀 기록을 관리한다';
COMMENT ON COLUMN TB_SCR_CUD_HIST.HIST_ID IS '이력번호';
COMMENT ON COLUMN TB_SCR_CUD_HIST.DEPT_CODE IS '부서코드';
COMMENT ON COLUMN TB_SCR_CUD_HIST.OFC_CODE IS '사무소코드';
COMMENT ON COLUMN TB_SCR_CUD_HIST.PER_NO IS '개인번호';
COMMENT ON COLUMN TB_SCR_CUD_HIST.MENU_ID IS '메뉴ID';
COMMENT ON COLUMN TB_SCR_CUD_HIST.CUD_TYPE IS 'C:등록 U:수정 D:삭제';
COMMENT ON COLUMN TB_SCR_CUD_HIST.SUCC_YN IS '성공여부';
COMMENT ON COLUMN TB_SCR_CUD_HIST.URI IS 'URI';
COMMENT ON COLUMN TB_SCR_CUD_HIST.PARAMS IS '파라미터';
COMMENT ON COLUMN TB_SCR_CUD_HIST.SQL_TEXT IS '실행SQL문장';
COMMENT ON COLUMN TB_SCR_CUD_HIST.CRT_DTM IS '생성일시';
COMMENT ON COLUMN TB_SCR_CUD_HIST.CRT_CMENO IS '생성개인번호';
COMMENT ON COLUMN TB_SCR_CUD_HIST.LSCHG_DTM IS '최종변경일시';
COMMENT ON COLUMN TB_SCR_CUD_HIST.LS_CMENO IS '최종변경자개인번호';
