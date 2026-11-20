package kr.co.nh.atms.web.authmgt;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.web.authmgt.dto.AuthDept;
import kr.co.nh.atms.web.authmgt.dto.AuthMenu;
import kr.co.nh.atms.web.authmgt.dto.AuthMenuSaveResult;
import kr.co.nh.atms.web.authmgt.dto.AuthUser;
import kr.co.nh.atms.web.authmgt.dto.AuthUserDeleteRequest;
import kr.co.nh.atms.web.authmgt.dto.AuthUserKey;
import kr.co.nh.atms.web.authmgt.dto.AuthUserSave;
import kr.co.nh.atms.web.authmgt.dto.AuthUserSearchDto;
import kr.co.nh.atms.web.authmgt.mapper.AuthMgtMapper;
import kr.co.nh.atms.web.authmgt.service.AuthMgtService;
import kr.co.nh.atms.web.common.BatchChanges;
import kr.co.nh.atms.web.common.ForbiddenException;
import kr.co.nh.atms.web.common.NotFoundException;
import kr.co.nh.atms.web.common.auth.CurrentUserLoader;
import kr.co.nh.atms.web.common.auth.MenuAuthService;
import kr.co.nh.atms.web.common.auth.Perm;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 권한관리 견본 규칙 — 사용자 권한(키 정리·E404·ATH_002·ATH_001·VAL_001·캐시 비우기) / 메뉴 권한(종류별 권한·넣음/고침 나누기·ATH_003·트리 순서)
 */
class AuthMgtServiceTest {

    private static final String SCREEN = "sample.authMgt";

    private final AuthMgtMapper mapper = mock(AuthMgtMapper.class);
    private final MenuAuthService auth = mock(MenuAuthService.class);
    private final CurrentUserLoader loader = mock(CurrentUserLoader.class);
    private final AuthMgtService service = new AuthMgtService(mapper, auth, loader, "");

    private static String code(Throwable t) {
        return ((BusinessException) t).getCode();
    }

    private static Object[] args(Throwable t) {
        return ((BusinessException) t).getArgs();
    }

    private static AuthUserSave save(String dept, String ofc, String perNo, String a, String apv) {
        AuthUserSave s = new AuthUserSave();
        s.setDeptCode(dept);
        s.setOfcCode(ofc);
        s.setPerNo(perNo);
        s.setAuth(a);
        s.setApvSts(apv);
        return s;
    }

    private static AuthDept dept(String samuso, String online) {
        AuthDept d = new AuthDept();
        d.setSamuso(samuso);
        d.setSamusoName("부서" + samuso);
        d.setOnlineCode(online);
        return d;
    }

    private static AuthMenu menu(String a, String id, String flags) {
        AuthMenu m = new AuthMenu();
        m.setAuth(a);
        m.setMenuId(id);
        m.setQryRolYn(flags.substring(0, 1));
        m.setRegRolYn(flags.substring(1, 2));
        m.setUpdRolYn(flags.substring(2, 3));
        m.setDelRolYn(flags.substring(3, 4));
        m.setExeRolYn(flags.substring(4, 5));
        m.setUseYn(flags.substring(5, 6));
        return m;
    }

    private static AuthMenu node(String id, String parent, int sort) {
        AuthMenu m = new AuthMenu();
        m.setMenuId(id);
        m.setParentMenuId(parent);
        m.setSortNo(sort);
        return m;
    }

    /* ---------- 사용자 권한 ---------- */

    @Test
    void searchNormalizesAndSkipsListWhenEmpty() {
        AuthUserSearchDto s = new AuthUserSearchDto();
        s.setOfcCode("80");
        s.setApvSts(" y ");
        s.setAuth(" ");
        s.setQ(" 홍 ");
        s.setKind("dept");
        when(mapper.countUserList(any())).thenReturn(0L);

        assertThat(service.searchUsers(s).getContent()).isEmpty();
        assertThat(s.getOfcCode()).isEqualTo("000080");
        assertThat(s.getApvSts()).isEqualTo("Y");
        assertThat(s.getAuth()).isNull();
        assertThat(s.getQ()).isEqualTo("홍");
        assertThat(s.getKind()).isEqualTo("DEPT");
        AuthUserSearchDto bad = new AuthUserSearchDto();
        bad.setKind("ALL");
        assertThatThrownBy(() -> service.searchUsers(bad)).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_003"));
        verify(mapper, never()).selectUserList(any(), any());
    }

