package kr.co.nh.atms.web.uselog.controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.common.auth.Perm;
import kr.co.nh.atms.web.common.auth.RequireMenuAuth;
import kr.co.nh.atms.web.common.uselog.ExcludeCrudLog;
import kr.co.nh.atms.web.uselog.dto.UseLog;
import kr.co.nh.atms.web.uselog.dto.UseLogExcelRequest;
import kr.co.nh.atms.web.uselog.dto.UseLogSearchDto;
import kr.co.nh.atms.web.uselog.service.UseLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용기록조회 API — API 계약 §12 (화면키 sample.useLog, 화면 경로 /sample/use-log).
 * 조회·상세 = 메뉴 권한 QRY(관리자 = 전체, 그 밖 = 내 사무소 기록). 이 화면을 본 것도 사용기록에 남는다(R).
 * 엑셀 기록은 모든 화면의 공통 엑셀 버튼이 부른다 → 메뉴 권한 없이, 자동 기록에서는 빼고(@ExcludeCrudLog) 서비스가 E 로 직접 남긴다.
 */
@RestController
@RequiredArgsConstructor
public class UseLogController {

    /** 화면키(screens/screenIds.ts) */
    static final String SCREEN = "sample.useLog";

    private final UseLogService service;

    @RequireMenuAuth(screen = SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/use-log")
    public ApiResponse<PageResult<UseLog>> selectList(@ModelAttribute UseLogSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }

    @RequireMenuAuth(screen = SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/use-log/{histId}")
    public ApiResponse<UseLog> selectDetail(@PathVariable String histId) {
        return ApiResponse.ok(service.get(histId));
    }

    @ExcludeCrudLog("엑셀 기록 — 서비스가 수행작업 E 로 직접 남긴다")
    @PostMapping("/api/web/use-log/excel")
    public ApiResponse<Void> insertExcel(@RequestBody UseLogExcelRequest body, HttpServletRequest req) {
        service.recordExcel(req, body);
        return ApiResponse.ok(null);
    }
}
