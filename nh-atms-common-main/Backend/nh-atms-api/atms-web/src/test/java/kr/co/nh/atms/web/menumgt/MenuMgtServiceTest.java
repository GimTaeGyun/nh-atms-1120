package kr.co.nh.atms.web.menumgt;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.web.common.NotFoundException;
import kr.co.nh.atms.web.menumgt.dto.MenuMgtDeleteRequest;
import kr.co.nh.atms.web.menumgt.dto.MenuMgtDeleteResult;
import kr.co.nh.atms.web.menumgt.dto.MenuMgtRow;
import kr.co.nh.atms.web.menumgt.dto.MenuMgtSave;
import kr.co.nh.atms.web.menumgt.dto.MenuMgtSearchDto;
import kr.co.nh.atms.web.menumgt.mapper.MenuMgtMapper;
import kr.co.nh.atms.web.menumgt.service.MenuMgtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 메뉴관리 — 목록(트리 순서·레벨·거르기), 등록(레벨별 채번·검사·MNU_005/008/009), 수정(그룹은 경로 없음), 삭제(하위 검사·권한행 함께) */
class MenuMgtServiceTest {

    private final MenuMgtMapper mapper = mock(MenuMgtMapper.class);
    private final MenuMgtService service = new MenuMgtService(mapper);

    private static String code(Throwable t) {
        return ((BusinessException) t).getCode();
    }

    private static MenuMgtRow row(String id, String parent, String nm, String type, int sort, String use) {
        MenuMgtRow r = new MenuMgtRow();
        r.setMenuId(id);
        r.setParentMenuId(parent);
        r.setMenuNm(nm);
        r.setMenuType(type);
        r.setSortNo(sort);
        r.setUseYn(use);
        return r;
    }

    private static MenuMgtSave save(Integer level, String parent, String prefix, String nm, String path) {
        MenuMgtSave s = new MenuMgtSave();
        s.setMenuLevel(level);
        s.setParentMenuId(parent);
        s.setPrefix(prefix);
        s.setMenuNm(nm);
        s.setMenuPath(path);
        s.setSortNo(1);
        s.setUseYn("Y");
        return s;
    }

    private List<MenuMgtRow> sample() {
        return new ArrayList<>(List.of(
                row("SYS0000", "0", "시스템관리", "G", 900, "Y"),
                row("SMP0000", "0", "견본", "G", 950, "Y"),
                row("SYS0300", "SYS0000", "공통코드", "G", 2, "Y"),
                row("SYS0100", "SYS0000", "사용자", "G", 1, "N"),
                row("SYS0312", "SYS0300", "은행코드", "M", 2, "Y"),
                row("SYS0301", "SYS0300", "공통코드 그룹", "M", 1, "Y")));
    }

    @Test
    void 목록은_트리_순서에_레벨_하위수_상위경로() {
        when(mapper.selectAll()).thenReturn(sample());
        List<MenuMgtRow> out = service.search(new MenuMgtSearchDto());
        assertThat(out).extracting(MenuMgtRow::getMenuId).containsExactly("SYS0000", "SYS0100", "SYS0300", "SYS0301", "SYS0312", "SMP0000");
        assertThat(out).extracting(MenuMgtRow::getMenuLevel).containsExactly(1, 2, 2, 3, 3, 1);
        assertThat(out.get(0).getChildCount()).isEqualTo(2);
        assertThat(out.get(4).getParentPath()).isEqualTo("시스템관리 › 공통코드");
        assertThat(out.get(0).getParentPath()).isNull();
    }

    @Test
    void 목록_거르기_대메뉴_레벨_사용_검색어() {
        when(mapper.selectAll()).thenReturn(sample());
        MenuMgtSearchDto s = new MenuMgtSearchDto();
        s.setTopMenuId("SYS0000");
        s.setMenuLevel(3);
        assertThat(service.search(s)).extracting(MenuMgtRow::getMenuId).containsExactly("SYS0301", "SYS0312");

        when(mapper.selectAll()).thenReturn(sample());
        MenuMgtSearchDto u = new MenuMgtSearchDto();
        u.setUseYn("n");
        assertThat(service.search(u)).extracting(MenuMgtRow::getMenuId).containsExactly("SYS0100");

        when(mapper.selectAll()).thenReturn(sample());
        MenuMgtSearchDto q = new MenuMgtSearchDto();
        q.setQ("smp");
        assertThat(service.search(q)).extracting(MenuMgtRow::getMenuId).containsExactly("SMP0000");
    }

