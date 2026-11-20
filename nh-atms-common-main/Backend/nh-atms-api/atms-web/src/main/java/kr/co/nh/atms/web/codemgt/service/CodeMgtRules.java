package kr.co.nh.atms.web.codemgt.service;

import java.util.Set;

/**
 * 코드관리 규칙 한 곳 — 길이는 정본 컬럼 크기 그대로(TB_CODE_MASTER·TB_CODE_DTL, docs/DB_TOBE_TABLES.md).
 * <ul>
 *   <li>코드구분·코드: 1~10자, 공백 없음. 대소문자는 그대로 둔다(기존 코드값을 바꾸지 않음)</li>
 *   <li>업무구분 10바이트·처리구분 1자: 값 목록이 없어 직접 입력, 길이만 검사(<b>잠정 Q-28</b>)</li>
 *   <li>데이터4 NUMBER(4) = -9999~9999, 데이터5 CHAR(1) = 한 글자</li>
 *   <li>상세코드가 있는 코드구분은 삭제 금지(물리 FK 없음, CDM_002) — 사용 안 하면 사용여부 N</li>
 * </ul>
 */
public final class CodeMgtRules {
    private CodeMgtRules() {}

    public static final int KEY_MAX_LEN = 10;
    public static final int SECT_NM_MAX_BYTES = 100;
    public static final int DESC_MAX_BYTES = 100;
    public static final int TASK_SECT_MAX_BYTES = 10;
    public static final int PRC_SECT_MAX_LEN = 1;
    public static final int CODE_NM_MAX_BYTES = 80;
    public static final int DATA_MAX_BYTES = 80;
    public static final int DATA4_MIN = -9999;
    public static final int DATA4_MAX = 9999;
    public static final int DATA5_MAX_LEN = 1;
    public static final int MAX_DELETE = 100;
    public static final Set<String> YN = Set.of("Y", "N");

    /** 코드구분·코드 모양: 1~10자, 공백 없음 */
    public static boolean validKey(String v) {
        return v != null && !v.isEmpty() && v.length() <= KEY_MAX_LEN && v.chars().noneMatch(Character::isWhitespace);
    }

    public static boolean validData4(Integer v) {
        return v == null || (v >= DATA4_MIN && v <= DATA4_MAX);
    }
}
