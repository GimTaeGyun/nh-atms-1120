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

import java.util.ArrayList;
import java.util.List;

/**
 * 저널조회(jnal.jnalList) — API 계약 §13. 규칙은 JnalRules 한 곳.
 * 기간을 운영/ILM 묶음으로 나눠 묶음마다 SQL → 거래명 채움 → As-Is 대로 합친 뒤 운영 묶음, ILM 묶음 순으로 이어 붙인다(페이지 없음).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JnalService {

    private final JnalMapper mapper;

    public List<Jnal> search(JnalSearchDto s) {
        String today = Dates.today();
        JnalRules.normalize(s, CurrentUser.auth(), CurrentUser.isAdmin(), CurrentUser.ofcCode(), today);
        List<Jnal> out = new ArrayList<>();
        for (JnalSearchDto part : JnalRules.split(s, today)) {
            List<Jnal> rows = mapper.selectList(part);
            for (Jnal r : rows) {
                r.setIlm(part.isIlm());
                r.setScrNm(JnalRules.scrNm(r.getJnalType(), r.getScrNo(), r.getScrNm()));
            }
            out.addAll(JnalRules.merge(rows));
        }
        return out;
    }

    /** 저널 한 건의 수표 — 상세 팝업 수표목록 탭 */
    public List<JnalCheck> checks(JnalCheckKey k) {
        JnalRules.normalizeKey(k, CurrentUser.auth(), CurrentUser.isAdmin(), CurrentUser.ofcCode(), Dates.today());
        return mapper.selectChecks(k);
    }
}
