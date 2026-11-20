package kr.co.nh.atms.web.uselog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.common.CurrentUser;
import kr.co.nh.atms.web.common.NotFoundException;
import kr.co.nh.atms.web.common.auth.LoginUser;
import kr.co.nh.atms.web.common.uselog.CrudLogAspect;
import kr.co.nh.atms.web.common.uselog.CrudLogEntry;
import kr.co.nh.atms.web.common.uselog.CrudLogRules;
import kr.co.nh.atms.web.common.uselog.CrudLogWriter;
import kr.co.nh.atms.web.uselog.dto.UseLog;
import kr.co.nh.atms.web.uselog.dto.UseLogExcelRequest;
import kr.co.nh.atms.web.uselog.dto.UseLogSearchDto;
import kr.co.nh.atms.web.uselog.mapper.UseLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용기록조회(sample.useLog) — API 계약 §12. 기록 자체는 공통 {@code web/common/uselog}(CrudLogAspect)가 남긴다.
 * 엑셀 기록은 화면 엑셀(클라이언트에서 만듦)이 서버를 거치지 않아 프론트 공통 downloadExcel 이 따로 알린다 → 수행작업 E.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UseLogService {

    private final UseLogMapper mapper;
    private final CrudLogWriter writer;
    private final ObjectMapper objectMapper;

    /** 목록(페이징, 최근 순). 건수가 0 이면 목록 SQL 은 부르지 않는다 */
    public PageResult<UseLog> search(UseLogSearchDto s) {
        UseLogRules.normalize(s, CurrentUser.isAdmin(), CurrentUser.ofcCode());
        long total = mapper.countList(s);
        List<UseLog> rows = total == 0 ? List.of() : mapper.selectList(s);
        for (UseLog r : rows) {
            UseLogRules.splitParams(r);
            r.setParams(null);
        }
        return PageResult.of(rows, total, s);
    }

    /** 1건(파라미터 원문·실행 SQL 포함). 없거나 볼 수 없는(다른 사무소) 기록이면 404 ULG_001 */
    public UseLog get(String histId) {
        long id;
        try {
            id = Long.parseLong(histId.trim());
        } catch (RuntimeException e) {
            throw new NotFoundException("ULG_001");
        }
        UseLog r = mapper.selectOne(id);
        if (r == null || !UseLogRules.canSee(r, CurrentUser.isAdmin(), CurrentUser.ofcCode())) throw new NotFoundException("ULG_001");
        UseLogRules.splitParams(r);
        return r;
    }

    /**
     * 엑셀 기록(수행작업 E) — 사유 없이 기록만(2026-11-17 사용자 결정). 화면 = X-Screen-Key 헤더.
     * 사용자가 없으면 기록하지 않는다. 저장 실패는 로그만 남기고 응답은 정상(내려받기를 막지 않음).
     */
    public void recordExcel(HttpServletRequest req, UseLogExcelRequest body) {
        LoginUser user = CurrentUser.get();
        if (user == null) return;
        try {
            String args = CrudLogRules.argsJson(objectMapper, null, body);
            CrudLogEntry e = CrudLogAspect.entryOf(req, user, CrudLogRules.EXCEL, null, null, args);
            writer.write(e);
        } catch (Exception ex) {
            log.error("[사용기록] 엑셀 기록 실패(내려받기는 계속): user={} err={}", user.perNo(), ex.toString());
        }
    }
}
