package kr.co.nh.atms.web.menumgt.dto;

import kr.co.nh.atms.common.audit.AuditFields;
import lombok.Getter;
import lombok.Setter;

/**
 * 메뉴 1행 — API 계약 §9 {@code MenuMgtRow extends Audit}. TB_SCR_MENU 컬럼 + 서버가 붙이는 값(레벨·하위 수·상위 경로).
 * 목록·등록·수정 응답, 등록·수정 SQL 파라미터로 함께 쓴다(감사 4컬럼은 AuditInterceptor 가 채운다).
 */
@Getter
@Setter
public class MenuMgtRow extends AuditFields {
    /** 메뉴ID MENU_ID VARCHAR2(10) — 영문3 + 중메뉴 2자리 + 소메뉴 2자리 (예 SYS0312) */
    private String menuId;
    /** 상위메뉴ID PARENT_MENU_ID — 대메뉴는 '0' */
    private String parentMenuId;
    /** 메뉴명 MENU_NM VARCHAR2(100) */
    private String menuNm;
    /** 메뉴타입 MENU_TYPE CHAR(1) — G 그룹(대·중메뉴) / M 화면(소메뉴) */
    private String menuType;
    /** 메뉴경로 MENU_PATH VARCHAR2(300) — 화면(M)만 '/sample/menu-mgt' */
    private String menuPath;
    /** 메뉴순서 SORT_NO NUMBER(5) — 같은 상위 안에서 보이는 순서(메뉴ID 번호와 따로) */
    private Integer sortNo;
    /** 사용여부 USE_YN CHAR(1) */
    private String useYn;
    /** 레벨 1 대·2 중·3 소 — 메뉴ID 모양으로 서버가 계산(MenuMgtRules.levelOf). 규칙 밖 ID 는 null */
    private Integer menuLevel;
    /** 바로 아래 메뉴 수 (목록 응답만) */
    private Integer childCount;
    /** 상위 메뉴 이름 경로 '시스템관리 › 공통코드' (대메뉴는 null, 목록 응답만) */
    private String parentPath;
}
