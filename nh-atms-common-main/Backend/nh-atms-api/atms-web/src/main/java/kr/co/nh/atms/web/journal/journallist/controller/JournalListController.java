package kr.co.nh.atms.web.journal.journallist.controller;

import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.common.auth.Perm;
import kr.co.nh.atms.web.common.auth.RequireMenuAuth;
import kr.co.nh.atms.web.journal.journallist.dto.JournalListRow;
import kr.co.nh.atms.web.journal.journallist.dto.JournalListSearchDto;
import kr.co.nh.atms.web.journal.journallist.service.JournalListService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 저널조회 API — API 계약 §13 (화면키 journal.journalList, 화면 경로 /journal/journal-list).
 * 조회만 — 메뉴 권한 QRY, 사용기록 R 자동(메서드 이름 select…).
 */
@RestController
@RequiredArgsConstructor
public class JournalListController {

    /** 화면키(screens/screenIds.ts) */
    static final String SCREEN = "journal.journalList";

    private final JournalListService service;

    @RequireMenuAuth(screen = SCREEN, need = Perm.QRY)
    @GetMapping("/api/web/journal/journal-list")
    public ApiResponse<List<JournalListRow>> selectList(@ModelAttribute JournalListSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }
}
