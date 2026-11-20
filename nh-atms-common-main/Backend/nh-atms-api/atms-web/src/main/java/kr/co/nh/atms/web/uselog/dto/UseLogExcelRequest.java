package kr.co.nh.atms.web.uselog.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** 엑셀 기록 요청 — API 계약 §12. 공통 downloadExcel 이 내려받기 직전에 보낸다(화면키는 X-Screen-Key 헤더) */
@Getter
@Setter
@ToString
public class UseLogExcelRequest {
    /** 엑셀 파일 이름(날짜 붙이기 전) */
    private String fileName;
    /** 내려받은 행 수 */
    private Integer rowCount;
}
