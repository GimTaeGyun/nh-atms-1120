package kr.co.nh.atms.web.codemgt.dto;

import kr.co.nh.atms.common.audit.AuditFields;
import lombok.Getter;
import lombok.Setter;

/**
 * 코드구분 1행 — API 계약 §11 {@code CodeMaster extends Audit}. TB_CODE_MASTER + 상세 건수.
 * 목록·상세·등록·수정 응답, 등록·수정 SQL 파라미터로 함께 쓴다(감사 4컬럼은 AuditInterceptor 가 채운다).
 */
@Getter
@Setter
public class CodeMaster extends AuditFields {
    /** 코드구분 CODE_SECT VARCHAR2(10) PK (2026-11-16 사용자 확인: 마스터·상세 모두 10자리) */
    private String codeSect;
    /** 코드구분명(화면 '코드명') CODE_SECT_NM VARCHAR2(100) */
    private String codeSectNm;
    /** 설명1~5 DESC1~5 VARCHAR2(100) — 상세 표 칸 제목과 연결하지 않는다(2026-11-16 사용자 결정: 데이터1~5 고정) */
    private String desc1;
    private String desc2;
    private String desc3;
    private String desc4;
    private String desc5;
    /** 업무구분 TASK_SECT VARCHAR2(10) — 값 목록 미정, 직접 입력(잠정 Q-28) */
    private String taskSect;
    /** 처리구분 PRC_SECT CHAR(1) — 값 목록 미정, 직접 입력(잠정 Q-28) */
    private String prcSect;
    /** 사용여부 USED_YN CHAR(1) Y/N */
    private String usedYn;
    /** 상세코드 수(목록·상세 응답만) */
    private Integer detailCount;
}
