package kr.co.nh.atms.web.authmgt.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 권한코드 1행(메뉴 권한 탭 왼쪽 목록) — API 계약 §8 {@code AuthCode}.
 * 목록 = TB_SCR_AUTH ∪ TB_SCR_AUTH_MENU 에 있는 AUTH (+ 설정 코드구분의 TB_CO_CODE 코드, 잠정 Q-20).
 */
@Getter
@Setter
public class AuthCode {
    private String auth;
    /** 권한명(설정 코드구분이 없으면 null) */
    private String authNm;
    /** 이 권한을 가진 사용자 행 수(TB_SCR_AUTH) */
    private Integer userCount;
    /** 이 권한에 등록된 메뉴 수(TB_SCR_AUTH_MENU) */
    private Integer menuCount;
}
