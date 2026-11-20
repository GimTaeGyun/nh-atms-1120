package kr.co.nh.atms.web.notice.dto;

import kr.co.nh.atms.common.audit.AuditFields;
import lombok.Getter;
import lombok.Setter;

/**
 * 공지사항 1건 — API 계약 §3 {@code Notice extends Audit}. TB_NOTICE ⟕ TB_OFC_REG_MGT(KANM).
 * 필드 = 정본 컬럼 camelCase(MyBatis map-underscore-to-camel-case). 목록 응답에서는 {@code text} 가 null(SELECT 에서 뺀다).
 * 등록·수정 때 매퍼 파라미터로도 쓴다 — {@link AuditFields} 라서 감사 4컬럼은 AuditInterceptor 가 채운다.
 */
@Getter
@Setter
public class Notice extends AuditFields {
    /** 번호 NO NUMBER(22) PK — 채번 NoticeRules.nextNo(잠정 Q-02) */
    private Long no;
    /** 사무소코드 OFC_CODE CHAR(6) — null 이면 전체 공지(모든 사무소에 보임, 잠정 Q-24) */
    private String ofcCode;
    /** 사무소명 TB_OFC_REG_MGT.KANM (조회 전용) */
    private String kanm;
    /** 제목 SUBJECT VARCHAR2(200) */
    private String subject;
    /** 내용 TEXT VARCHAR2(4000) — 목록에서는 null */
    private String text;
    /** 글방문횟수 VISIT NUMBER(22) */
    private Long visit;
    /** 등록자이름 NAME VARCHAR2(10) */
    private String name;
    /** 개인번호 PER_NO VARCHAR2(9) */
    private String perNo;
    /** 작성일자 — DB 는 WRT_DATE DATE, SQL 이 TO_CHAR(WRT_DATE,'YYYYMMDD') 로 내린다 */
    private String wrtDate;

    /* ---- 상세(GET /notice/{no}) 에서만 채움, 목록은 null — 내가 볼 수 있는 공지 안에서 번호 바로 앞·뒤 ---- */
    /** 이전 글 번호(이 글보다 번호가 작은 것 중 가장 큰 것). 없으면 null */
    private Long prevNo;
    /** 이전 글 제목 */
    private String prevSubject;
    /** 다음 글 번호(이 글보다 번호가 큰 것 중 가장 작은 것). 없으면 null */
    private Long nextNo;
    /** 다음 글 제목 */
    private String nextSubject;
}
