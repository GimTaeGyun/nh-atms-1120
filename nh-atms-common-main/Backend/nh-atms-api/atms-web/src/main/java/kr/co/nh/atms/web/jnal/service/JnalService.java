package kr.co.nh.atms.web.jnal.service;

import kr.co.nh.atms.web.common.CurrentUser;
import kr.co.nh.atms.web.common.Dates;
import kr.co.nh.atms.web.jnal.dto.Jnal;
import kr.co.nh.atms.web.jnal.dto.JnalCheck;
import kr.co.nh.atms.web.jnal.dto.JnalCheckKey;
import kr.co.nh.atms.web.jnal.dto.JnalSearchDto;
import kr.co.nh.atms.web.jnal.mapper.JnalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 저널조회(jnal.jnalList) — API 계약 §13. 규칙은 JnalRules 한 곳.
 * 조건 정리 → TB_JL_JNAL 한 번 읽기 → 거래명 채움 → As-Is 대로 합침(페이지 없음). ILM 은 저널 2차 파기 조회 화면 몫.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JnalService {

    private final JnalMapper mapper;

    public List<Jnal> search(JnalSearchDto s) {
        JnalRules.normalize(s, CurrentUser.auth(), CurrentUser.isAdmin(), CurrentUser.ofcCode(), Dates.today());
        List<Jnal> rows = mapper.selectList(s);
        for (Jnal r : rows) r.setScrNm(JnalRules.scrNm(r.getJnalType(), r.getScrNo(), r.getScrNm()));
        return JnalRules.merge(rows);
    }

    /** 저널 한 건의 수표 — 상세 팝업 수표목록 탭 */
    public List<JnalCheck> checks(JnalCheckKey k) {
        JnalRules.normalizeKey(k, CurrentUser.auth(), CurrentUser.isAdmin(), CurrentUser.ofcCode(), Dates.today());
        return mapper.selectChecks(k);
    }
}
