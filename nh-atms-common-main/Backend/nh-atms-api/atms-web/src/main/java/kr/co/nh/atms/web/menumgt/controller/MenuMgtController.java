package kr.co.nh.atms.web.menumgt.controller;

import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.common.auth.Perm;
import kr.co.nh.atms.web.common.auth.RequireMenuAuth;
import kr.co.nh.atms.web.menumgt.dto.MenuMgtDeleteRequest;
import kr.co.nh.atms.web.menumgt.dto.MenuMgtDeleteResult;
import kr.co.nh.atms.web.menumgt.dto.MenuMgtRow;
import kr.co.nh.atms.web.menumgt.dto.MenuMgtSave;
import kr.co.nh.atms.web.menumgt.dto.MenuMgtSearchDto;
import kr.co.nh.atms.web.menumgt.service.MenuMgtService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 메뉴관리 API — API 계약 §9 (화면키 sample.menuMgt, 화면 경로 /sample/menu-mgt). TB_SCR_MENU 목록 + 팝업 등록·수정, 다건 삭제.
 * 권한은 메서드마다 {@code @RequireMenuAuth}(조회 QRY·등록 REG·수정 UPD·삭제 DEL, 없으면 403 SYS_403).
 */
@RestController
@RequiredArgsConstructor
public class MenuMgtController {

    private final MenuMgtService service;

    /** 목록(트리 순서 전체, 페이징 없음). 조건을 모두 비우면 전체 — 등록 팝업의 대·중메뉴 콤보도 이것을 쓴다 */
    @RequireMenuAuth(screen = MenuMgtService.SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/menu-mgt/menus")
    public ApiResponse<List<MenuMgtRow>> selectMenus(@ModelAttribute MenuMgtSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }

    @RequireMenuAuth(screen = MenuMgtService.SCREEN, need = Perm.REG)
    @PostMapping("/api/web/menu-mgt/menus")
    public ApiResponse<MenuMgtRow> insert(@RequestBody MenuMgtSave req) {
        return ApiResponse.ok(service.create(req), "S002");
    }

    @RequireMenuAuth(screen = MenuMgtService.SCREEN, need = Perm.UPD)
    @PutMapping("/api/web/menu-mgt/menus/{menuId}")
    public ApiResponse<MenuMgtRow> update(@PathVariable String menuId, @RequestBody MenuMgtSave req) {
        return ApiResponse.ok(service.update(menuId, req), "S003");
    }

    @RequireMenuAuth(screen = MenuMgtService.SCREEN, need = Perm.DEL)
    @PostMapping("/api/web/menu-mgt/menus/delete")
    public ApiResponse<MenuMgtDeleteResult> delete(@RequestBody MenuMgtDeleteRequest req) {
        MenuMgtDeleteResult r = service.delete(req);
        return ApiResponse.ok(r, "S005", r.count());
    }
}
