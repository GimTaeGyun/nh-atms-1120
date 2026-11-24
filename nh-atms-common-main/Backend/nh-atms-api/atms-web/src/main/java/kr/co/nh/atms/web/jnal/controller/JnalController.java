package kr.co.nh.atms.web.jnal.controller;

import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.common.auth.Perm;
import kr.co.nh.atms.web.common.auth.RequireMenuAuth;
import kr.co.nh.atms.web.jnal.dto.Jnal;
import kr.co.nh.atms.web.jnal.dto.JnalCheck;
import kr.co.nh.atms.web.jnal.dto.JnalCheckKey;
import kr.co.nh.atms.web.jnal.dto.JnalSearchDto;
import kr.co.nh.atms.web.jnal.service.JnalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 저널조회 API — API 계약 §13 (화면키 jnal.jnalList, 화면 경로 /jnal/jnal-list).
 * 조회만 — 메뉴 권한 QRY, 사용기록 R 자동(메서드 이름 select…).
 */
@RestController
@RequiredArgsConstructor
public class JnalController {

    /** 화면키(screens/screenIds.ts) */
    static final String SCREEN = "jnal.jnalList";

    private final JnalService service;

    @RequireMenuAuth(screen = SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/jnal")
    public ApiResponse<List<Jnal>> selectList(@ModelAttribute JnalSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }

    @RequireMenuAuth(screen = SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/jnal/checks")
    public ApiResponse<List<JnalCheck>> selectChecks(@ModelAttribute JnalCheckKey k) {
        return ApiResponse.ok(service.checks(k));
    }
}