    @Test
    void createPersonTakesKeysFromPersonsOfcRawAndEvictsCache() {
        when(mapper.countInsa("1234567")).thenReturn(1);
        when(mapper.selectPersonDept("1234567")).thenReturn(dept("80", "80"));
        when(mapper.countUserByKey(any())).thenReturn(0);
        when(mapper.selectUser(any(), any())).thenReturn(new AuthUser());

        AuthUserSave s = save("999999", "999999", " 1234567 ", "A", "y"); // 본문 부서·사무소는 무시
        s.setDueDate("99991231235959");
        s.setMemo("  메모  ");
        service.createUser(s);

        ArgumentCaptor<AuthUser> c = ArgumentCaptor.forClass(AuthUser.class);
        verify(mapper).insertUser(c.capture());
        AuthUser row = c.getValue();
        assertThat(row.getDeptCode()).isEqualTo("000080");
        assertThat(row.getOfcCode()).isEqualTo("000080");
        assertThat(row.getPerNo()).isEqualTo("1234567");
        assertThat(row.getApvSts()).isEqualTo("Y");
        assertThat(row.getMemo()).isEqualTo("메모");
        assertThat(row.getInsDate()).hasSize(14);
        assertThat(row.getInsId()).isNotBlank();
        verify(loader).evictAll();
    }

    @Test
    void createDeptUsesPerNoOneAndOnlineCodeOfDept() {
        when(mapper.selectDept("000123")).thenReturn(dept("000123", "000080"));
        when(mapper.countUserByKey(any())).thenReturn(0);
        when(mapper.selectUser(any(), any())).thenReturn(new AuthUser());

        service.createUser(save("123", null, "1", "T", "Y"));

        ArgumentCaptor<AuthUser> c = ArgumentCaptor.forClass(AuthUser.class);
        verify(mapper).insertUser(c.capture());
        assertThat(c.getValue().getDeptCode()).isEqualTo("000123");
        assertThat(c.getValue().getOfcCode()).isEqualTo("000080");
        assertThat(c.getValue().getPerNo()).isEqualTo("1");
        verify(mapper, never()).countInsa(any());
        assertThat(AuthMgtService.isDeptAuth(" 1 ")).isTrue();
        assertThat(AuthMgtService.isDeptAuth("1234567")).isFalse();
    }

    @Test
    void createDeptErrors() {
        assertThatThrownBy(() -> service.createUser(save(null, null, "1", "T", "Y"))).satisfies(t -> assertThat(args(t)).containsExactly("deptCode"));
        assertThatThrownBy(() -> service.createUser(save("777", null, "1", "T", "Y")))
                .satisfies(t -> {
                    assertThat(code(t)).isEqualTo("ATH_004");
                    assertThat(args(t)).containsExactly("000777");
                });
        when(mapper.selectDept("000555")).thenReturn(dept("000555", " "));
        assertThatThrownBy(() -> service.createUser(save("555", null, "1", "T", "Y"))).satisfies(t -> assertThat(code(t)).isEqualTo("ATH_005"));
    }

    @Test
    void createRejectsUnknownPersonNoDeptAndDuplicate() {
        when(mapper.countInsa("9999999")).thenReturn(0);
        assertThatThrownBy(() -> service.createUser(save("80", "80", "9999999", "A", "Y")))
                .satisfies(t -> assertThat(code(t)).isEqualTo("E404"));

        when(mapper.countInsa("1234567")).thenReturn(1);
        assertThatThrownBy(() -> service.createUser(save(null, null, "1234567", "A", "Y")))
                .satisfies(t -> assertThat(code(t)).isEqualTo("ATH_006"));

        when(mapper.selectPersonDept("1234567")).thenReturn(dept("80", "80"));
        when(mapper.countUserByKey(any())).thenReturn(1);
        assertThatThrownBy(() -> service.createUser(save("80", "80", "1234567", "A", "Y")))
                .satisfies(t -> {
                    assertThat(code(t)).isEqualTo("ATH_002");
                    assertThat(args(t)).containsExactly("000080-000080-1234567");
                });
        verify(mapper, never()).insertUser(any());
    }

