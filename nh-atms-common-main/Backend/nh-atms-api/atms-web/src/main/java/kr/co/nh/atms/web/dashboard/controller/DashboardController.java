package kr.co.nh.atms.web.dashboard.controller;

import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.common.auth.Perm;
import kr.co.nh.atms.web.common.auth.RequireMenuAuth;
import kr.co.nh.atms.web.dashboard.dto.DashOffice;
import kr.co.nh.atms.web.dashboard.dto.DashScope;
import kr.co.nh.atms.web.dashboard.dto.DashSummary;
import kr.co.nh.atms.web.dashboard.service.DashboardService;
import kr.co.nh.atms.web.notice.dto.Notice;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import kr.co.nh.atms.web.common.uselog.ExcludeCrudLog;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 대시보드 API — API 계약 §10 (화면키 main.dashboard, 메뉴 경로 /main/dashboard, 홈 탭이 이 화면).
 * 2026-11-16 사용자 결정: 대시보드를 메뉴로 등록해 권한관리의 조회(QRY) 권한으로 막는다(메뉴 행 SQL db/change/dashboard_menu.sql).
 * 공지 2개 API 도 이 화면 권한으로 — 공지사항 메뉴 권한이 없어도 대시보드에서는 보인다(보이는 범위 규칙은 공지사항과 같음).
 */
@ExcludeCrudLog("대시보드 — 홈 탭이라 매번 불림")
@RestController
@RequiredArgsConstructor
public class DashboardController {

    static final String SCREEN = "main.dashboard";

    private final DashboardService service;

    @RequireMenuAuth(screen = SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/dashboard/scope")
    public ApiResponse<DashScope> scope() {
        return ApiResponse.ok(service.scope());
    }

    @RequireMenuAuth(screen = SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/dashboard/offices")
    public ApiResponse<List<DashOffice>> offices(@RequestParam(required = false) String region) {
        return ApiResponse.ok(service.offices(region));
    }

    @RequireMenuAuth(screen = SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/dashboard/summary")
    public ApiResponse<DashSummary> summary(@RequestParam(required = false) String region, @RequestParam(required = false) String ofcCode) {
        return ApiResponse.ok(service.summary(region, ofcCode));
    }

    @RequireMenuAuth(screen = SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/dashboard/notices")
    public ApiResponse<List<Notice>> notices() {
        return ApiResponse.ok(service.notices());
    }

    @RequireMenuAuth(screen = SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/dashboard/notices/{no}")
    public ApiResponse<Notice> notice(@PathVariable long no) {
        return ApiResponse.ok(service.notice(no));
    }
}