    @Test
    void 소메뉴_등록은_그_중메뉴_안에서_MAX_더하기_1_화면M() {
        when(mapper.selectMenu("SYS0300")).thenReturn(row("SYS0300", "SYS0000", "공통코드", "G", 2, "Y"));
        when(mapper.selectChildIds("SYS0300")).thenReturn(List.of("SYS0301", "SYS0312"));
        when(mapper.selectMenu("SYS0313")).thenReturn(row("SYS0313", "SYS0300", "외화코드", "M", 1, "Y"));
        MenuMgtRow r = service.create(save(3, "SYS0300", null, " 외화코드 ", "/sys/fx-code"));

        ArgumentCaptor<MenuMgtRow> cap = ArgumentCaptor.forClass(MenuMgtRow.class);
        verify(mapper).insertMenu(cap.capture());
        assertThat(cap.getValue().getMenuId()).isEqualTo("SYS0313");
        assertThat(cap.getValue().getParentMenuId()).isEqualTo("SYS0300");
        assertThat(cap.getValue().getMenuType()).isEqualTo("M");
        assertThat(cap.getValue().getMenuNm()).isEqualTo("외화코드");
        assertThat(cap.getValue().getMenuPath()).isEqualTo("/sys/fx-code");
        assertThat(r.getMenuLevel()).isEqualTo(3);
    }

    @Test
    void 대메뉴_등록은_접두어_대문자_그룹G_경로없음() {
        when(mapper.countIdPrefix("FAU")).thenReturn(0);
        when(mapper.selectMenu("FAU0000")).thenReturn(row("FAU0000", "0", "장애관리", "G", 1000, "Y"));
        service.create(save(1, null, "fau", "장애관리", "/should/be-ignored"));

        ArgumentCaptor<MenuMgtRow> cap = ArgumentCaptor.forClass(MenuMgtRow.class);
        verify(mapper).insertMenu(cap.capture());
        assertThat(cap.getValue().getMenuId()).isEqualTo("FAU0000");
        assertThat(cap.getValue().getParentMenuId()).isEqualTo("0");
        assertThat(cap.getValue().getMenuType()).isEqualTo("G");
        assertThat(cap.getValue().getMenuPath()).isNull();
    }

