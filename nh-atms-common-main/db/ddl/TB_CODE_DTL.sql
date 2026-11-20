-- =====================================================================
-- TB_CODE_DTL  상세코드
-- ---------------------------------------------------------------------
-- 생성물: db/tools/gen_ddl.py 가 만든 파일입니다. 손으로 고치지 마세요.
--         정본 CSV 를 고친 뒤 python3 -I db/tools/gen_ddl.py 로 다시 만듭니다.
-- 원본  : nh-atms Document/2.개발문서/20_TOBE컬럼정의서.csv (사본 db/ddl/_source_columns.csv)
--         이 테이블 컬럼 행 12개, 순번 9315~9326, 원본페이지 7/39
--         20_TOBE엔터티목록.csv 순번 912, 원본페이지 7/39
-- 주제영역: 기기관리 / 자동화기기관리_기기관리
-- 정의    : 코드 구분별 상세코드와 설명을 관리한다
-- 업무규칙: 상세코드 등록 및 변경 시 생성,갱신한다
-- 비고    : 판독본(정본 CSV 대조 필요)
-- PK      : CODE_SECT, CODE
-- FK      : 만들지 않는다(원본 FK 전부 N, 21지침 §6)
-- 주의    : 참고용 DDL 이다. 개발 DB 에 같은 이름 테이블이 이미 있을 수 있으니 db/check 로 먼저 확인(설계 Q10).
-- 경고    :
--   * 판독메모 CODE_SECT: 테이블 전체가 판독본(nh-atms-server docs/DB_TOBE_TABLES.md, 사진 판독) 행 — 정본 CSV 대조 필요
-- =====================================================================
SET DEFINE OFF

CREATE TABLE TB_CODE_DTL (
    CODE_SECT  VARCHAR2(10)  NOT NULL,
    CODE       VARCHAR2(10)  NOT NULL,
    CODE_NM    VARCHAR2(80),
    DATA1      VARCHAR2(80),
    DATA2      VARCHAR2(80),
    DATA3      VARCHAR2(80),
    DATA4      NUMBER(4),
    DATA5      CHAR(1),
    CRT_DTM    CHAR(14),
    CRT_CMENO  VARCHAR2(9),
    LSCHG_DTM  CHAR(14),
    LS_CMENO   VARCHAR2(9),
    CONSTRAINT PK_TB_CODE_DTL PRIMARY KEY (CODE_SECT, CODE)
);

COMMENT ON TABLE TB_CODE_DTL IS '상세코드 - 코드 구분별 상세코드와 설명을 관리한다';
COMMENT ON COLUMN TB_CODE_DTL.CODE_SECT IS '코드구분';
COMMENT ON COLUMN TB_CODE_DTL.CODE IS '코드';
COMMENT ON COLUMN TB_CODE_DTL.CODE_NM IS '코드명';
COMMENT ON COLUMN TB_CODE_DTL.DATA1 IS '데이터1';
COMMENT ON COLUMN TB_CODE_DTL.DATA2 IS '데이터2';
COMMENT ON COLUMN TB_CODE_DTL.DATA3 IS '데이터3';
COMMENT ON COLUMN TB_CODE_DTL.DATA4 IS '데이터4';
COMMENT ON COLUMN TB_CODE_DTL.DATA5 IS '데이터5';
COMMENT ON COLUMN TB_CODE_DTL.CRT_DTM IS '생성일시';
COMMENT ON COLUMN TB_CODE_DTL.CRT_CMENO IS '생성개인번호';
COMMENT ON COLUMN TB_CODE_DTL.LSCHG_DTM IS '최종변경일시';
COMMENT ON COLUMN TB_CODE_DTL.LS_CMENO IS '최종변경자개인번호';
