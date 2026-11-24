-- =====================================================================
-- TB_JL_JNAL_CHK_ILM  전자저널_저널수표정보_ILM
-- ---------------------------------------------------------------------
-- 생성물: db/tools/gen_ddl.py 가 만든 파일입니다. 손으로 고치지 마세요.
--         정본 CSV 를 고친 뒤 python3 -I db/tools/gen_ddl.py 로 다시 만듭니다.
-- 원본  : nh-atms Document/2.개발문서/20_TOBE컬럼정의서.csv (사본 db/ddl/_source_columns.csv)
--         이 테이블 컬럼 행 12개, 순번 426~437, 원본페이지 6/39
--         20_TOBE엔터티목록.csv 순번 25, 원본페이지 2/6
-- 주제영역: 전자저널 ILM / 자동화기기관리_저널_ILM
-- 정의    : 전자저널에 포함된 수표 거래 상세정보 내역을 관리한다
-- 업무규칙: 수표 관련 저널 처리 시 해당 저널과 연계하여 생성한다.
-- PK      : JNAL_DT, OFC_CODE, MCN_NO, JNAL_SEQNO, SEQNO
-- FK      : 만들지 않는다(원본 FK 전부 N, 21지침 §6)
-- 주의    : 참고용 DDL 이다. 개발 DB 에 같은 이름 테이블이 이미 있을 수 있으니 db/check 로 먼저 확인(설계 Q10).
-- =====================================================================
SET DEFINE OFF

CREATE TABLE TB_JL_JNAL_CHK_ILM (
    JNAL_DT     CHAR(8)       NOT NULL,
    OFC_CODE    CHAR(6)       NOT NULL,
    MCN_NO      CHAR(3)       NOT NULL,
    JNAL_SEQNO  CHAR(6)       NOT NULL,
    SEQNO       CHAR(2)       NOT NULL,
    DEAL_DATE   CHAR(8),
    DTLS_SEQNO  CHAR(4),
    CHK_NO      VARCHAR2(17),
    CRT_DTM     CHAR(14),
    CRT_CMENO   VARCHAR2(9),
    LSCHG_DTM   CHAR(14),
    LS_CMENO    VARCHAR2(9),
    CONSTRAINT PK_TB_JL_JNAL_CHK_ILM PRIMARY KEY (JNAL_DT, OFC_CODE, MCN_NO, JNAL_SEQNO, SEQNO)
);

COMMENT ON TABLE TB_JL_JNAL_CHK_ILM IS '전자저널_저널수표정보_ILM - 전자저널에 포함된 수표 거래 상세정보 내역을 관리한다';
COMMENT ON COLUMN TB_JL_JNAL_CHK_ILM.JNAL_DT IS '저널일자 - 저널일자를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_CHK_ILM.OFC_CODE IS '사무소코드 - 사무소코드를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_CHK_ILM.MCN_NO IS '기번 - 기번을 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_CHK_ILM.JNAL_SEQNO IS '저널일련번호 - 저널일련번호를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_CHK_ILM.SEQNO IS '일련번호(SQ_CO_REQSEQ.NEXTVAL) - 일련번호(SQ_CO_REQSEQ.NEXTVAL)를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_CHK_ILM.DEAL_DATE IS '거래날짜 - 거래날짜를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_CHK_ILM.DTLS_SEQNO IS '명세표일련번호 - 명세표일련번호를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_CHK_ILM.CHK_NO IS '수표번호 - 수표번호를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_CHK_ILM.CRT_DTM IS '생성일시 - 해당데이터가 생성된 일시를 관리';
COMMENT ON COLUMN TB_JL_JNAL_CHK_ILM.CRT_CMENO IS '생성개인번호 - 생성 담당자의 개인 번호';
COMMENT ON COLUMN TB_JL_JNAL_CHK_ILM.LSCHG_DTM IS '최종변경일시 - 해당데이터가 마지막으로 수정된 일시를 관리';
COMMENT ON COLUMN TB_JL_JNAL_CHK_ILM.LS_CMENO IS '최종변경자개인번호 - 최종 수정 담당자의 개인 번호';
