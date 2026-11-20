package kr.co.nh.atms.web.codemgt.dto;

import kr.co.nh.atms.common.audit.AuditFields;
import lombok.Getter;
import lombok.Setter;

/**
 * 상세코드 1행 — API 계약 §11 {@code CodeDetail extends Audit}. TB_CODE_DTL.
 * 상세의 코드구분(CODE_SECT)으로 마스터(TB_CODE_MASTER)를 찾는다(물리 FK 없음 — 서비스가 확인).
 */
@Getter
@Setter
public class CodeDetail extends AuditFields {
    /** 코드구분 CODE_SECT VARCHAR2(10) PK */
    private String codeSect;
    /** 코드 CODE VARCHAR2(10) PK */
    private String code;
    /** 코드명 CODE_NM VARCHAR2(80) */
    private String codeNm;
    /** 데이터1~3 DATA1~3 VARCHAR2(80) — 표에 데이터1~5 모두 보임(2026-11-16 사용자 결정) */
    private String data1;
    private String data2;
    private String data3;
    /** 데이터4 DATA4 NUMBER(4) -9999~9999 */
    private Integer data4;
    /** 데이터5 DATA5 CHAR(1) */
    private String data5;
}
