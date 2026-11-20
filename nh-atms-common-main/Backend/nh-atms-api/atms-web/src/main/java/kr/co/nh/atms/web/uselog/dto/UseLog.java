package kr.co.nh.atms.web.uselog.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 사용기록 한 행 — API 계약 §12 {@code UseLog}. TB_SCR_CUD_HIST ⋈ TB_INSA(성명) ⋈ TB_OFC_REG_MGT(사무소명) ⋈ TB_SCR_MENU(메뉴명), 모두 LEFT JOIN.
 * IP·권한·오류코드는 컬럼이 없어 PARAMS 머리에서 꺼낸다(서비스, CrudLogRules.headOf).
 */
@Getter
@Setter
@ToString
public class UseLog {
    /** 이력번호 HIST_ID — 19자리라 JSON 숫자로는 잘린다 → 글자로 */
    private String histId;
    /** 일시 CRT_DTM YYYYMMDDHH24MISS */
    private String crtDtm;
    private String deptCode;
    private String ofcCode;
    /** 사무소명 TB_OFC_REG_MGT.KANM */
    private String kanm;
    private String perNo;
    /** 성명 TB_INSA.NAME (인사에 없으면 null) */
    private String name;
    private String menuId;
    /** 메뉴명 TB_SCR_MENU.MENU_NM (메뉴를 못 찾으면 null) */
    private String menuNm;
    /** R조회 C등록 U수정 D삭제 E엑셀 */
    private String cudType;
    private String succYn;
    private String uri;
    /** PARAMS 머리 ip= */
    private String ip;
    /** PARAMS 머리 auth= (기록 당시 권한코드) */
    private String auth;
    /** PARAMS 머리 err= (실패 때 오류코드) */
    private String err;
    /** 파라미터 원문 — 상세에서만(목록은 null) */
    private String params;
    /** 실행 SQL — 상세에서만(목록은 null) */
    private String sqlText;
}
