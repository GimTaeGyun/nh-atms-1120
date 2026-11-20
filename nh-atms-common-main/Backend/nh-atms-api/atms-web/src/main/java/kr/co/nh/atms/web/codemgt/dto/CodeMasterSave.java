package kr.co.nh.atms.web.codemgt.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 코드구분 등록·수정 본문 — API 계약 §11 {@code CodeMasterSave}. 수정(PUT)은 codeSect 를 경로로 받고 본문 codeSect 는 무시.
 * 검사는 CodeMgtRules 한 곳(VAL_001 필드명).
 */
@Getter
@Setter
public class CodeMasterSave {
    private String codeSect;
    private String codeSectNm;
    private String desc1;
    private String desc2;
    private String desc3;
    private String desc4;
    private String desc5;
    private String taskSect;
    private String prcSect;
    private String usedYn;
}
