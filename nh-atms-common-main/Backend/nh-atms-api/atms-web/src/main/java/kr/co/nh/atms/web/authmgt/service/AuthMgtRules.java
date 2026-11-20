package kr.co.nh.atms.web.authmgt.service;

import java.util.Set;

/**
 * 권한관리 견본 규칙 모음 — 잠정 규칙은 여기 한 곳(정본·현업 답이 오면 여기만 고친다).
 */
public final class AuthMgtRules {
    private AuthMgtRules() {}

    /** 부서권한 표시 개인번호 — TB_SCR_AUTH.PER_NO = '1' (2026-11-11 사용자 결정, 로그인 판단 CurrentUserLoader 와 같은 값) */
    public static final String DEPT_PER_NO = kr.co.nh.atms.web.common.auth.CurrentUserLoader.DEPT_PER_NO;

    /** 조회조건 구분 값 */
    public static final String KIND_DEPT = "DEPT";
    public static final String KIND_PERSON = "PERSON";

    /** 부서 콤보 최대 건수 */
    public static final int DEPT_LIMIT = 200;

    /** 권한 AUTH VARCHAR2(2) — 1~2자 */
    public static final int AUTH_MAX_LEN = 2;

    /** 개인번호 PER_NO VARCHAR2(9) */
    public static final int PER_NO_MAX_LEN = 9;

    /** 메뉴ID MENU_ID VARCHAR2(10) */
    public static final int MENU_ID_MAX_LEN = 10;

    /** 메모 MEMO VARCHAR2(100) — 바이트 계산은 공통 DbBytes(잠정 Q-07) */
    public static final int MEMO_MAX_BYTES = 100;

    /** 한 번에 지울 수 있는 사용자 권한 행 수 */
    public static final int MAX_DELETE = 100;

    /**
     * 승인상태 APV_STS CHAR(2) 허용 값.
     * <p>
     * <b>잠정 Q-18</b>: 정본에 코드표가 없다. 고정 사용자 채택 규칙(CurrentUserLoader.pickAuth)이 'Y' 를 승인으로 보므로 'Y'(승인)·'N'(미승인) 두 값만 받는다.
     */
    public static final Set<String> APV_STS_VALUES = Set.of("Y", "N");

    /** 플래그(조회·등록·수정·삭제·실행·사용) 허용 값 */
    public static final Set<String> YN = Set.of("Y", "N");

    /** 만료일시 형식 YYYYMMDDHH24MISS(14자리 숫자). 비우면 무기한 */
    public static boolean isDt14(String v) {
        return v != null && v.matches("\\d{14}");
    }
}
