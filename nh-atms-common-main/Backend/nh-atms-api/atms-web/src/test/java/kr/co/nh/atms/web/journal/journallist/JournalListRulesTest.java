package kr.co.nh.atms.web.journal.journallist;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.web.journal.journallist.dto.JournalListRow;
import kr.co.nh.atms.web.journal.journallist.dto.JournalListSearchDto;
import kr.co.nh.atms.web.journal.journallist.service.JournalListRules;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 저널조회 규칙 — 필수·2일·5년·조회 허가·사무소·IT부문·시간·명세표 범위·합치기·거래명 */
class JournalListRulesTest {

    /** 오늘(고정) — 조회 허가 경계 20260824, 2차 파기 경계 20211124 */
    private static final String TODAY = "20261124";

    private static JournalListSearchDto search(String from, String to) {
        JournalListSearchDto s = new JournalListSearchDto();
        s.setOfcCode("000080");
        s.setMcnNo("001");
        s.setFromDate(from);
        s.setToDate(to);
        return s;
    }

    private static void norm(JournalListSearchDto s) {
        JournalListRules.normalize(s, "A", true, "000080", TODAY);
    }

    private static String code(Runnable r) {
        try {
            r.run();
        } catch (BusinessException e) {
            return e.getCode();
        }
        throw new AssertionError("예외가 나야 함");
    }

    @Test
    void 필수_형식_순서_최대2일() {
        assertThat(code(() -> norm(search(null, "20261124")))).isEqualTo("VAL_002");
        JournalListSearchDto noMcn = search("20261124", "20261124");
        noMcn.setMcnNo(" ");
        assertThat(code(() -> norm(noMcn))).isEqualTo("VAL_002");
        assertThat(code(() -> norm(search("2026112", "20261124")))).isEqualTo("VAL_003");
        assertThat(code(() -> norm(search("20261124", "20261123")))).isEqualTo("VAL_001");
        assertThat(code(() -> norm(search("20261122", "20261124")))).isEqualTo("VAL_001");
        JournalListSearchDto ok = search("20261123", "20261124");
        ok.setMcnNo("1");
        norm(ok);
        assertThat(ok.getMcnNo()).isEqualTo("001");
    }

    @Test
    void 이차파기_5년이면_막는다() {
        assertThat(code(() -> norm(search("20211124", "20211124")))).isEqualTo("JNL_001");
        norm(search("20211125", "20211125"));
    }

    @Test
    void 조회허가_3개월넘으면_A_L만() {
        assertThat(code(() -> JournalListRules.normalize(search("20260824", "20260824"), "T", false, "000080", TODAY))).isEqualTo("JNL_002");
        assertThat(code(() -> JournalListRules.normalize(search("20260824", "20260824"), "P", false, "000080", TODAY))).isEqualTo("JNL_002");
        JournalListRules.normalize(search("20260824", "20260824"), "L", false, "000080", TODAY);
        JournalListRules.normalize(search("20260825", "20260825"), "T", false, "000080", TODAY);
    }

    @Test
    void 사무소_덮어쓰기() {
        JournalListSearchDto p = search("20261124", "20261124");
        p.setOfcCode("000099");
        JournalListRules.normalize(p, "P", false, "80", TODAY);
        assertThat(p.getOfcCode()).isEqualTo("000080");
        JournalListSearchDto t = search("20261124", "20261124");
        t.setOfcCode("99");
        JournalListRules.normalize(t, "T", false, "000080", TODAY);
        assertThat(t.getOfcCode()).isEqualTo("000099");
        assertThat(JournalListRules.canPickOffice("L")).isTrue();
        assertThat(JournalListRules.canPickOffice("P")).isFalse();
        assertThat(JournalListRules.canPickOffice(null)).isFalse();
    }

    @Test
    void IT부문은_검색조건_무시() {
        JournalListSearchDto s = search("20261124", "20261124");
        s.setField("ACCT");
        s.setKeyword("1234");
        JournalListRules.normalize(s, "T", false, "000080", TODAY);
        assertThat(s.getField()).isNull();
        assertThat(s.getSchData1()).isNull();
        JournalListSearchDto a = search("20261124", "20261124");
        a.setField("ACCT");
        a.setKeyword("1234");
        JournalListRules.normalize(a, "A", true, "000080", TODAY);
        assertThat(a.getField()).isEqualTo("ACCT");
        assertThat(a.getSchData1()).isEqualTo("1234");
        JournalListSearchDto other = search("20261124", "20261124");
        other.setField("acct");
        other.setKeyword(" 77 ");
        JournalListRules.normalize(other, "T", false, "000100", TODAY);
        assertThat(other.getField()).isEqualTo("ACCT");
        assertThat(other.getSchData1()).isEqualTo("77");
    }