    @Test
    void inputErrorsAreVal001WithFieldName() {
        assertThatThrownBy(() -> service.createUser(save("12a", null, "1", "A", "Y")))
                .satisfies(t -> {
                    assertThat(code(t)).isEqualTo("VAL_003");
                    assertThat(args(t)).containsExactly("deptCode");
                });
        assertThatThrownBy(() -> service.createUser(save(null, null, " ", "A", "Y"))).satisfies(t -> assertThat(args(t)).containsExactly("perNo"));
        when(mapper.countInsa(any())).thenReturn(1);
        when(mapper.selectPersonDept(any())).thenReturn(dept("80", "80"));
        assertThatThrownBy(() -> service.createUser(save("80", "80", "1234567", "ABC", "Y"))).satisfies(t -> assertThat(args(t)).containsExactly("auth"));
        assertThatThrownBy(() -> service.createUser(save("80", "80", "1234567", "A", "X"))).satisfies(t -> assertThat(args(t)).containsExactly("apvSts"));
        AuthUserSave bad = save("80", "80", "1234567", "A", "Y");
        bad.setDueDate("20261231");
        assertThatThrownBy(() -> service.createUser(bad)).satisfies(t -> assertThat(args(t)).containsExactly("dueDate"));
        AuthUserSave longMemo = save("80", "80", "1234567", "A", "Y");
        longMemo.setMemo("가".repeat(51));
        assertThatThrownBy(() -> service.createUser(longMemo)).satisfies(t -> assertThat(args(t)).containsExactly("memo"));
    }

    @Test
    void updateMissingIsAth001() {
        when(mapper.updateUser(any())).thenReturn(0);
        assertThatThrownBy(() -> service.updateUser("80", "80", "1234567", save(null, null, null, "T", "N")))
                .isInstanceOf(NotFoundException.class)
                .satisfies(t -> assertThat(code(t)).isEqualTo("ATH_001"));
        verify(loader, never()).evictAll();
    }

