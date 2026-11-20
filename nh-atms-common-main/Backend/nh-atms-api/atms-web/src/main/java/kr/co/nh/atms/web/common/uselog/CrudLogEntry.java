package kr.co.nh.atms.web.common.uselog;

import kr.co.nh.atms.common.audit.AuditFields;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 사용기록 한 행 — TB_SCR_CUD_HIST(사용자기록이력, 정본 사진 2026-11-17). 감사 컬럼 4개는 AuditInterceptor 가 채운다.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class CrudLogEntry extends AuditFields {
    /** 이력번호 HIST_ID NUMBER(19) PK — {@link HistIdGenerator} */
    private Long histId;
    /** 부서코드 DEPT_CODE CHAR(6) — 로그인 사용자의 권한 부서 */
    private String deptCode;
    /** 사무소코드 OFC_CODE CHAR(6) — 로그인 사용자의 권한 사무소 */
    private String ofcCode;
    /** 개인번호 PER_NO VARCHAR2(9) */
    private String perNo;
    /** 메뉴경로(컬럼 아님) — INSERT 때 TB_SCR_MENU.MENU_PATH 로 MENU_ID 를 찾는다 */
    private String menuPath;
    /** C등록 U수정 D삭제 (+ 잠정 R조회 E엑셀, Q-30) CUD_TYPE CHAR(1) */
    private String cudType;
    /** 성공여부 SUCC_YN CHAR(1) */
    private String succYn;
    /** URI VARCHAR(200) */
    private String uri;
    /** 파라미터 PARAMS VARCHAR(500) — {@link CrudLogRules#params} */
    private String params;
    /** 실행SQL문장 SQL_TEXT CLOB — 자르지 않음 */
    private String sqlText;
}
