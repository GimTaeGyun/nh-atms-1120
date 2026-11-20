package kr.co.nh.atms.web.menu;

import kr.co.nh.atms.web.menu.dto.MenuNode;
import kr.co.nh.atms.web.menu.service.MenuService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 내 메뉴 트리 — 조회권한 없는 화면·빈 그룹 숨김, 관리자 전부 Y, 3단계 가지치기 (2026-11-17 반입 전 점검) */
class MenuServiceTest {

    private static MenuNode node(String id, String parent, String type, int sort, String qry) {
        MenuNode n = new MenuNode();
        n.setMenuId(id);
        n.setParentMenuId(parent);
        n.setMenuNm(id);
        n.setMenuType(type);
        n.setMenuPath("M".equals(type) ? "/sample/" + id : null);
        n.setSortNo(sort);
        n.setUseYn("Y");
        n.setQryRolYn(qry);
        n.setRegRolYn("N");
        n.setUpdRolYn("N");
        n.setDelRolYn("N");
        n.setExeRolYn("N");
        return n;
    }

    private static List<MenuNode> rows() {
        return List.of(
                node("G1", "ROOT", "G", 1, "N"),
                node("G2", "ROOT", "G", 2, "N"),
                node("notice", "G1", "M", 1, "Y"),
                node("mcn-info", "G1", "M", 2, "N"),
                node("mcn-model", "G2", "M", 1, "N"));
    }

    @Test
    void hidesScreensWithoutQueryAndEmptyGroups() {
        List<MenuNode> tree = MenuService.buildTree(rows(), false);
        assertThat(tree).extracting(MenuNode::getMenuId).containsExactly("G1");
        assertThat(tree.get(0).getChildren()).extracting(MenuNode::getMenuId).containsExactly("notice");
    }

    @Test
    void adminSeesAllWithAllFlags() {
        List<MenuNode> tree = MenuService.buildTree(rows(), true);
        assertThat(tree).extracting(MenuNode::getMenuId).containsExactly("G1", "G2");
        MenuNode model = tree.get(1).getChildren().get(0);
        assertThat(model.getRegRolYn()).isEqualTo("Y");
        assertThat(model.getExeRolYn()).isEqualTo("Y");
    }

    /** 대 → 중 → 소 3단계: 소메뉴 조회권한이 하나도 없으면 그 중메뉴·대메뉴까지 숨김. 플래그는 메뉴 권한 그대로 */
    @Test
    void threeLevelTreeHidesWholeBranchWithoutQuery() {
        MenuNode leafOn = node("SMP0101", "SMP0100", "M", 1, "Y");
        leafOn.setRegRolYn("Y");
        List<MenuNode> rows = List.of(
                node("SMP0000", "0", "G", 1, "N"),
                node("SMP0100", "SMP0000", "G", 1, "N"),
                leafOn,
                node("SMP0102", "SMP0100", "M", 2, "N"),
                node("SYS0000", "0", "G", 2, "N"),
                node("SYS0100", "SYS0000", "G", 1, "N"),
                node("SYS0101", "SYS0100", "M", 1, null)); // 메뉴 권한 행이 없음(SQL 이 NVL 'N' — 여기서는 null 도 숨김)
        List<MenuNode> tree = MenuService.buildTree(rows, false);
        assertThat(tree).extracting(MenuNode::getMenuId).containsExactly("SMP0000");
        MenuNode mid = tree.get(0).getChildren().get(0);
        assertThat(mid.getChildren()).extracting(MenuNode::getMenuId).containsExactly("SMP0101");
        assertThat(mid.getChildren().get(0).getRegRolYn()).isEqualTo("Y");
        assertThat(mid.getChildren().get(0).getUpdRolYn()).isEqualTo("N");
    }
}
