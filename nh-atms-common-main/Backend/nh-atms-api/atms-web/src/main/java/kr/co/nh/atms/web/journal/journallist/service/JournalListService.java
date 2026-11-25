package kr.co.nh.atms.web.journal.journallist.service;

import kr.co.nh.atms.web.common.CurrentUser;
import kr.co.nh.atms.web.common.Dates;
import kr.co.nh.atms.web.journal.journallist.dto.JournalListRow;
import kr.co.nh.atms.web.journal.journallist.dto.JournalListSearchDto;
import kr.co.nh.atms.web.journal.journallist.mapper.JournalListMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 저널조회(journal.journalList) — API 계약 §13. 규칙은 JournalListRules 한 곳.
 * 조건 정리 → TB_JL_JNAL 한 번 읽기 → 거래명 채움 → As-Is 대로 합침(페이지 없음). ILM 은 저널 2차 파기 조회 화면 몫.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JournalListService {

    private final JournalListMapper mapper;

    public List<JournalListRow> search(JournalListSearchDto s) {
        JournalListRules.normalize(s, CurrentUser.auth(), CurrentUser.isAdmin(), CurrentUser.ofcCode(), Dates.today());
        List<JournalListRow> rows = mapper.selectList(s);
        for (JournalListRow r : rows) r.setScrNm(JournalListRules.scrNm(r.getJnalType(), r.getScrNo(), r.getScrNm()));
        return JournalListRules.merge(rows);
    }
}
