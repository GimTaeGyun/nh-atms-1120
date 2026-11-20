package kr.co.nh.atms.web.codemgt.dto;

import lombok.Getter;
import lombok.Setter;

/** 상세코드 등록·수정 본문 — API 계약 §11 {@code CodeDetailSave}. 코드구분은 경로, 수정은 code 도 경로(본문 code 무시) */
@Getter
@Setter
public class CodeDetailSave {
    private String code;
    private String codeNm;
    private String data1;
    private String data2;
    private String data3;
    private Integer data4;
    private String data5;
}
