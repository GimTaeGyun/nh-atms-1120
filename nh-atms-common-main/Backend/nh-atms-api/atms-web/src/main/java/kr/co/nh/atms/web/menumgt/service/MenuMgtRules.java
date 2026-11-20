package kr.co.nh.atms.web.menumgt.service;

import kr.co.nh.atms.common.exception.BusinessException;

import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 메뉴관리 규칙 모음 — 메뉴ID 채번 규칙은 여기 한 곳(바뀌면 여기와 프론트 MenuMgtParts.previewMenuId 만 고친다).
 * <p>
 * <b>메뉴ID = 영문 대문자 3 + 중메뉴 번호 2 + 소메뉴 번호 2</b> (7자, MENU_ID VARCHAR2(10)) — 2026-11-12 사용자 결정.
 * <ul>
 *   <li>대메뉴 {@code SYS0000} — 상단 메뉴, G(그룹), 상위 '0'. 접두어 3자는 사람이 정한다(겹치면 안 됨)</li>
 *   <li>중메뉴 {@code SYS0300} — 좌측 메뉴, G(그룹), 상위 = 대메뉴</li>
 *   <li>소메뉴 {@code SYS0312} — 실제 화면, M(화면, 메뉴경로 필수), 상위 = 중메뉴</li>
 * </ul>
 * 번호는 같은 상위 안의 가장 큰 번호 + 1(지운 번호는 다시 쓰지 않음, 01~99). 보이는 순서는 SORT_NO 로 따로 정한다.
 * 레벨은 새 컬럼 없이 메뉴ID 모양으로 판단한다.
 */
public final class MenuMgtRules {
    private MenuMgtRules() {}

    /** 대메뉴의 상위메뉴ID (최상위 값, 잠정 Q-05) */
    public static final String ROOT_PARENT = "0";

    /** 대메뉴 접두어 — 영문 대문자 3자 */
    public static final Pattern PREFIX = Pattern.compile("^[A-Z]{3}$");
    /** 대메뉴ID SSS0000 */
    public static final Pattern TOP_ID = Pattern.compile("^[A-Z]{3}0000$");
    /** 중메뉴ID SSS0100~SSS9900 */
    public static final Pattern MID_ID = Pattern.compile("^[A-Z]{3}(?!00)\\d{2}00$");
    /** 소메뉴ID SSS0101~SSS9999 */
    public static final Pattern LEAF_ID = Pattern.compile("^[A-Z]{3}(?!00)\\d{2}(?!00)\\d{2}$");

    /** 메뉴경로 '/{대분류}/{kebab 이름}' — 화면키 규칙(MenuAuthService.menuPathOf)으로 만든 모양 */
    public static final Pattern PATH = Pattern.compile("^/[a-z][a-z0-9]*/[a-z0-9]+(-[a-z0-9]+)*$");

    /** 번호 최댓값(2자리) */
    public static final int MAX_NO = 99;
    /** 메뉴명 MENU_NM VARCHAR2(100) — 바이트(잠정 Q-07, DbBytes) */
    public static final int MENU_NM_MAX_BYTES = 100;
    /** 메뉴경로 MENU_PATH VARCHAR2(300) */
    public static final int MENU_PATH_MAX_LEN = 300;
    /** 메뉴ID MENU_ID VARCHAR2(10) */
    public static final int MENU_ID_MAX_LEN = 10;
    /** 순서 SORT_NO NUMBER(5) */
    public static final int SORT_NO_MAX = 99999;
    /** 한 번에 지울 수 있는 메뉴 수 */
    public static final int MAX_DELETE = 100;

    /** 사용여부 허용 값 */
    public static final Set<String> YN = Set.of("Y", "N");

    /** 레벨 1 대·2 중·3 소, 규칙 밖 ID(예전 6자리 등)는 null */
    public static Integer levelOf(String menuId) {
        if (menuId == null) return null;
        if (TOP_ID.matcher(menuId).matches()) return 1;
        if (MID_ID.matcher(menuId).matches()) return 2;
        if (LEAF_ID.matcher(menuId).matches()) return 3;
        return null;
    }

    /** 레벨의 메뉴타입 — 소메뉴만 화면(M), 대·중은 그룹(G) */
    public static String menuTypeOf(int level) {
        return level == 3 ? "M" : "G";
    }

    /** 대메뉴ID — 접두어 3자 + 0000. 접두어 형식이 틀리면 400 VAL_001 prefix */
    public static String topId(String prefix) {
        if (prefix == null || !PREFIX.matcher(prefix).matches()) throw new BusinessException("VAL_001", "prefix");
        return prefix + "0000";
    }

    /**
     * 중·소메뉴의 다음 메뉴ID — 같은 상위 안 번호의 MAX + 1.
     *
     * @param level    2 중메뉴 · 3 소메뉴
     * @param parentId 중메뉴면 대메뉴ID(SSS0000), 소메뉴면 중메뉴ID(SSS0100). 모양이 아니면 400 MNU_007
     * @param siblings 그 상위의 지금 하위 메뉴ID 들(규칙 밖 ID 는 세지 않는다)
     * @return 다음 메뉴ID. 99 를 넘으면 400 MNU_006
     */
    public static String nextId(int level, String parentId, Collection<String> siblings) {
        if (level == 2) {
            if (parentId == null || !TOP_ID.matcher(parentId).matches()) throw new BusinessException("MNU_007", parentId);
            String head = parentId.substring(0, 3);
            int max = siblings.stream().filter(id -> id != null && MID_ID.matcher(id).matches() && id.startsWith(head))
                    .mapToInt(id -> Integer.parseInt(id.substring(3, 5))).max().orElse(0);
            if (max >= MAX_NO) throw new BusinessException("MNU_006", parentId);
            return head + two(max + 1) + "00";
        }
        if (level == 3) {
            if (parentId == null || !MID_ID.matcher(parentId).matches()) throw new BusinessException("MNU_007", parentId);
            String head = parentId.substring(0, 5);
            int max = siblings.stream().filter(id -> id != null && LEAF_ID.matcher(id).matches() && id.startsWith(head))
                    .mapToInt(id -> Integer.parseInt(id.substring(5, 7))).max().orElse(0);
            if (max >= MAX_NO) throw new BusinessException("MNU_006", parentId);
            return head + two(max + 1);
        }
        throw new BusinessException("VAL_001", "menuLevel");
    }

    private static String two(int n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }
}
