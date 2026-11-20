package kr.co.nh.atms.web.menumgt;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.web.menumgt.service.MenuMgtRules;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 메뉴ID 규칙 — 영문3 + 중 2 + 소 2, 레벨 판단, 번호 MAX+1(지운 번호 재사용 안 함), 99 넘침, 상위 모양 검사 */
class MenuMgtRulesTest {

    private static String code(Throwable t) {
        return ((BusinessException) t).getCode();
    }

    @Test
    void 레벨은_메뉴ID_모양으로() {
        assertThat(MenuMgtRules.levelOf("SYS0000")).isEqualTo(1);
        assertThat(MenuMgtRules.levelOf("SYS0300")).isEqualTo(2);
        assertThat(MenuMgtRules.levelOf("SYS0312")).isEqualTo(3);
        assertThat(MenuMgtRules.levelOf("SYS0012")).isNull(); // 중메뉴 번호 00 + 소메뉴 번호 → 규칙 밖
        assertThat(MenuMgtRules.levelOf("SMP001")).isNull();  // 예전 6자리
        assertThat(MenuMgtRules.levelOf(null)).isNull();
        assertThat(MenuMgtRules.menuTypeOf(1)).isEqualTo("G");
        assertThat(MenuMgtRules.menuTypeOf(2)).isEqualTo("G");
        assertThat(MenuMgtRules.menuTypeOf(3)).isEqualTo("M");
    }

    @Test
    void 대메뉴ID_는_접두어_더하기_0000() {
        assertThat(MenuMgtRules.topId("SYS")).isEqualTo("SYS0000");
        assertThatThrownBy(() -> MenuMgtRules.topId("SY")).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));
        assertThatThrownBy(() -> MenuMgtRules.topId("sys")).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));
        assertThatThrownBy(() -> MenuMgtRules.topId("S1S")).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));
    }

    @Test
    void 중메뉴_번호는_MAX_더하기_1() {
        assertThat(MenuMgtRules.nextId(2, "SYS0000", List.of())).isEqualTo("SYS0100");
        // 02 를 지워도 03 다음은 04 (빈 번호 재사용 안 함). 규칙 밖 ID 는 세지 않는다
        assertThat(MenuMgtRules.nextId(2, "SYS0000", List.of("SYS0100", "SYS0300", "SYS999", "ABC0500"))).isEqualTo("SYS0400");
    }

    @Test
    void 소메뉴_번호는_그_중메뉴_안에서_MAX_더하기_1() {
        assertThat(MenuMgtRules.nextId(3, "SYS0300", List.of())).isEqualTo("SYS0301");
        assertThat(MenuMgtRules.nextId(3, "SYS0300", List.of("SYS0301", "SYS0311", "SYS0305"))).isEqualTo("SYS0312");
    }

    @Test
    void 번호_99_를_넘으면_MNU_006() {
        List<String> full = new ArrayList<>();
        for (int i = 1; i <= 99; i++) full.add("SYS03" + (i < 10 ? "0" + i : i));
        assertThatThrownBy(() -> MenuMgtRules.nextId(3, "SYS0300", full)).satisfies(t -> assertThat(code(t)).isEqualTo("MNU_006"));
        assertThatThrownBy(() -> MenuMgtRules.nextId(2, "SYS0000", List.of("SYS9900"))).satisfies(t -> assertThat(code(t)).isEqualTo("MNU_006"));
    }

    @Test
    void 상위_모양이_레벨과_안_맞으면_MNU_007() {
        assertThatThrownBy(() -> MenuMgtRules.nextId(2, "SYS0300", List.of())).satisfies(t -> assertThat(code(t)).isEqualTo("MNU_007"));
        assertThatThrownBy(() -> MenuMgtRules.nextId(3, "SYS0000", List.of())).satisfies(t -> assertThat(code(t)).isEqualTo("MNU_007"));
        assertThatThrownBy(() -> MenuMgtRules.nextId(3, "SMP000", List.of())).satisfies(t -> assertThat(code(t)).isEqualTo("MNU_007"));
        assertThatThrownBy(() -> MenuMgtRules.nextId(1, "0", List.of())).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));
    }

    @Test
    void 메뉴경로_모양() {
        assertThat(MenuMgtRules.PATH.matcher("/sample/menu-mgt").matches()).isTrue();
        assertThat(MenuMgtRules.PATH.matcher("/sys/code2-group").matches()).isTrue();
        assertThat(MenuMgtRules.PATH.matcher("/sample/MenuMgt").matches()).isFalse();
        assertThat(MenuMgtRules.PATH.matcher("/sample/").matches()).isFalse();
        assertThat(MenuMgtRules.PATH.matcher("sample/menu-mgt").matches()).isFalse();
        assertThat(MenuMgtRules.PATH.matcher("/sample/menu--mgt").matches()).isFalse();
    }
}
