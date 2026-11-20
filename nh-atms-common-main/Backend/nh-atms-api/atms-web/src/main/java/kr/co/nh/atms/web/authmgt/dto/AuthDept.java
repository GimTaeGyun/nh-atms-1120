package kr.co.nh.atms.web.authmgt.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 부서 1행 — TB_OFC_RAW(사무소정보 Raw Data). API 계약 §8 {@code AuthDept}.
 * 부서권한 등록 팝업의 부서 콤보, 개인권한 등록 때 그 사람의 소속 부서·사무소(키 자동 채움)에 쓴다.
 */
@Getter
@Setter
public class AuthDept {
    /** 부서코드 TB_OFC_RAW.SAMUSO VARCHAR2(6) PK = TB_SCR_AUTH.DEPT_CODE */
    private String samuso;
    /** 부서명 TB_OFC_RAW.SAMUSO_NAME */
    private String samusoName;
    /** 사무소코드 TB_OFC_RAW.ONLINE_CODE = TB_SCR_AUTH.OFC_CODE */
    private String onlineCode;
}