    @Test
    void deleteDedupesAndChecksRange() {
        AuthUserDeleteRequest empty = new AuthUserDeleteRequest();
        empty.setKeys(List.of());
        assertThatThrownBy(() -> service.deleteUsers(empty)).satisfies(t -> assertThat(args(t)).containsExactly("keys"));

        AuthUserDeleteRequest req = new AuthUserDeleteRequest();
        req.setKeys(List.of(new AuthUserKey("80", "80", "1"), new AuthUserKey("000080", "000080", "1")));
        when(mapper.deleteUsers(any())).thenReturn(1);
        assertThat(service.deleteUsers(req).count()).isEqualTo(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AuthUserKey>> c = ArgumentCaptor.forClass(List.class);
        verify(mapper).deleteUsers(c.capture());
        assertThat(c.getValue()).hasSize(1);

        List<AuthUserKey> many = new ArrayList<>();
        for (int i = 0; i < 101; i++) many.add(new AuthUserKey("80", "80", String.valueOf(i)));
        req.setKeys(many);
        assertThatThrownBy(() -> service.deleteUsers(req)).satisfies(t -> assertThat(args(t)).containsExactly("keys"));
    }

    /* ---------- 메뉴 권한 ---------- */

    @Test
    void saveSplitsInsertAndUpdateAndChecksEachPermission() {
        AuthMenu existing = menu("T", "SMP002", "YYYNNN"); // 사용 해제도 고침(USE_YN 'N', 행은 남음)
        AuthMenu fresh = menu("T", "SMP005", "YNNNNN");    // 사용 안 켜고 조회만 — 그대로 USE_YN 'N' 으로 넣음
        when(mapper.countAuthMenu(existing)).thenReturn(1);
        when(mapper.countAuthMenu(fresh)).thenReturn(0);
        when(mapper.countMenu("SMP005")).thenReturn(1);
        when(mapper.updateAuthMenu(existing)).thenReturn(1);
        BatchChanges<AuthMenu> ch = new BatchChanges<>();
        ch.setUpdated(List.of(existing, fresh));

        AuthMenuSaveResult r = service.saveAuthMenus(ch);

        assertThat(r.inserted()).isEqualTo(1);
        assertThat(r.updated()).isEqualTo(1);
        verify(auth).check(SCREEN, Perm.REG);
        verify(auth).check(SCREEN, Perm.UPD);
        verify(auth, never()).check(SCREEN, Perm.DEL);
        ArgumentCaptor<AuthMenu> c = ArgumentCaptor.forClass(AuthMenu.class);
        verify(mapper).insertAuthMenu(c.capture());
        assertThat(c.getValue().getUseYn()).isEqualTo("N");
        verify(mapper).updateAuthMenu(existing);
        assertThat(existing.getUseYn()).isEqualTo("N");
    }

    @Test
    void saveStopsOnPermissionBeforeWriting() {
        AuthMenu fresh = menu("T", "SMP005", "YNNNNY");
        when(mapper.countAuthMenu(fresh)).thenReturn(0);
        doThrow(new ForbiddenException("SYS_403")).when(auth).check(SCREEN, Perm.REG);
        BatchChanges<AuthMenu> ch = new BatchChanges<>();
        ch.setUpdated(List.of(fresh));
        assertThatThrownBy(() -> service.saveAuthMenus(ch)).isInstanceOf(ForbiddenException.class);
        verify(mapper, never()).insertAuthMenu(any());
    }

    @Test
    void saveRejectsAddedDeletedBadRowsAndMissingMenu() {
        BatchChanges<AuthMenu> added = new BatchChanges<>();
        added.setAdded(List.of(menu("T", "SMP002", "YYYYYY")));
        assertThatThrownBy(() -> service.saveAuthMenus(added)).satisfies(t -> assertThat(args(t)).containsExactly("added"));

        BatchChanges<AuthMenu> deleted = new BatchChanges<>();
        deleted.setDeleted(List.of(menu("T", "SMP002", "NNNNNN")));
        assertThatThrownBy(() -> service.saveAuthMenus(deleted)).satisfies(t -> assertThat(args(t)).containsExactly("deleted"));

        BatchChanges<AuthMenu> badFlag = new BatchChanges<>();
        badFlag.setUpdated(List.of(menu("T", "SMP002", "YXYYYY")));
        assertThatThrownBy(() -> service.saveAuthMenus(badFlag)).satisfies(t -> assertThat(args(t)).containsExactly("regRolYn"));
        verifyNoInteractions(auth);

        AuthMenu noMenu = menu("T", "XXX999", "YNNNNY");
        when(mapper.countAuthMenu(noMenu)).thenReturn(0);
        when(mapper.countMenu("XXX999")).thenReturn(0);
        BatchChanges<AuthMenu> ins = new BatchChanges<>();
        ins.setUpdated(List.of(noMenu));
        assertThatThrownBy(() -> service.saveAuthMenus(ins))
                .satisfies(t -> {
                    assertThat(code(t)).isEqualTo("ATH_003");
                    assertThat(args(t)).containsExactly("T-XXX999");
                });
    }

    @Test
    void authMenusRequiresAuthAndSetsIt() {
        assertThatThrownBy(() -> service.authMenus(" ")).satisfies(t -> assertThat(args(t)).containsExactly("auth"));
        when(mapper.selectAuthMenus(eq("T"))).thenReturn(new ArrayList<>(List.of(node("SMP000", "0", 900))));
        assertThat(service.authMenus("T")).singleElement().satisfies(m -> {
            assertThat(m.getAuth()).isEqualTo("T");
            assertThat(m.getLvl()).isEqualTo(1);
        });
    }

    @Test
    void flattenTreeOrdersParentThenChildrenBySort() {
        List<AuthMenu> rows = new ArrayList<>(List.of(
                node("SMP002", "SMP000", 2),
                node("SYS000", "0", 100),
                node("SMP001", "SMP000", 1),
                node("SMP000", "0", 900),
                node("SYS001", "SYS000", 1)));
        List<AuthMenu> out = AuthMgtService.flattenTree(rows);
        assertThat(out).extracting(AuthMenu::getMenuId).containsExactly("SYS000", "SYS001", "SMP000", "SMP001", "SMP002");
        assertThat(out).extracting(AuthMenu::getLvl).containsExactly(1, 2, 1, 2, 2);
    }

    @Test
    void flattenTreeKeepsCycles() {
        List<AuthMenu> rows = new ArrayList<>(List.of(node("A", "B", 1), node("B", "A", 2)));
        assertThat(AuthMgtService.flattenTree(rows)).hasSize(2);
    }
}
