package kr.co.nh.atms.web.menumgt.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.web.common.CurrentUser;
import kr.co.nh.atms.web.common.DbBytes;
import kr.co.nh.atms.web.common.NotFoundException;
import kr.co.nh.atms.web.menumgt.dto.MenuMgtDeleteRequest;
import kr.co.nh.atms.web.menumgt.dto.MenuMgtDeleteResult;
import kr.co.nh.atms.web.menumgt.dto.MenuMgtRow;
import kr.co.nh.atms.web.menumgt.dto.MenuMgtSave;
import kr.co.nh.atms.web.menumgt.dto.MenuMgtSearchDto;
import kr.co.nh.atms.web.menumgt.mapper.MenuMgtMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 메뉴관리(sample.menuMgt) — API 계약 §9. 목록 + 팝업형 CRUD(공지사항 견본 모양), TB_SCR_MENU.
 * <ul>
 *   <li>메뉴ID 는 서버가 정한다 — 대메뉴 = 접두어 3자 + 0000, 중·소메뉴 = 같은 상위 안 번호 MAX + 1 ({@link MenuMgtRules}, 2026-11-12 사용자 결정)</li>
 *   <li>수정은 메뉴명·메뉴경로(화면만)·순서·사용여부만 — 상위·레벨은 메뉴ID(PK)에 들어 있어 못 바꾼다</li>
 *   <li>삭제는 물리 삭제 + 그 메뉴의 TB_SCR_AUTH_MENU 행도 함께(2026-11-12 사용자 결정). 하위가 남으면 400 MNU_001</li>
 * </ul>
 * 오류: 입력 400 VAL_001(필드명), 없음 404 MNU_004, 접두어 겹침 400 MNU_005, 번호 다 씀 400 MNU_006,
 * 상위가 규칙 밖 400 MNU_007, 메뉴경로 겹침 400 MNU_008, 동시 등록 400 MNU_009.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuMgtService {

    /** 화면키(screens/screenIds.ts) — 권한 검사의 메뉴 경로 원천 */
    public static final String SCREEN = "sample.menuMgt";

    private final MenuMgtMapper mapper;

    /** 목록 — 트리 순서(부모 다음 자식, 형제는 SORT_NO·MENU_ID 순) 전체에 레벨·하위 수·상위 경로를 붙이고 조회조건으로 거른다 */
    public List<MenuMgtRow> search(MenuMgtSearchDto s) {
        List<MenuMgtRow> all = tree(mapper.selectAll());
        if (s == null) return all;
        String top = trimToNull(s.getTopMenuId());
        String use = upper(s.getUseYn());
        String q = trimToNull(s.getQ());
        String ql = q == null ? null : q.toLowerCase(Locale.ROOT);
        Map<String, MenuMgtRow> byId = new HashMap<>();
        for (MenuMgtRow r : all) byId.put(r.getMenuId(), r);
        List<MenuMgtRow> out = new ArrayList<>();
        for (MenuMgtRow r : all) {
            if (top != null && !top.equals(topOf(r, byId))) continue;
            if (s.getMenuLevel() != null && !s.getMenuLevel().equals(r.getMenuLevel())) continue;
            if (use != null && !use.equals(r.getUseYn())) continue;
            if (ql != null && !contains(r.getMenuId(), ql) && !contains(r.getMenuNm(), ql) && !contains(r.getMenuPath(), ql)) continue;
            out.add(r);
        }
        return out;
    }

    /**
     * 등록 — 레벨별로 상위를 확인하고 메뉴ID 를 정한 뒤 넣는다. 응답 S002.
     * 대메뉴: 접두어 영문 3자(겹치면 MNU_005), 상위 '0'. 중메뉴: 상위 = 대메뉴(SSS0000). 소메뉴: 상위 = 중메뉴(SSS0100), 메뉴경로 필수.
     */
    @Transactional
    public MenuMgtRow create(MenuMgtSave req) {
        if (req == null) throw new BusinessException("VAL_004");
        Integer level = req.getMenuLevel();
        if (level == null || level < 1 || level > 3) throw new BusinessException("VAL_001", "menuLevel");

        MenuMgtRow row = new MenuMgtRow();
        if (level == 1) {
            String prefix = upper(req.getPrefix());
            String id = MenuMgtRules.topId(prefix);
            if (mapper.countIdPrefix(prefix) > 0) throw new BusinessException("MNU_005", prefix);
            row.setMenuId(id);
            row.setParentMenuId(MenuMgtRules.ROOT_PARENT);
        } else {
            String parent = trimToNull(req.getParentMenuId());
            if (parent == null) throw new BusinessException("VAL_001", "parentMenuId");
            if (mapper.selectMenu(parent) == null) throw new BusinessException("MNU_004", parent);
            row.setMenuId(MenuMgtRules.nextId(level, parent, mapper.selectChildIds(parent)));
            row.setParentMenuId(parent);
        }
        row.setMenuType(MenuMgtRules.menuTypeOf(level));
        fill(row, req, level == 3, null);
        try {
            mapper.insertMenu(row);
        } catch (DuplicateKeyException e) {
            // 번호를 센 뒤 넣기 전에 다른 사람이 같은 번호를 넣은 경우 — 500 대신 다시 저장하라고 알린다
            throw new BusinessException("MNU_009", row.getMenuId());
        }
        log.info("[메뉴관리] 등록 id={} parent={} name={} user={}", row.getMenuId(), row.getParentMenuId(), row.getMenuNm(), CurrentUser.id());
        return found(row.getMenuId());
    }

    /** 수정 — 메뉴명·메뉴경로(화면 M 만)·순서·사용여부. 응답 S003. 없으면 404 MNU_004 */
    @Transactional
    public MenuMgtRow update(String menuId, MenuMgtSave req) {
        if (req == null) throw new BusinessException("VAL_004");
        MenuMgtRow cur = found(idOf(menuId));
        MenuMgtRow row = new MenuMgtRow();
        row.setMenuId(cur.getMenuId());
        fill(row, req, "M".equals(cur.getMenuType()), cur.getMenuId());
        if (mapper.updateMenu(row) == 0) throw new NotFoundException("MNU_004", cur.getMenuId());
        log.info("[메뉴관리] 수정 id={} name={} use={} user={}", row.getMenuId(), row.getMenuNm(), row.getUseYn(), CurrentUser.id());
        return found(row.getMenuId());
    }

    /**
     * 다건 물리 삭제 — 1~100건(중복은 하나로). 지울 목록 밖에 하위가 남으면 400 MNU_001(그 메뉴ID).
     * 그 메뉴들의 TB_SCR_AUTH_MENU 행을 먼저 지우고 메뉴를 지운다(한 트랜잭션). 하나도 못 지우면 404 MNU_004. 응답 S005
     */
    @Transactional
    public MenuMgtDeleteResult delete(MenuMgtDeleteRequest req) {
        List<String> raw = req == null ? null : req.getMenuIds();
        if (raw == null || raw.isEmpty() || raw.stream().anyMatch(Objects::isNull)) throw new BusinessException("VAL_001", "menuIds");
        Set<String> ids = new LinkedHashSet<>();
        for (String r : raw) ids.add(idOf(r));
        if (ids.size() > MenuMgtRules.MAX_DELETE) throw new BusinessException("VAL_001", "menuIds");
        for (String id : ids) {
            for (String child : mapper.selectChildIds(id)) {
                if (!ids.contains(child)) throw new BusinessException("MNU_001", id);
            }
        }
        List<String> list = new ArrayList<>(ids);
        int authCount = mapper.deleteAuthMenus(list);
        int count = mapper.deleteMenus(list);
        if (count == 0) throw new NotFoundException("MNU_004", String.join(", ", ids));
        log.info("[메뉴관리] 삭제 ids={} count={} authRows={} user={}", ids, count, authCount, CurrentUser.id());
        return new MenuMgtDeleteResult(count, authCount);
    }

    /**
     * 메뉴 행을 트리 순서로 펴고 레벨(메뉴ID 모양)·하위 수·상위 이름 경로를 붙인다.
     * 부모가 목록에 없는 행(최상위 '0' 등)은 맨 위 단계. 순환이 있어도 한 번씩만 넣는다.
     */
    public static List<MenuMgtRow> tree(List<MenuMgtRow> rows) {
        if (rows == null || rows.isEmpty()) return List.of();
        Map<String, MenuMgtRow> byId = new HashMap<>();
        for (MenuMgtRow r : rows) byId.put(r.getMenuId(), r);
        Map<String, List<MenuMgtRow>> children = new HashMap<>();
        List<MenuMgtRow> roots = new ArrayList<>();
        for (MenuMgtRow r : rows) {
            String p = r.getParentMenuId();
            if (p == null || p.equals(r.getMenuId()) || !byId.containsKey(p)) roots.add(r);
            else children.computeIfAbsent(p, x -> new ArrayList<>()).add(r);
        }
        Comparator<MenuMgtRow> order = Comparator.comparing((MenuMgtRow r) -> r.getSortNo() == null ? Integer.MAX_VALUE : r.getSortNo())
                .thenComparing(r -> Objects.toString(r.getMenuId(), ""));
        roots.sort(order);
        List<MenuMgtRow> out = new ArrayList<>(rows.size());
        Set<String> done = new HashSet<>();
        for (MenuMgtRow r : roots) walk(r, null, children, order, done, out);
        for (MenuMgtRow r : rows) {
            if (!done.contains(r.getMenuId())) walk(r, null, children, order, done, out);
        }
        return out;
    }

    private static void walk(MenuMgtRow r, String parentPath, Map<String, List<MenuMgtRow>> children, Comparator<MenuMgtRow> order, Set<String> done, List<MenuMgtRow> out) {
        if (!done.add(r.getMenuId())) return;
        List<MenuMgtRow> kids = children.getOrDefault(r.getMenuId(), List.of());
        r.setMenuLevel(MenuMgtRules.levelOf(r.getMenuId()));
        r.setChildCount(kids.size());
        r.setParentPath(parentPath);
        out.add(r);
        String path = parentPath == null ? r.getMenuNm() : parentPath + " › " + r.getMenuNm();
        List<MenuMgtRow> sorted = new ArrayList<>(kids);
        sorted.sort(order);
        for (MenuMgtRow c : sorted) walk(c, path, children, order, done, out);
    }

    /* ======================= 내부 ======================= */

    private MenuMgtRow found(String menuId) {
        MenuMgtRow r = mapper.selectMenu(menuId);
        if (r == null) throw new NotFoundException("MNU_004", menuId);
        r.setMenuLevel(MenuMgtRules.levelOf(r.getMenuId()));
        return r;
    }

    /** 본문 검사 → 메뉴명·메뉴경로·순서·사용여부를 row 에. screen = 화면(M)이라 경로 필수, 아니면 경로 null */
    private void fill(MenuMgtRow row, MenuMgtSave req, boolean screen, String excludeId) {
        String nm = trimToNull(req.getMenuNm());
        if (nm == null || DbBytes.over(nm, MenuMgtRules.MENU_NM_MAX_BYTES)) throw new BusinessException("VAL_001", "menuNm");
        String path = null;
        if (screen) {
            path = trimToNull(req.getMenuPath());
            if (path == null || path.length() > MenuMgtRules.MENU_PATH_MAX_LEN || !MenuMgtRules.PATH.matcher(path).matches()) {
                throw new BusinessException("VAL_001", "menuPath");
            }
            if (mapper.countPath(path, excludeId) > 0) throw new BusinessException("MNU_008", path);
        }
        Integer sort = req.getSortNo();
        if (sort == null || sort < 0 || sort > MenuMgtRules.SORT_NO_MAX) throw new BusinessException("VAL_001", "sortNo");
        String use = upper(req.getUseYn());
        if (use == null || !MenuMgtRules.YN.contains(use)) throw new BusinessException("VAL_001", "useYn");
        row.setMenuNm(nm);
        row.setMenuPath(path);
        row.setSortNo(sort);
        row.setUseYn(use);
    }

    /** 메뉴ID 필수·10자 이내 (VAL_001 menuId) */
    private static String idOf(String menuId) {
        String id = trimToNull(menuId);
        if (id == null || id.length() > MenuMgtRules.MENU_ID_MAX_LEN) throw new BusinessException("VAL_001", "menuId");
        return id;
    }

    /** 맨 위 조상 메뉴ID(대메뉴) */
    private static String topOf(MenuMgtRow r, Map<String, MenuMgtRow> byId) {
        MenuMgtRow cur = r;
        Set<String> seen = new HashSet<>();
        while (seen.add(cur.getMenuId())) {
            MenuMgtRow p = cur.getParentMenuId() == null ? null : byId.get(cur.getParentMenuId());
            if (p == null) return cur.getMenuId();
            cur = p;
        }
        return cur.getMenuId();
    }

    private static boolean contains(String v, String ql) {
        return v != null && v.toLowerCase(Locale.ROOT).contains(ql);
    }

    private static String upper(String v) {
        String t = trimToNull(v);
        return t == null ? null : t.toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