    @Test
    void 등록_오류들() {
        // 접두어가 이미 쓰임
        when(mapper.countIdPrefix("SYS")).thenReturn(3);
        assertThatThrownBy(() -> service.create(save(1, null, "SYS", "x", null))).satisfies(t -> assertThat(code(t)).isEqualTo("MNU_005"));
        // 상위가 없음
        assertThatThrownBy(() -> service.create(save(2, "ZZZ0000", null, "x", null))).satisfies(t -> assertThat(code(t)).isEqualTo("MNU_004"));
        // 레벨 없음 · 상위 없음 · 이름 없음 · 화면 경로 형식
        assertThatThrownBy(() -> service.create(save(null, "SYS0000", null, "x", null))).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));
        assertThatThrownBy(() -> service.create(save(2, " ", null, "x", null))).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));
        when(mapper.selectMenu("SYS0300")).thenReturn(row("SYS0300", "SYS0000", "공통코드", "G", 2, "Y"));
        when(mapper.selectChildIds("SYS0300")).thenReturn(List.of());
        assertThatThrownBy(() -> service.create(save(3, "SYS0300", null, " ", "/sys/a"))).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));
        assertThatThrownBy(() -> service.create(save(3, "SYS0300", null, "a", "/Sys/a"))).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));
        // 같은 경로가 이미 있음
        when(mapper.countPath("/sys/a", null)).thenReturn(1);
        assertThatThrownBy(() -> service.create(save(3, "SYS0300", null, "a", "/sys/a"))).satisfies(t -> assertThat(code(t)).isEqualTo("MNU_008"));
        verify(mapper, never()).insertMenu(any());
    }

    @Test
    void 동시_등록으로_같은_ID_면_MNU_009() {
        when(mapper.selectMenu("SYS0000")).thenReturn(row("SYS0000", "0", "시스템관리", "G", 900, "Y"));
        when(mapper.selectChildIds("SYS0000")).thenReturn(List.of("SYS0100"));
        doThrow(new DuplicateKeyException("dup")).when(mapper).insertMenu(any());
        assertThatThrownBy(() -> service.create(save(2, "SYS0000", null, "로그", null))).satisfies(t -> assertThat(code(t)).isEqualTo("MNU_009"));
    }

    @Test
    void 수정은_이름_경로_순서_사용만_그룹은_경로를_비운다() {
        when(mapper.selectMenu("SYS0300")).thenReturn(row("SYS0300", "SYS0000", "공통코드", "G", 2, "Y"));
        when(mapper.updateMenu(any())).thenReturn(1);
        service.update("SYS0300", save(3, "SMP0000", "XXX", "코드", "/sys/abc"));
        ArgumentCaptor<MenuMgtRow> cap = ArgumentCaptor.forClass(MenuMgtRow.class);
        verify(mapper).updateMenu(cap.capture());
        assertThat(cap.getValue().getMenuId()).isEqualTo("SYS0300");
        assertThat(cap.getValue().getMenuNm()).isEqualTo("코드");
        assertThat(cap.getValue().getMenuPath()).isNull();
        assertThat(cap.getValue().getParentMenuId()).isNull(); // 상위는 SQL 에 없음

        // 화면은 자기 자신을 빼고 경로 겹침 검사
        when(mapper.selectMenu("SYS0312")).thenReturn(row("SYS0312", "SYS0300", "은행코드", "M", 2, "Y"));
        service.update("SYS0312", save(null, null, null, "은행코드", "/sys/bank-code"));
        verify(mapper).countPath("/sys/bank-code", "SYS0312");

        when(mapper.selectMenu("NONE")).thenReturn(null);
        assertThatThrownBy(() -> service.update("NONE", save(null, null, null, "a", null))).isInstanceOf(NotFoundException.class);
    }

    @Test
    void 삭제는_하위가_남으면_MNU_001_아니면_권한행부터_함께() {
        MenuMgtDeleteRequest req = new MenuMgtDeleteRequest();
        req.setMenuIds(List.of("SYS0300"));
        when(mapper.selectChildIds("SYS0300")).thenReturn(List.of("SYS0301"));
        assertThatThrownBy(() -> service.delete(req)).satisfies(t -> assertThat(code(t)).isEqualTo("MNU_001"));
        verify(mapper, never()).deleteMenus(anyList());

        // 하위까지 같이 고르면 지운다
        req.setMenuIds(List.of("SYS0300", "SYS0301", "SYS0301"));
        when(mapper.selectChildIds("SYS0301")).thenReturn(List.of());
        when(mapper.deleteAuthMenus(anyList())).thenReturn(4);
        when(mapper.deleteMenus(anyList())).thenReturn(2);
        MenuMgtDeleteResult r = service.delete(req);
        assertThat(r.count()).isEqualTo(2);
        assertThat(r.authCount()).isEqualTo(4);
        verify(mapper).deleteAuthMenus(eq(List.of("SYS0300", "SYS0301")));
    }

    @Test
    void 삭제_입력_검사() {
        MenuMgtDeleteRequest empty = new MenuMgtDeleteRequest();
        assertThatThrownBy(() -> service.delete(empty)).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));
        MenuMgtDeleteRequest none = new MenuMgtDeleteRequest();
        none.setMenuIds(List.of("NONE"));
        when(mapper.selectChildIds("NONE")).thenReturn(List.of());
        when(mapper.deleteMenus(anyList())).thenReturn(0);
        assertThatThrownBy(() -> service.delete(none)).isInstanceOf(NotFoundException.class);
    }
}
