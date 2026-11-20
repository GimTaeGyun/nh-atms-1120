-- =====================================================================
-- TB_CODE_MASTER  코드마스터
-- ---------------------------------------------------------------------
-- 생성물: db/tools/gen_ddl.py 가 만든 파일입니다. 손으로 고치지 마세요.
--         정본 CSV 를 고친 뒤 python3 -I db/tools/gen_ddl.py 로 다시 만듭니다.
-- 원본  : nh-atms Document/2.개발문서/20_TOBE컬럼정의서.csv (사본 db/ddl/_source_columns.csv)
--         이 테이블 컬럼 행 14개, 순번 9301~9314, 원본페이지 6/39, 7/39
--         20_TOBE엔터티목록.csv 순번 911, 원본페이지 6/39, 7/39
-- 주제영역: 기기관리 / 자동화기기관리_기기관리
-- 정의    : 업무에서 사용하는 기준 코드 정보를 관리한다
-- 업무규칙: 코드 구분별 상세코드를 등록,변경한다
-- 비고    : 판독본(정본 CSV 대조 필요)
-- PK      : CODE_SECT
-- FK      : 만들지 않는다(원본 FK 전부 N, 21지침 §6)
-- 주의    : 참고용 DDL 이다. 개발 DB 에 같은 이름 테이블이 이미 있을 수 있으니 db/check 로 먼저 확인(설계 Q10).
-- 경고    :
--   * 판독메모 CODE_SECT: 테이블 전체가 판독본(nh-atms-server docs/DB_TOBE_TABLES.md, 사진 판독) 행 — 정본 CSV 대조 필요 · CODE_SECT 판독 VARCHAR2(2) → VARCHAR2(10) 정정(2026-11-16 사용자 확인)
-- =====================================================================
SET DEFINE OFF

CREATE TABLE TB_CODE_MASTER (
    CODE_SECT     VARCHAR2(10)   NOT NULL,
    CODE_SECT_NM  VARCHAR2(100),
    DESC1         VARCHAR2(100),
    DESC2         VARCHAR2(100),
    DESC3         VARCHAR2(100),
    DESC4         VARCHAR2(100),
    DESC5         VARCHAR2(100),
    TASK_SECT     VARCHAR2(10),
    PRC_SECT      CHAR(1),
    USED_YN       CHAR(1),
    CRT_DTM       CHAR(14),
    CRT_CMENO     VARCHAR2(9),
    LSCHG_DTM     CHAR(14),
    LS_CMENO      VARCHAR2(9),
    CONSTRAINT PK_TB_CODE_MASTER PRIMARY KEY (CODE_SECT)
);

COMMENT ON TABLE TB_CODE_MASTER IS '코드마스터 - 업무에서 사용하는 기준 코드 정보를 관리한다';
COMMENT ON COLUMN TB_CODE_MASTER.CODE_SECT IS '코드구분';
COMMENT ON COLUMN TB_CODE_MASTER.CODE_SECT_NM IS '코드구분명';
COMMENT ON COLUMN TB_CODE_MASTER.DESC1 IS '설명1';
COMMENT ON COLUMN TB_CODE_MASTER.DESC2 IS '설명2';
COMMENT ON COLUMN TB_CODE_MASTER.DESC3 IS '설명3';
COMMENT ON COLUMN TB_CODE_MASTER.DESC4 IS '설명4';
COMMENT ON COLUMN TB_CODE_MASTER.DESC5 IS '설명5';
COMMENT ON COLUMN TB_CODE_MASTER.TASK_SECT IS '업무구분';
COMMENT ON COLUMN TB_CODE_MASTER.PRC_SECT IS '처리구분';
COMMENT ON COLUMN TB_CODE_MASTER.USED_YN IS '사용여부';
COMMENT ON COLUMN TB_CODE_MASTER.CRT_DTM IS '생성일시';
COMMENT ON COLUMN TB_CODE_MASTER.CRT_CMENO IS '생성개인번호';
COMMENT ON COLUMN TB_CODE_MASTER.LSCHG_DTM IS '최종변경일시';
COMMENT ON COLUMN TB_CODE_MASTER.LS_CMENO IS '최종변경자개인번호';