    @Test
    void 저널구분에_맞지않는_검색조건은_오류() {
        JournalListSearchDto s = search("20261124", "20261124");
        s.setJnalType("22");
        s.setField("CHK");
        s.setKeyword("1");
        assertThat(code(() -> norm(s))).isEqualTo("VAL_003");
        JournalListSearchDto t = search("20261124", "20261124");
        t.setJnalType("99");
        assertThat(code(() -> norm(t))).isEqualTo("VAL_003");
        JournalListSearchDto r = search("20261124", "20261124");
        r.setJrnlProcRest("03");
        assertThat(code(() -> norm(r))).isEqualTo("VAL_003");
    }

    @Test
    void 시간은_HHmm_시작00초_끝59초() {
        JournalListSearchDto s = search("20261124", "20261124");
        s.setFromTime("0900");
        s.setToTime("1359");
        norm(s);
        assertThat(s.getFrTi()).isEqualTo("090000");
        assertThat(s.getToTi()).isEqualTo("135959");
        JournalListSearchDto d = search("20261124", "20261124");
        norm(d);
        assertThat(d.getFrTi()).isEqualTo("000000");
        assertThat(d.getToTi()).isEqualTo("235959");
        JournalListSearchDto bad = search("20261124", "20261124");
        bad.setToTime("2400");
        assertThat(code(() -> norm(bad))).isEqualTo("VAL_003");
    }

    @Test
    void 명세표_범위_나누기() {
        assertThat(dtls("0001-0050")).containsExactly("0001", "0050");
        assertThat(dtls("12")).containsExactly("0012", "0012");
        assertThat(dtls("-0050")).containsExactly(null, "0050");
        assertThat(dtls("0001-")).containsExactly("0001", null);
        assertThat(code(() -> dtls("0050-0001"))).isEqualTo("VAL_003");
        assertThat(code(() -> dtls("12a"))).isEqualTo("VAL_003");
        assertThat(code(() -> dtls("12345"))).isEqualTo("VAL_003");
    }

    @Test
    void 계좌_검색어는_하이픈과_공백을_뗀다_명세표는_공백을_뗀다() {
        JournalListSearchDto s = search("20261124", "20261124");
        s.setField("ACCT");
        s.setKeyword("301-12-345678");
        norm(s);
        assertThat(s.getSchData1()).isEqualTo("30112345678");
        assertThat(dtls(" 0001 - 0050 ")).containsExactly("0001", "0050");
        JournalListSearchDto dash = search("20261124", "20261124");
        dash.setField("ACCT");
        dash.setKeyword("--");
        norm(dash);
        assertThat(dash.getField()).isNull();
        assertThat(dash.getSchData1()).isNull();
    }

    private static List<String> dtls(String keyword) {
        JournalListSearchDto s = search("20261124", "20261124");
        s.setField("DTLS");
        s.setKeyword(keyword);
        norm(s);
        List<String> out = new ArrayList<>();
        out.add(s.getSchData1());
        out.add(s.getSchData2());
        return out;
    }

    private static JournalListRow row(String type, int pRn, String bfo, String aft, long amt) {
        JournalListRow r = new JournalListRow();
        r.setJnalType(type);
        r.setPRn(pRn);
        r.setBfoComt(bfo);
        r.setAftComt(aft);
        r.setDealAmt(amt);
        return r;
    }

    @Test
    void 같은_명세표는_앞행에_합친다() {
        JournalListRow a = row("33", 1, "A전", "A후", 10000);
        JournalListRow b = row("22", 2, "B전", "B후", 50000);
        JournalListRow c = row("55", 2, "C전", "C후", 0);
        List<JournalListRow> out = JournalListRules.merge(List.of(a, b, c));
        assertThat(out).containsExactly(a, c);
        assertThat(a.getRolComts()).containsExactly("A전\nA후", "B전\nB후");
        assertThat(a.getDtlComts()).containsExactly("A후", "B후");
        assertThat(a.getDealAmt()).isEqualTo(50000L);
        assertThat(c.getRolComts()).containsExactly("C전\nC후");
    }

    @Test
    void 앞행이_없으면_합치지_않는다() {
        JournalListRow first = row("22", 2, null, "후", 0);
        List<JournalListRow> out = JournalListRules.merge(List.of(first));
        assertThat(out).containsExactly(first);
        assertThat(first.getRolComts()).containsExactly("\n후");
    }

    @Test
    void 거래명() {
        assertThat(JournalListRules.scrNm("55", "1234", "코드명")).isEqualTo("기기운영관리");
        assertThat(JournalListRules.scrNm("00", "9937", "코드명")).isEqualTo("타행이체");
        assertThat(JournalListRules.scrNm("33", "9949", null)).isEqualTo("타행이체조회");
        assertThat(JournalListRules.scrNm("22", "1001", "현금인출")).isEqualTo("현금인출");
        assertThat(JournalListRules.scrNm("22", "1001", null)).isNull();
    }
}
