package kr.co.nh.atms.web.common.auth;

import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.common.CurrentUser;
import kr.co.nh.atms.web.common.UnauthorizedException;
import kr.co.nh.atms.web.common.uselog.ExcludeCrudLog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 나(현재 사용자) — 프론트 머리글·기본 사무소·관리자 여부 (API 계약 §1) */
@ExcludeCrudLog("내 정보 — 화면 틀이 자동으로 부름")
@RestController
public class MeController {

    @GetMapping("/api/web/me")
    public ApiResponse<Me> me() {
        LoginUser u = CurrentUser.get();
        if (u == null) throw new UnauthorizedException("ME_001");
        return ApiResponse.ok(Me.of(u));
    }
}
