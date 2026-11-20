package kr.co.nh.atms.web.menumgt.dto;

/** 메뉴 다건 삭제 응답 {@code { count, authCount }} — 지운 메뉴 수, 함께 지운 권한별 메뉴(TB_SCR_AUTH_MENU) 행 수. API 계약 §9 */
public record MenuMgtDeleteResult(int count, int authCount) {}
