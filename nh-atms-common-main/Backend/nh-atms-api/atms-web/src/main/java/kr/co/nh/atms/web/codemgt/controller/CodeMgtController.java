package kr.co.nh.atms.web.codemgt.controller;

import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.codemgt.dto.CodeDetail;
import kr.co.nh.atms.web.codemgt.dto.CodeDetailSave;
import kr.co.nh.atms.web.codemgt.dto.CodeMaster;
import kr.co.nh.atms.web.codemgt.dto.CodeMasterSave;
import kr.co.nh.atms.web.codemgt.dto.CodeMgtDeleteRequest;
import kr.co.nh.atms.web.codemgt.dto.CodeMgtDeleteResult;
import kr.co.nh.atms.web.codemgt.dto.CodeMgtSearchDto;
import kr.co.nh.atms.web.codemgt.service.CodeMgtService;
import kr.co.nh.atms.web.common.auth.Perm;
import kr.co.nh.atms.web.common.auth.RequireMenuAuth;
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
 * 코드관리 API — API 계약 §11 (화면키 sample.codeMgt, 화면 경로 /sample/code-mgt). TB_CODE_MASTER · TB_CODE_DTL.
 * 권한은 메서드마다 {@code @RequireMenuAuth}(조회 QRY·등록 REG·수정 UPD·삭제 DEL, 없으면 403 SYS_403).
 */
@RestController
@RequiredArgsConstructor
public class CodeMgtController {

    private final CodeMgtService service;

    @RequireMenuAuth(screen = CodeMgtService.SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/code-mgt/masters")
    public ApiResponse<List<CodeMaster>> selectMasters(@ModelAttribute CodeMgtSearchDto s) {
        return ApiResponse.ok(service.masters(s));
    }

    @RequireMenuAuth(screen = CodeMgtService.SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/code-mgt/masters/{codeSect}")
    public ApiResponse<CodeMaster> selectMaster(@PathVariable String codeSect) {
        return ApiResponse.ok(service.master(codeSect));
    }

    @RequireMenuAuth(screen = CodeMgtService.SCREEN, need = Perm.REG)
    @PostMapping("/api/web/code-mgt/masters")
    public ApiResponse<CodeMaster> insertMaster(@RequestBody CodeMasterSave req) {
        return ApiResponse.ok(service.createMaster(req), "S002");
    }

    @RequireMenuAuth(screen = CodeMgtService.SCREEN, need = Perm.UPD)
    @PutMapping("/api/web/code-mgt/masters/{codeSect}")
    public ApiResponse<CodeMaster> updateMaster(@PathVariable String codeSect, @RequestBody CodeMasterSave req) {
        return ApiResponse.ok(service.updateMaster(codeSect, req), "S003");
    }

    @RequireMenuAuth(screen = CodeMgtService.SCREEN, need = Perm.DEL)
    @PostMapping("/api/web/code-mgt/masters/delete")
    public ApiResponse<CodeMgtDeleteResult> deleteMasters(@RequestBody CodeMgtDeleteRequest req) {
        CodeMgtDeleteResult r = service.deleteMasters(req);
        return ApiResponse.ok(r, "S005", r.count());
    }

    @RequireMenuAuth(screen = CodeMgtService.SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/code-mgt/masters/{codeSect}/codes")
    public ApiResponse<List<CodeDetail>> selectDetails(@PathVariable String codeSect) {
        return ApiResponse.ok(service.details(codeSect));
    }

    @RequireMenuAuth(screen = CodeMgtService.SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/code-mgt/masters/{codeSect}/codes/{code}")
    public ApiResponse<CodeDetail> selectDetail(@PathVariable String codeSect, @PathVariable String code) {
        return ApiResponse.ok(service.detail(codeSect, code));
    }

    @RequireMenuAuth(screen = CodeMgtService.SCREEN, need = Perm.REG)
    @PostMapping("/api/web/code-mgt/masters/{codeSect}/codes")
    public ApiResponse<CodeDetail> insertDetail(@PathVariable String codeSect, @RequestBody CodeDetailSave req) {
        return ApiResponse.ok(service.createDetail(codeSect, req), "S002");
    }

    @RequireMenuAuth(screen = CodeMgtService.SCREEN, need = Perm.UPD)
    @PutMapping("/api/web/code-mgt/masters/{codeSect}/codes/{code}")
    public ApiResponse<CodeDetail> updateDetail(@PathVariable String codeSect, @PathVariable String code, @RequestBody CodeDetailSave req) {
        return ApiResponse.ok(service.updateDetail(codeSect, code, req), "S003");
    }

    @RequireMenuAuth(screen = CodeMgtService.SCREEN, need = Perm.DEL)
    @PostMapping("/api/web/code-mgt/masters/{codeSect}/codes/delete")
    public ApiResponse<CodeMgtDeleteResult> deleteDetails(@PathVariable String codeSect, @RequestBody CodeMgtDeleteRequest req) {
        CodeMgtDeleteResult r = service.deleteDetails(codeSect, req);
        return ApiResponse.ok(r, "S005", r.count());
    }
}
