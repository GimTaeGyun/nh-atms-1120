package kr.co.nh.atms.web.menu.controller;

import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.menu.dto.MenuNode;
import kr.co.nh.atms.web.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import kr.co.nh.atms.web.common.uselog.ExcludeCrudLog;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 내 메뉴 트리 (API 계약 §1) */
@ExcludeCrudLog("내 메뉴 — 화면 틀이 자동으로 부름")
@RestController
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/api/web/menu/my")
    public ApiResponse<List<MenuNode>> my() {
        return ApiResponse.ok(menuService.myMenus());
    }
}
