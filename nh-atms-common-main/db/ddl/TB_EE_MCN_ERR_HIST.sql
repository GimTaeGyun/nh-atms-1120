-- =====================================================================
-- TB_EE_MCN_ERR_HIST  기기_장애_이력
-- ---------------------------------------------------------------------
-- 생성물: db/tools/gen_ddl.py 가 만든 파일입니다. 손으로 고치지 마세요.
--         정본 CSV 를 고친 뒤 python3 -I db/tools/gen_ddl.py 로 다시 만듭니다.
-- 원본  : nh-atms Document/2.개발문서/20_TOBE컬럼정의서.csv (사본 db/ddl/_source_columns.csv)
--         이 테이블 컬럼 행 30개, 순번 9239~9268, 원본페이지 12/39, 13/39
--         20_TOBE엔터티목록.csv 순번 903, 원본페이지 12/39, 13/39
-- 주제영역: 기기관리 / 자동화기기관리_기기관리
-- 정의    : 자동화기기별 장애 발생 및 처리 이력을 관리한다
-- 업무규칙: 
-- 비고    : 판독본(정본 CSV 대조 필요)
-- PK      : OFC_CODE, MCN_NO, HIST_COMM_NO, ERR_OCR_DT
-- FK      : 만들지 않는다(원본 FK 전부 N, 21지침 §6)
-- 주의    : 참고용 DDL 이다. 개발 DB 에 같은 이름 테이블이 이미 있을 수 있으니 db/check 로 먼저 확인(설계 Q10).
-- 경고    :
--   * 판독메모 OFC_CODE: 테이블 전체가 판독본(nh-atms-server docs/DB_TOBE_TABLES.md, 사진 판독) 행 — 정본 CSV 대조 필요
-- =====================================================================
SET DEFINE OFF

CREATE TABLE TB_EE_MCN_ERR_HIST (
    OFC_CODE            CHAR(6)       NOT NULL,
    MCN_NO              CHAR(3)       NOT NULL,
    HIST_COMM_NO        VARCHAR2(18)  NOT NULL,
    ERR_OCR_DT          CHAR(8)       NOT NULL,
    INST_PST_CODE       VARCHAR2(3),
    VEND_CODE           CHAR(3),
    BLT_OLD_OFC_CODE    CHAR(6),
    SRC                 VARCHAR2(3),
    MO_KIND             CHAR(1),
    ERR_NM              VARCHAR2(80),
    ERR_OCR_TIME        VARCHAR2(6),
    MO_REQ_DT           CHAR(8),
    MO_REQ_TIME         VARCHAR2(6),
    ARV_DT              CHAR(8),
    ARV_TIME            VARCHAR2(6),
    MO_NOFT_KIND        VARCHAR2(1),
    MGM_END_DT          CHAR(8),
    MGM_END_TIME        VARCHAR2(6),
    MGM_REST            VARCHAR2(1),
    ERR_MO_USE_TIME     NUMBER(15),
    ERR_RCVR_USE_TIME   NUMBER(15),
    HMIN_MO_YN          CHAR(1),
    QMIN_RCVR_YN        CHAR(1),
    CASH_END_PROC_TIME  NUMBER(15),
    CT_DT               CHAR(14),
    SLA_VALU_TARGET_YN  CHAR(1),
    CRT_DTM             CHAR(14),
    CRT_CMENO           VARCHAR2(9),
    LSCHG_DTM           CHAR(14),
    LS_CMENO            VARCHAR2(9),
    CONSTRAINT PK_TB_EE_MCN_ERR_HIST PRIMARY KEY (OFC_CODE, MCN_NO, HIST_COMM_NO, ERR_OCR_DT)
);

COMMENT ON TABLE TB_EE_MCN_ERR_HIST IS '기기_장애_이력 - 자동화기기별 장애 발생 및 처리 이력을 관리한다';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.OFC_CODE IS '사무소코드';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.MCN_NO IS '기번';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.HIST_COMM_NO IS '히스토리통번';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.ERR_OCR_DT IS '장애_발생_일자';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.INST_PST_CODE IS '코너코드';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.VEND_CODE IS '업체코드';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.BLT_OLD_OFC_CODE IS '소속영업점코드(기존물리적영업점)';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.SRC IS '기종_구분_코드(SELECT SRC,SRC_NM FROM …)';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.MO_KIND IS '출동종류(즉시출동=1,10분후출동=2,수동…)';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.ERR_NM IS '장애_명';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.ERR_OCR_TIME IS '장애_발생_시간';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.MO_REQ_DT IS '출동_요청_일자';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.MO_REQ_TIME IS '출동_요청_시간';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.ARV_DT IS '도착일(YYYYMMDD)';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.ARV_TIME IS '도착시간(HHMMSS)';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.MO_NOFT_KIND IS '1:용역사1차출동,2:기기사2차출동';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.MGM_END_DT IS '조치종료일(YYYYMMDD)';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.MGM_END_TIME IS '조치종료시간(HHMMSS)';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.MGM_REST IS '1:미조치2:조치완료(1차출동에관한것)';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.ERR_MO_USE_TIME IS '장애_출동_소요_시간(초)';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.ERR_RCVR_USE_TIME IS '장애_복구_소요_시간(초)';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.HMIN_MO_YN IS '30분_출동_여부';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.QMIN_RCVR_YN IS '15분_복구_여부';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.CASH_END_PROC_TIME IS '현금부족_처리시간(초)';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.CT_DT IS '생성_일시(YYYYMMDDHH24MISS)';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.SLA_VALU_TARGET_YN IS 'SLA_평가_대상_여부';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.CRT_DTM IS '생성일시 - 해당데이터가 생성된 일시를 관리';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.CRT_CMENO IS '생성개인번호 - 생성 담당자의 개인 번호';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.LSCHG_DTM IS '최종변경일시 - 해당데이터가 마지막으로 수정된 일시를 관리';
COMMENT ON COLUMN TB_EE_MCN_ERR_HIST.LS_CMENO IS '최종변경자개인번호 - 최종 수정 담당자의 개인 번호';
