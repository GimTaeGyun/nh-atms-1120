package kr.co.nh.atms.web.authmgt.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.authmgt.dto.AuthCode;
import kr.co.nh.atms.web.authmgt.dto.AuthDept;
import kr.co.nh.atms.web.authmgt.dto.AuthDeleteResult;
import kr.co.nh.atms.web.authmgt.dto.AuthMenu;
import kr.co.nh.atms.web.authmgt.dto.AuthMenuSaveResult;
import kr.co.nh.atms.web.authmgt.dto.AuthUser;
import kr.co.nh.atms.web.authmgt.dto.AuthUserDeleteRequest;
import kr.co.nh.atms.web.authmgt.dto.AuthUserKey;
import kr.co.nh.atms.web.authmgt.dto.AuthUserSave;
import kr.co.nh.atms.web.authmgt.dto.AuthUserSearchDto;
import kr.co.nh.atms.web.authmgt.mapper.AuthMgtMapper;
import kr.co.nh.atms.web.common.BatchChanges;
import kr.co.nh.atms.web.common.CurrentUser;
import kr.co.nh.atms.web.common.Dates;
import kr.co.nh.atms.web.common.DbBytes;
import kr.co.nh.atms.web.common.NotFoundException;
import kr.co.nh.atms.web.common.OfficeCode;
import kr.co.nh.atms.web.common.auth.CurrentUserLoader;
import kr.co.nh.atms.web.common.auth.MenuAuthService;
import kr.co.nh.atms.web.common.auth.Perm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 권한관리(sample.authMgt) — API 계약 §8. AS-IS "시스템 사용권한부여"(사용자 권한 탭) + 메뉴별 권한 탭.
 * <ul>
 *   <li>사용자 권한(TB_SCR_AUTH): 팝업형 CRUD(공지사항 견본 모양). 키 = 부서+사무소+개인번호, 수정은 권한·승인상태·만료일시·메모만.
 *       PER_NO = '1' 은 부서권한, 그 밖은 개인권한(2026-11-11 사용자 결정) — 화면은 탭으로 나눠 kind=DEPT/PERSON 으로 조회.
 *       등록 때 키는 서버가 TB_OFC_RAW 로 채운다: 부서권한 = 고른 부서의 SAMUSO·ONLINE_CODE, 개인권한 = 그 사람 소속(TB_INSA.SAMUSO) 부서의 SAMUSO·ONLINE_CODE</li>
 *   <li>메뉴 권한(TB_SCR_AUTH_MENU): 권한코드 1개 × 메뉴 전체 표를 직접 편집 → 일괄 저장(기기모델코드 견본 모양).
 *       updated = 행이 있으면 고침(UPD), 없으면 넣음(REG) — 들어온 종류만 {@link MenuAuthService#check}.
 *       행 삭제는 없다(2026-11-11 사용자 결정): 사용 해제 = USE_YN 'N' 으로 고침. 행이 없는 메뉴는 화면에 모두 미체크(사용 'N')로 보인다</li>
 *   <li>쓰기가 끝나면 {@link CurrentUserLoader#evictAll()} — 고정 사용자 자신의 권한을 바꿨을 때 1분 캐시를 기다리지 않게</li>
 * </ul>
 * 오류: 입력 400 VAL_001(필드명)·VAL_003(사무소 형식), 인사정보 없음 400 E404, 없음 404 ATH_001·ATH_003, 중복 400 ATH_002,
 * 부서 없음 400 ATH_004, 부서의 사무소코드 없음 400 ATH_005, 사람의 소속 부서 없음 400 ATH_006.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class AuthMgtService {

    /** 화면키(screens/screenIds.ts) — 권한 검사의 메뉴 경로 원천 */
    public static final String SCREEN = "sample.authMgt";

    private final AuthMgtMapper mapper;
    private final MenuAuthService menuAuth;
    private final CurrentUserLoader userLoader;
    private final String authCodeType;

    public AuthMgtService(AuthMgtMapper mapper,
                          MenuAuthService menuAuth,
                          CurrentUserLoader userLoader,
                          @Value("${atms.auth-mgt.auth-code-type:}") String authCodeTypeSetting) {
        this.mapper = mapper;
        this.menuAuth = menuAuth;
        this.userLoader = userLoader;
        this.authCodeType = authCodeTypeOf(authCodeTypeSetting);
    }

    /**
     * 권한명 원천 코드구분.
     * <p>
     * <b>잠정 Q-20</b>: AS-IS 코드관리 화면에 코드구분 {@code AUTHGRP}(사용권한)가 보이지만 TO-BE {@code TB_CODE_DTL}(2026-11-16, 예전 TB_CO_CODE) 에 있는지 모른다.
     * 설정 {@code atms.auth-mgt.auth-code-type} 이 있으면 그 코드구분의 {@code CODE_NM}, 없으면(기본) 권한명 null → 화면은 코드만 보인다.
     */
    public static String authCodeTypeOf(String setting) {
        return trimToNull(setting);
    }

    /* ======================= 사용자 권한 ======================= */

    /** 목록(페이징). 건수가 0 이면 목록 SQL 은 부르지 않는다 */
    public PageResult<AuthUser> searchUsers(AuthUserSearchDto s) {
        normalize(s);
        long total = mapper.countUserList(s);
        List<AuthUser> rows = total == 0 ? List.of() : mapper.selectUserList(s, authCodeType);
        return PageResult.of(rows, total, s);
    }

    /** 1건. 없으면 404 ATH_001 */
    public AuthUser getUser(String deptCode, String ofcCode, String perNo) {
        return found(key(deptCode, ofcCode, perNo));
    }

    /**
     * 등록 — 키는 서버가 TB_OFC_RAW 로 채운다(본문의 부서·사무소는 부서권한의 부서코드만 쓴다).
     * 부서권한(perNo '1'): 부서 없음 ATH_004, 부서의 ONLINE_CODE 없음 ATH_005.
     * 개인권한: 인사정보 없음 E404, 소속 부서 없음 ATH_006. 같은 키가 있으면 400 ATH_002. 등록일시 = 지금, 등록자 = 고정 사용자
     */
    @Transactional
    public AuthUser createUser(AuthUserSave req) {
        if (req == null) throw new BusinessException("VAL_004");
        AuthUserKey k = newKey(req);
        if (mapper.countUserByKey(k) > 0) throw new BusinessException("ATH_002", keyText(k));
        AuthUser row = toRow(k, req);
        row.setInsDate(Dates.now14());
        row.setInsId(CurrentUser.perNo());
        try {
            mapper.insertUser(row);
        } catch (DuplicateKeyException e) {
            // 세는 시점과 넣는 시점 사이에 같은 키가 생긴 경우 — 500 대신 계약대로 400
            throw new BusinessException("ATH_002", keyText(k));
        }
        userLoader.evictAll();
        log.info("[권한관리] 사용자 권한 등록 key={} auth={} user={}", keyText(k), row.getAuth(), CurrentUser.id());
        return found(k);
    }

    /** 부서 목록(TB_OFC_RAW) — 부서권한 등록 팝업 콤보. q = 부서코드·부서명 부분일치, 최대 200건 */
    public List<AuthDept> depts(String q) {
        return mapper.selectDepts(trimToNull(q), AuthMgtRules.DEPT_LIMIT);
    }

    /** 그 사람의 소속 부서(개인권한 등록 팝업에 보여 줌). 인사정보·부서가 없으면 400 ATH_006 */
    public AuthDept personDept(String perNo) {
        String p = trimToNull(perNo);
        if (p == null || p.length() > AuthMgtRules.PER_NO_MAX_LEN) throw new BusinessException("VAL_001", "perNo");
        AuthDept d = mapper.selectPersonDept(p);
        if (d == null) throw new BusinessException("ATH_006", p);
        return d;
    }

    /** 수정 — 권한·승인상태·만료일시·메모만(키는 주소에서). 없으면 404 ATH_001 */
    @Transactional
    public AuthUser updateUser(String deptCode, String ofcCode, String perNo, AuthUserSave req) {
        if (req == null) throw new BusinessException("VAL_004");
        AuthUserKey k = key(deptCode, ofcCode, perNo);
        AuthUser row = toRow(k, req);
        if (mapper.updateUser(row) == 0) throw new NotFoundException("ATH_001", keyText(k));
        userLoader.evictAll();
        log.info("[권한관리] 사용자 권한 수정 key={} auth={} user={}", keyText(k), row.getAuth(), CurrentUser.id());
        return found(k);
    }

    /** 다건 물리 삭제 — 1~100건(중복은 하나로), 아니면 400 VAL_001. 하나도 못 지우면 404 ATH_001 */
    @Transactional
    public AuthDeleteResult deleteUsers(AuthUserDeleteRequest req) {
        List<AuthUserKey> raw = req == null ? null : req.getKeys();
        if (raw == null || raw.isEmpty() || raw.stream().anyMatch(Objects::isNull)) throw new BusinessException("VAL_001", "keys");
        Map<String, AuthUserKey> uniq = new LinkedHashMap<>();
        for (AuthUserKey r : raw) {
            AuthUserKey k = key(r.getDeptCode(), r.getOfcCode(), r.getPerNo());
            uniq.put(keyText(k), k);
        }
        if (uniq.size() > AuthMgtRules.MAX_DELETE) throw new BusinessException("VAL_001", "keys");
        int count = mapper.deleteUsers(new ArrayList<>(uniq.values()));
        if (count == 0) throw new NotFoundException("ATH_001", String.join(", ", uniq.keySet()));
        userLoader.evictAll();
        log.info("[권한관리] 사용자 권한 삭제 keys={} count={} user={}", uniq.keySet(), count, CurrentUser.id());
        return new AuthDeleteResult(count);
    }

    /* ======================= 권한코드 · 메뉴 권한 ======================= */

    /** 권한코드 목록(메뉴 권한 탭 왼쪽) */
    public List<AuthCode> authCodes() {
        return mapper.selectAuthCodes(authCodeType);
    }

    /** 이 권한의 메뉴 전체(트리 순서, 깊이 붙음). auth 1~2자 필수 */
    public List<AuthMenu> authMenus(String auth) {
        String a = authOf(auth);
        List<AuthMenu> rows = mapper.selectAuthMenus(a);
        rows.forEach(r -> r.setAuth(a));
        return flattenTree(rows);
    }

    /**
     * 메뉴 권한 일괄 저장 — 입력 검사 → 권한(있는 종류만) → 저장(PK AUTH+MENU_ID 로 있으면 고침, 없으면 넣음), 한 트랜잭션.
     * added·deleted 는 받지 않는다(메뉴 행은 서버가 다 내려 준다 — 표에서 고치면 updated, 사용 해제도 updated). 응답 S009.
     * 사용(USE_YN)을 안 켠 채 다른 권한만 켜도 그대로 'N' 으로 저장한다(사용자 결정).
     */
    @Transactional
    public AuthMenuSaveResult saveAuthMenus(BatchChanges<AuthMenu> ch) {
        if (ch == null) throw new BusinessException("VAL_004");
        if (ch.getAdded() != null && !ch.getAdded().isEmpty()) throw new BusinessException("VAL_001", "added");
        if (ch.getDeleted() != null && !ch.getDeleted().isEmpty()) throw new BusinessException("VAL_001", "deleted");
        List<AuthMenu> updated = nz(ch.getUpdated());
        updated.forEach(AuthMgtService::cleanMenu);

        Set<String> seen = new HashSet<>();
        for (AuthMenu r : updated) {
            if (!seen.add(menuKeyText(r))) throw new BusinessException("VAL_001", "updated");
        }

        // 고칠 행 / 새로 넣을 행을 나눠 종류별 권한을 본다(새로 넣음 REG · 고침 UPD)
        List<AuthMenu> toUpdate = new ArrayList<>();
        List<AuthMenu> toInsert = new ArrayList<>();
        for (AuthMenu r : updated) {
            if (mapper.countAuthMenu(r) > 0) toUpdate.add(r);
            else toInsert.add(r);
        }
        if (!toInsert.isEmpty()) menuAuth.check(SCREEN, Perm.REG);
        if (!toUpdate.isEmpty()) menuAuth.check(SCREEN, Perm.UPD);

        for (AuthMenu r : toUpdate) {
            if (mapper.updateAuthMenu(r) == 0) throw new NotFoundException("ATH_003", menuKeyText(r));
        }
        for (AuthMenu r : toInsert) {
            if (mapper.countMenu(r.getMenuId()) == 0) throw new NotFoundException("ATH_003", menuKeyText(r));
            try {
                mapper.insertAuthMenu(r);
            } catch (DuplicateKeyException e) {
                mapper.updateAuthMenu(r); // 동시에 다른 사람이 먼저 넣었으면 고침으로
            }
        }
        log.info("[권한관리] 메뉴 권한 저장 넣음 {} 고침 {} user={}", toInsert.size(), toUpdate.size(), CurrentUser.id());
        return new AuthMenuSaveResult(toInsert.size(), toUpdate.size());
    }

    /**
     * 메뉴 행을 트리 순서(부모 다음 자식, 형제는 SORT_NO·MENU_ID 순)로 펴고 깊이(lvl, 1부터)를 붙인다.
     * 부모가 목록에 없는 행(최상위 '0' 등, 잠정 Q-05)은 맨 위 단계. 순환이 있어도 한 번씩만 넣는다.
     */
    public static List<AuthMenu> flattenTree(List<AuthMenu> rows) {
        if (rows == null || rows.isEmpty()) return List.of();
        Map<String, AuthMenu> byId = new HashMap<>();
        for (AuthMenu r : rows) byId.put(r.getMenuId(), r);
        Map<String, List<AuthMenu>> children = new HashMap<>();
        List<AuthMenu> roots = new ArrayList<>();
        for (AuthMenu r : rows) {
            String p = r.getParentMenuId();
            if (p == null || p.equals(r.getMenuId()) || !byId.containsKey(p)) roots.add(r);
            else children.computeIfAbsent(p, x -> new ArrayList<>()).add(r);
        }
        Comparator<AuthMenu> order = Comparator.comparing((AuthMenu r) -> r.getSortNo() == null ? Integer.MAX_VALUE : r.getSortNo())
                .thenComparing(r -> Objects.toString(r.getMenuId(), ""));
        List<AuthMenu> out = new ArrayList<>(rows.size());
        Set<String> done = new HashSet<>();
        roots.sort(order);
        for (AuthMenu r : roots) walk(r, 1, children, order, done, out);
        // 순환 때문에 어느 뿌리에도 닿지 않은 행 — 빠뜨리지 않고 맨 끝에
        for (AuthMenu r : rows) {
            if (!done.contains(r.getMenuId())) walk(r, 1, children, order, done, out);
        }
        return out;
    }

    private static void walk(AuthMenu r, int lvl, Map<String, List<AuthMenu>> children, Comparator<AuthMenu> order, Set<String> done, List<AuthMenu> out) {
        if (!done.add(r.getMenuId())) return;
        r.setLvl(lvl);
        out.add(r);
        List<AuthMenu> kids = children.get(r.getMenuId());
        if (kids == null) return;
        kids.sort(order);
        for (AuthMenu c : kids) walk(c, lvl + 1, children, order, done, out);
    }

    /* ======================= 내부 ======================= */

    private AuthUser found(AuthUserKey k) {
        AuthUser u = mapper.selectUser(k, authCodeType);
        if (u == null) throw new NotFoundException("ATH_001", keyText(k));
        return u;
    }

    /** 오류 문구 인자·로그용 키 글 '부서-사무소-개인번호' (예 000080-000080-1234567) */
    static String keyText(AuthUserKey k) {
        return k.getDeptCode() + "-" + k.getOfcCode() + "-" + k.getPerNo();
    }

    /** 오류 문구 인자 '권한-메뉴ID' (예 A-SMP002) */
    static String menuKeyText(AuthMenu r) {
        return r.getAuth() + "-" + r.getMenuId();
    }

    /** 부서권한 행인가(PER_NO = '1') */
    public static boolean isDeptAuth(String perNo) {
        return AuthMgtRules.DEPT_PER_NO.equals(trimToNull(perNo));
    }

    /** 등록 키 — 부서권한은 고른 부서의 TB_OFC_RAW, 개인권한은 그 사람 소속 부서의 TB_OFC_RAW 에서 부서·사무소를 채운다 */
    private AuthUserKey newKey(AuthUserSave req) {
        AuthDept d;
        String perNo;
        if (isDeptAuth(req.getPerNo())) {
            perNo = AuthMgtRules.DEPT_PER_NO;
            String dept = code6(req.getDeptCode(), "deptCode");
            if (dept == null) throw new BusinessException("VAL_001", "deptCode");
            d = mapper.selectDept(dept);
            if (d == null) throw new BusinessException("ATH_004", dept);
        } else {
            perNo = trimToNull(req.getPerNo());
            if (perNo == null || perNo.length() > AuthMgtRules.PER_NO_MAX_LEN) throw new BusinessException("VAL_001", "perNo");
            if (mapper.countInsa(perNo) == 0) throw new BusinessException("E404");
            d = mapper.selectPersonDept(perNo);
            if (d == null) throw new BusinessException("ATH_006", perNo);
        }
        if (trimToNull(d.getOnlineCode()) == null) throw new BusinessException("ATH_005", d.getSamuso());
        return key(d.getSamuso(), d.getOnlineCode(), perNo);
    }

    /** 키 3개 필수 — 부서·사무소는 숫자 1~6자리를 6자리로(형식이 틀리면 VAL_003), 개인번호 9자 이내 */
    private static AuthUserKey key(String deptCode, String ofcCode, String perNo) {
        String d = code6(deptCode, "deptCode");
        String o = code6(ofcCode, "ofcCode");
        String p = trimToNull(perNo);
        if (d == null) throw new BusinessException("VAL_001", "deptCode");
        if (o == null) throw new BusinessException("VAL_001", "ofcCode");
        if (p == null || p.length() > AuthMgtRules.PER_NO_MAX_LEN) throw new BusinessException("VAL_001", "perNo");
        return new AuthUserKey(d, o, p);
    }

    /** 숫자 1~6자리 → 6자리(사무소코드와 같은 규칙 OfficeCode.norm). 비면 null, 형식이 틀리면 400 VAL_003(필드명) */
    private static String code6(String v, String field) {
        try {
            return OfficeCode.norm(v);
        } catch (BusinessException e) {
            throw new BusinessException("VAL_003", field);
        }
    }

    /** 본문 검사·정리 → 매퍼 파라미터(키 포함) */
    private static AuthUser toRow(AuthUserKey k, AuthUserSave req) {
        String apv = trimToNull(req.getApvSts());
        apv = apv == null ? null : apv.toUpperCase(Locale.ROOT);
        if (apv == null || !AuthMgtRules.APV_STS_VALUES.contains(apv)) throw new BusinessException("VAL_001", "apvSts");
        String due = trimToNull(req.getDueDate());
        if (due != null && !AuthMgtRules.isDt14(due)) throw new BusinessException("VAL_001", "dueDate");
        String memo = trimToNull(req.getMemo());
        if (DbBytes.over(memo, AuthMgtRules.MEMO_MAX_BYTES)) throw new BusinessException("VAL_001", "memo");
        AuthUser row = new AuthUser();
        row.setDeptCode(k.getDeptCode());
        row.setOfcCode(k.getOfcCode());
        row.setPerNo(k.getPerNo());
        row.setAuth(authOf(req.getAuth()));
        row.setApvSts(apv);
        row.setDueDate(due);
        row.setMemo(memo);
        return row;
    }

    /** 권한 1~2자 필수 (VAL_001 auth) */
    private static String authOf(String auth) {
        String a = trimToNull(auth);
        if (a == null || a.length() > AuthMgtRules.AUTH_MAX_LEN) throw new BusinessException("VAL_001", "auth");
        return a;
    }

    /** 메뉴 권한 1행 검사 — 권한·메뉴ID 필수, 플래그 6개(조회·등록·수정·삭제·실행·사용)가 Y/N. 읽기 전용 칸은 지운다 */
    private static void cleanMenu(AuthMenu r) {
        if (r == null) throw new BusinessException("VAL_001", "row");
        r.setAuth(authOf(r.getAuth()));
        String id = trimToNull(r.getMenuId());
        if (id == null || id.length() > AuthMgtRules.MENU_ID_MAX_LEN) throw new BusinessException("VAL_001", "menuId");
        r.setMenuId(id);
        r.setQryRolYn(yn(r.getQryRolYn(), "qryRolYn"));
        r.setRegRolYn(yn(r.getRegRolYn(), "regRolYn"));
        r.setUpdRolYn(yn(r.getUpdRolYn(), "updRolYn"));
        r.setDelRolYn(yn(r.getDelRolYn(), "delRolYn"));
        r.setExeRolYn(yn(r.getExeRolYn(), "exeRolYn"));
        r.setUseYn(yn(r.getUseYn(), "useYn"));
        r.setParentMenuId(null);
        r.setMenuNm(null);
        r.setMenuType(null);
        r.setMenuPath(null);
        r.setSortNo(null);
        r.setMenuUseYn(null);
        r.setMappedYn(null);
        r.setLvl(null);
    }

    private static String yn(String v, String field) {
        String t = trimToNull(v);
        t = t == null ? null : t.toUpperCase(Locale.ROOT);
        if (t == null || !AuthMgtRules.YN.contains(t)) throw new BusinessException("VAL_001", field);
        return t;
    }

    /** 조회조건 정리 — 구분 DEPT/PERSON(그 밖 VAL_003), 사무소 6자리(VAL_003), 승인상태 대문자, 빈 값 null */
    static void normalize(AuthUserSearchDto s) {
        String kind = trimToNull(s.getKind());
        kind = kind == null ? null : kind.toUpperCase(Locale.ROOT);
        if (kind != null && !AuthMgtRules.KIND_DEPT.equals(kind) && !AuthMgtRules.KIND_PERSON.equals(kind)) throw new BusinessException("VAL_003", "kind");
        s.setKind(kind);
        s.setOfcCode(OfficeCode.norm(s.getOfcCode()));
        s.setAuth(trimToNull(s.getAuth()));
        String apv = trimToNull(s.getApvSts());
        s.setApvSts(apv == null ? null : apv.toUpperCase(Locale.ROOT));
        s.setQ(trimToNull(s.getQ()));
    }

    private static <T> List<T> nz(List<T> l) {
        return l == null ? List.of() : l;
    }

    private static String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
