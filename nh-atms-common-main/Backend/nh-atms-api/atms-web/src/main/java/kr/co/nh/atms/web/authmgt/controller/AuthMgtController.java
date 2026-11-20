package kr.co.nh.atms.web.authmgt.controller;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.authmgt.dto.AuthCode;
import kr.co.nh.atms.web.authmgt.dto.AuthDept;
import kr.co.nh.atms.web.authmgt.dto.AuthDeleteResult;
import kr.co.nh.atms.web.authmgt.dto.AuthMenu;
import kr.co.nh.atms.web.authmgt.dto.AuthMenuSaveResult;
import kr.co.nh.atms.web.authmgt.dto.AuthUser;
import kr.co.nh.atms.web.authmgt.dto.AuthUserDeleteRequest;
import kr.co.nh.atms.web.authmgt.dto.AuthUserSave;
import kr.co.nh.atms.web.authmgt.dto.AuthUserSearchDto;
import kr.co.nh.atms.web.authmgt.service.AuthMgtService;
import kr.co.nh.atms.web.common.BatchChanges;
import kr.co.nh.atms.web.common.auth.Perm;
import kr.co.nh.atms.web.common.auth.RequireMenuAuth;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 권한관리 API — API 계약 §8 (화면키 sample.authMgt, 화면 경로 /sample/auth-mgt). AS-IS 시스템 사용권한부여 + 메뉴별 권한.
 * 권한은 계약 표 그대로 메서드마다 {@code @RequireMenuAuth}(조회 QRY·등록 REG·수정 UPD·삭제 DEL, 없으면 403 SYS_403).
 * 메뉴 권한 일괄 저장은 한 요청에 권한이 여럿 필요해서 AuthMgtService.saveAuthMenus 가 종류별로 검사한다(기기모델코드 견본과 같음).
 */
@RestController
@RequiredArgsConstructor
public class AuthMgtController {

    private final AuthMgtService service;

    /* ---------- 사용자 권한 (TB_SCR_AUTH) ---------- */

    @RequireMenuAuth(screen = AuthMgtService.SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/auth-mgt/users")
    public ApiResponse<PageResult<AuthUser>> selectUsers(@ModelAttribute AuthUserSearchDto s) {
        return ApiResponse.ok(service.searchUsers(s));
    }

    @RequireMenuAuth(screen = AuthMgtService.SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/auth-mgt/users/{deptCode}/{ofcCode}/{perNo}")
    public ApiResponse<AuthUser> selectUser(@PathVariable String deptCode, @PathVariable String ofcCode, @PathVariable String perNo) {
        return ApiResponse.ok(service.getUser(deptCode, ofcCode, perNo));
    }

    @RequireMenuAuth(screen = AuthMgtService.SCREEN, need = Perm.REG)
    @PostMapping("/api/web/auth-mgt/users")
    public ApiResponse<AuthUser> insertUser(@RequestBody AuthUserSave req) {
        return ApiResponse.ok(service.createUser(req), "S002");
    }

    @RequireMenuAuth(screen = AuthMgtService.SCREEN, need = Perm.UPD)
    @PutMapping("/api/web/auth-mgt/users/{deptCode}/{ofcCode}/{perNo}")
    public ApiResponse<AuthUser> updateUser(@PathVariable String deptCode, @PathVariable String ofcCode, @PathVariable String perNo,
                                            @RequestBody AuthUserSave req) {
        return ApiResponse.ok(service.updateUser(deptCode, ofcCode, perNo, req), "S003");
    }

    @RequireMenuAuth(screen = AuthMgtService.SCREEN, need = Perm.DEL)
    @PostMapping("/api/web/auth-mgt/users/delete")
    public ApiResponse<AuthDeleteResult> deleteUsers(@RequestBody AuthUserDeleteRequest req) {
        AuthDeleteResult r = service.deleteUsers(req);
        return ApiResponse.ok(r, "S005", r.count());
    }

    /** 부서 목록(TB_OFC_RAW) — 부서권한 등록 콤보 */
    @RequireMenuAuth(screen = AuthMgtService.SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/auth-mgt/depts")
    public ApiResponse<List<AuthDept>> selectDepts(@RequestParam(required = false) String q) {
        return ApiResponse.ok(service.depts(q));
    }

    /** 그 사람의 소속 부서·사무소(TB_INSA → TB_OFC_RAW) — 개인권한 등록 팝업 */
    @RequireMenuAuth(screen = AuthMgtService.SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/auth-mgt/person-dept/{perNo}")
    public ApiResponse<AuthDept> selectPersonDept(@PathVariable String perNo) {
        return ApiResponse.ok(service.personDept(perNo));
    }

    /* ---------- 권한코드 · 메뉴 권한 (TB_SCR_AUTH_MENU) ---------- */

    @RequireMenuAuth(screen = AuthMgtService.SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/auth-mgt/auth-codes")
    public ApiResponse<List<AuthCode>> selectAuthCodes() {
        return ApiResponse.ok(service.authCodes());
    }

    @RequireMenuAuth(screen = AuthMgtService.SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/auth-mgt/menus")
    public ApiResponse<List<AuthMenu>> selectAuthMenus(@RequestParam(required = false) String auth) {
        return ApiResponse.ok(service.authMenus(auth));
    }

    @PostMapping("/api/web/auth-mgt/menus/batch")
    public ApiResponse<AuthMenuSaveResult> updateAuthMenus(@RequestBody BatchChanges<AuthMenu> body) {
        return ApiResponse.ok(service.saveAuthMenus(body), "S009");
    }
}
