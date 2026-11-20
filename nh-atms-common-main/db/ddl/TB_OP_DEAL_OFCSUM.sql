-- =====================================================================
-- TB_OP_DEAL_OFCSUM  거래집계
-- ---------------------------------------------------------------------
-- 생성물: db/tools/gen_ddl.py 가 만든 파일입니다. 손으로 고치지 마세요.
--         정본 CSV 를 고친 뒤 python3 -I db/tools/gen_ddl.py 로 다시 만듭니다.
-- 원본  : nh-atms Document/2.개발문서/20_TOBE컬럼정의서.csv (사본 db/ddl/_source_columns.csv)
--         이 테이블 컬럼 행 22개, 순번 9217~9238, 원본페이지 31/39, 32/39
--         20_TOBE엔터티목록.csv 순번 902, 원본페이지 31/39, 32/39
-- 주제영역: 기기관리 / 자동화기기관리_기기관리
-- 정의    : 사무소별 거래 건수 및 금액의 집계를 관리한다
-- 업무규칙: 
-- 비고    : 판독본(정본 CSV 대조 필요)
-- PK      : DEAL_DT, OFC_CODE, MCN_NO, DEAL_TIME
-- FK      : 만들지 않는다(원본 FK 전부 N, 21지침 §6)
-- 주의    : 참고용 DDL 이다. 개발 DB 에 같은 이름 테이블이 이미 있을 수 있으니 db/check 로 먼저 확인(설계 Q10).
-- 경고    :
--   * 판독메모 DEAL_DT: 테이블 전체가 판독본(nh-atms-server docs/DB_TOBE_TABLES.md, 사진 판독) 행 — 정본 CSV 대조 필요
-- =====================================================================
SET DEFINE OFF

CREATE TABLE TB_OP_DEAL_OFCSUM (
    DEAL_DT          CHAR(8)      NOT NULL,
    OFC_CODE         CHAR(6)      NOT NULL,
    MCN_NO           CHAR(3)      NOT NULL,
    DEAL_TIME        CHAR(6)      NOT NULL,
    CASH_PMT_CNT     NUMBER(8),
    CASH_PMT_AMT     NUMBER(12),
    CHK_PMT_CNT      NUMBER(8),
    CHK_PMT_AMT      NUMBER(12),
    CASH_DEPO_CNT    NUMBER(8),
    CASH_DEPO_AMT    NUMBER(12),
    CHK_DEPO_CNT     NUMBER(8),
    CHK_DEPO_AMT     NUMBER(12),
    B10000_PMT_AMT   NUMBER(12),
    B50000_PMT_AMT   NUMBER(12),
    B1000_DEPO_AMT   NUMBER(12),
    B5000_DEPO_AMT   NUMBER(12),
    B10000_DEPO_AMT  NUMBER(12),
    B50000_DEPO_AMT  NUMBER(12),
    CRT_DTM          CHAR(14),
    CRT_CMENO        VARCHAR2(9),
    LSCHG_DTM        CHAR(14),
    LS_CMENO         VARCHAR2(9),
    CONSTRAINT PK_TB_OP_DEAL_OFCSUM PRIMARY KEY (DEAL_DT, OFC_CODE, MCN_NO, DEAL_TIME)
);

COMMENT ON TABLE TB_OP_DEAL_OFCSUM IS '거래집계 - 사무소별 거래 건수 및 금액의 집계를 관리한다';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.DEAL_DT IS '거래일자';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.OFC_CODE IS '사무소코드';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.MCN_NO IS '기번';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.DEAL_TIME IS '거래시간';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.CASH_PMT_CNT IS '현금지급건수';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.CASH_PMT_AMT IS '현금지급금액';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.CHK_PMT_CNT IS '수표지급건수';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.CHK_PMT_AMT IS '수표발행액금액단위:원';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.CASH_DEPO_CNT IS '현금입금건수';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.CASH_DEPO_AMT IS '현금입금금액';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.CHK_DEPO_CNT IS '수표입금건수';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.CHK_DEPO_AMT IS '수표입금액금액단위:원';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.B10000_PMT_AMT IS '만원권_출금_금액';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.B50000_PMT_AMT IS '오만원권_출금_금액';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.B1000_DEPO_AMT IS '천원권_입금_금액';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.B5000_DEPO_AMT IS '오천원권_입금_금액';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.B10000_DEPO_AMT IS '만원권_입금_금액';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.B50000_DEPO_AMT IS '오만원권_입금_금액';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.CRT_DTM IS '생성일시 - 해당데이터가 생성된 일시를 관리';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.CRT_CMENO IS '생성개인번호 - 생성 담당자의 개인 번호';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.LSCHG_DTM IS '최종변경일시 - 해당데이터가 마지막으로 수정된 일시를 관리';
COMMENT ON COLUMN TB_OP_DEAL_OFCSUM.LS_CMENO IS '최종변경자개인번호 - 최종 수정 담당자의 개인 번호';
