package kr.co.nh.atms.web.authmgt.dto;

import kr.co.nh.atms.common.audit.AuditFields;
import lombok.Getter;
import lombok.Setter;

/**
 * 사용자 권한 1행(부서권한·개인권한) — API 계약 §8 {@code AuthUser extends Audit}. TB_SCR_AUTH ⟕ TB_INSA(NAME) ⟕ TB_OFC_REG_MGT(사무소명) ⟕ TB_OFC_RAW(부서명) ⟕ TB_CO_CODE(권한명, 설정 시).
 * 키 = DEPT_CODE + OFC_CODE + PER_NO. CHAR 컬럼은 SQL 에서 RTRIM 해서 내린다.
 * 등록·수정 때 매퍼 파라미터로도 쓴다 — {@link AuditFields} 라서 감사 4컬럼은 AuditInterceptor 가 채운다.
 */
@Getter
@Setter
public class AuthUser extends AuditFields {
    /** 부서코드 DEPT_CODE CHAR(6) PK (넣는 값은 잠정 Q-13) */
    private String deptCode;
    /** 부서명 — TB_OFC_RAW.SAMUSO_NAME (TB_OFC_RAW.SAMUSO = DEPT_CODE) */
    private String deptNm;
    /** 사무소코드 OFC_CODE CHAR(6) PK */
    private String ofcCode;
    /** 사무소명 TB_OFC_REG_MGT.KANM */
    private String kanm;
    /** 개인번호 PER_NO VARCHAR2(9) PK — '1' 이면 부서권한(AuthMgtRules.DEPT_PER_NO) */
    private String perNo;
    /** 성명 TB_INSA.NAME */
    private String name;
    /** 권한 AUTH VARCHAR2(2) (값의 뜻은 잠정 Q-09) */
    private String auth;
    /** 권한명 — 설정 atms.auth-mgt.auth-code-type 이 있을 때 TB_CO_CODE.CODE_NM, 없으면 null(잠정 Q-20) */
    private String authNm;
    /** 승인상태 APV_STS CHAR(2) — 'Y' 승인, 'N' 미승인(잠정 Q-18) */
    private String apvSts;
    /** 메모 MEMO VARCHAR2(100) */
    private String memo;
    /** 만료일시 DUE_DATE CHAR(14) YYYYMMDDHH24MISS, null = 무기한 */
    private String dueDate;
    /** 등록일시 INS_DATE CHAR(14) */
    private String insDate;
    /** 등록자 INS_ID CHAR(9) */
    private String insId;
}
