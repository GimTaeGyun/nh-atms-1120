-- =====================================================================
-- TB_JL_JNAL_ILM  전자저널_저널정보_ILM
-- ---------------------------------------------------------------------
-- 생성물: db/tools/gen_ddl.py 가 만든 파일입니다. 손으로 고치지 마세요.
--         정본 CSV 를 고친 뒤 python3 -I db/tools/gen_ddl.py 로 다시 만듭니다.
-- 원본  : nh-atms Document/2.개발문서/20_TOBE컬럼정의서.csv (사본 db/ddl/_source_columns.csv)
--         이 테이블 컬럼 행 36개, 순번 390~425, 원본페이지 6/39
--         20_TOBE엔터티목록.csv 순번 24, 원본페이지 2/6
-- 주제영역: 전자저널 ILM / 자동화기기관리_저널_ILM
-- 정의    : 자동화기기에서 수집한 전자저널 내역을 관리한다.
-- 업무규칙: 전자저널 수신 및 저장 처리 시 생성한다.
-- PK      : JNAL_DT, OFC_CODE, MCN_NO, JNAL_SEQNO
-- FK      : 만들지 않는다(원본 FK 전부 N, 21지침 §6)
-- 주의    : 참고용 DDL 이다. 개발 DB 에 같은 이름 테이블이 이미 있을 수 있으니 db/check 로 먼저 확인(설계 Q10).
-- =====================================================================
SET DEFINE OFF

CREATE TABLE TB_JL_JNAL_ILM (
    JNAL_DT         CHAR(8)         NOT NULL,
    OFC_CODE        CHAR(6)         NOT NULL,
    MCN_NO          CHAR(3)         NOT NULL,
    JNAL_SEQNO      CHAR(6)         NOT NULL,
    JNAL_TYPE       VARCHAR2(2),
    JNAL_TIME       CHAR(6),
    JRNL_PROC_REST  VARCHAR2(2),
    JNAL_INCMPT_YN  CHAR(1),
    ATM_COPT_YN     VARCHAR2(2),
    DEAL_DATE       CHAR(8),
    DEAL_TIME       CHAR(6),
    DTLS_SEQNO      CHAR(4),
    SCR_NO          CHAR(4),
    DEAL_NM         VARCHAR2(30),
    ACCT_NO         VARCHAR2(50),
    MDN_TYPE        VARCHAR2(2),
    ESTB_BANK_CODE  VARCHAR2(2),
    CARD_NO         VARCHAR2(50),
    CHK_CNT         NUMBER(2),
    CHK_ISSUE_REST  VARCHAR2(2),
    DEAL_AMT        NUMBER(22),
    IMG_TYPE        VARCHAR2(1),
    FACE_IMG_YN     CHAR(1),
    ACPT_IMG_YN     CHAR(1),
    CARD_IMG_YN     CHAR(1),
    BPR_PROC_STAT   VARCHAR2(2),
    BFO_COMT        VARCHAR2(2048),
    AFT_COMT        VARCHAR2(2048),
    ERR_SVR_ID      VARCHAR2(2),
    ERR_MDL_NM      VARCHAR2(20),
    ERR_BIZ_NM      VARCHAR2(20),
    ERR_CODE        VARCHAR2(10),
    CRT_DTM         CHAR(14),
    CRT_CMENO       VARCHAR2(9),
    LSCHG_DTM       CHAR(14),
    LS_CMENO        VARCHAR2(9),
    CONSTRAINT PK_TB_JL_JNAL_ILM PRIMARY KEY (JNAL_DT, OFC_CODE, MCN_NO, JNAL_SEQNO)
);

COMMENT ON TABLE TB_JL_JNAL_ILM IS '전자저널_저널정보_ILM - 자동화기기에서 수집한 전자저널 내역을 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.JNAL_DT IS '저널일자 - 저널일자를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.OFC_CODE IS '사무소코드 - 사무소코드를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.MCN_NO IS '기번 - 기번을 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.JNAL_SEQNO IS '저널일련번호 - 저널일련번호를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.JNAL_TYPE IS '저널구분 - 저널구분을 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.JNAL_TIME IS '저널시간 - 저널시간을 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.JRNL_PROC_REST IS '저널처리결과 - 저널처리결과를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.JNAL_INCMPT_YN IS '저널미완료여부 - 저널미완료여부를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.ATM_COPT_YN IS 'ATM완료여부 - ATM완료여부를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.DEAL_DATE IS '거래날짜 - 거래날짜를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.DEAL_TIME IS '거래시간 - 거래시간을 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.DTLS_SEQNO IS '명세표일련번호 - 명세표일련번호를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.SCR_NO IS '화면번호 - 화면번호를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.DEAL_NM IS '거래명 - 거래명을 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.ACCT_NO IS '계좌번호 - 계좌번호를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.MDN_TYPE IS '매체구분 - 매체구분을 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.ESTB_BANK_CODE IS '개설은행코드 - 개설은행코드를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.CARD_NO IS '카드번호 - 카드번호를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.CHK_CNT IS '수표건수 - 수표건수를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.CHK_ISSUE_REST IS '수표발급결과 - 수표발급결과를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.DEAL_AMT IS '거래금액 - 거래금액을 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.IMG_TYPE IS '이미지구분 - 이미지구분을 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.FACE_IMG_YN IS '안면이미지여부 - 안면이미지여부를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.ACPT_IMG_YN IS '수취이미지여부 - 수취이미지여부를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.CARD_IMG_YN IS '카드이미지여부 - 카드이미지여부를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.BPR_PROC_STAT IS 'BPR처리상태 - BPR처리상태를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.BFO_COMT IS '전인자내용 - 전인자내용을 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.AFT_COMT IS '후인자내용 - 후인자내용을 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.ERR_SVR_ID IS '서버ID - 서버ID를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.ERR_MDL_NM IS '에러서비스명 - 에러서비스명을 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.ERR_BIZ_NM IS '에러모듈명 - 에러모듈명을 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.ERR_CODE IS '장애코드 - 장애코드를 관리한다.';
COMMENT ON COLUMN TB_JL_JNAL_ILM.CRT_DTM IS '생성일시 - 해당데이터가 생성된 일시를 관리';
COMMENT ON COLUMN TB_JL_JNAL_ILM.CRT_CMENO IS '생성개인번호 - 생성 담당자의 개인 번호';
COMMENT ON COLUMN TB_JL_JNAL_ILM.LSCHG_DTM IS '최종변경일시 - 해당데이터가 마지막으로 수정된 일시를 관리';
COMMENT ON COLUMN TB_JL_JNAL_ILM.LS_CMENO IS '최종변경자개인번호 - 최종 수정 담당자의 개인 번호';
