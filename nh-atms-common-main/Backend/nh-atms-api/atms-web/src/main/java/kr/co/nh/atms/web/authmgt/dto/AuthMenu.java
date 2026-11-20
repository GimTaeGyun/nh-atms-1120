package kr.co.nh.atms.web.authmgt.dto;

import kr.co.nh.atms.common.audit.AuditFields;
import lombok.Getter;
import lombok.Setter;

/**
 * 메뉴 권한 1행(권한코드 1개 × 메뉴 전체) — API 계약 §8 {@code AuthMenu extends Audit}.
 * TB_SCR_MENU ⟕ TB_SCR_AUTH_MENU(AUTH = 고른 권한). 키 = AUTH + MENU_ID.
 * 아직 등록되지 않은 메뉴(mappedYn='N')는 플래그·useYn 모두 'N'(화면에 미체크). 체크 후 저장하면 PK(AUTH+MENU_ID)로 새로 들어간다.
 * 일괄 저장 때 매퍼 파라미터로도 쓴다 — 감사 4컬럼은 AuditInterceptor 가 채운다.
 */
@Getter
@Setter
public class AuthMenu extends AuditFields {
    /** 권한 AUTH VARCHAR2(2) */
    private String auth;
    /** 메뉴ID MENU_ID VARCHAR2(10) */
    private String menuId;
    /** 상위메뉴ID (읽기 전용) */
    private String parentMenuId;
    /** 메뉴명 (읽기 전용) */
    private String menuNm;
    /** 메뉴타입 G 그룹 / M 화면 (읽기 전용) */
    private String menuType;
    /** 메뉴경로 (읽기 전용) */
    private String menuPath;
    /** 메뉴순서 (읽기 전용) */
    private Integer sortNo;
    /** 메뉴 사용여부 TB_SCR_MENU.USE_YN (읽기 전용) */
    private String menuUseYn;
    /** 트리 깊이 1부터 — 서버가 계산(AuthMgtService.flattenTree) */
    private Integer lvl;
    /** TB_SCR_AUTH_MENU 에 행이 있으면 'Y' (읽기 전용) */
    private String mappedYn;
    private String qryRolYn;
    private String regRolYn;
    private String updRolYn;
    private String delRolYn;
    private String exeRolYn;
    /** TB_SCR_AUTH_MENU.USE_YN */
    private String useYn;
}
