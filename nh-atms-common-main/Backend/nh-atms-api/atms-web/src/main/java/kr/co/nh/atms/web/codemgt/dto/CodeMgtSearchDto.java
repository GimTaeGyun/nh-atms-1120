package kr.co.nh.atms.web.codemgt.dto;

import lombok.Getter;
import lombok.Setter;

/** 코드구분 목록 조회조건 — API 계약 §11 {@code CodeMgtSearch}. 수백 건 이하라 페이징 없음 */
@Getter
@Setter
public class CodeMgtSearchDto {
    /** 코드구분·코드명 부분일치(코드구분은 대소문자 무시) */
    private String q;
    /** 사용여부 Y/N */
    private String usedYn;
}
