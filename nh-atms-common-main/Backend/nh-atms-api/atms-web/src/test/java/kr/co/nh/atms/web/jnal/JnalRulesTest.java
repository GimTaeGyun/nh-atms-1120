package kr.co.nh.atms.web.jnal;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.web.jnal.dto.Jnal;
import kr.co.nh.atms.web.jnal.dto.JnalSearchDto;
import kr.co.nh.atms.web.jnal.service.JnalRules;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 저널조회 규칙 — 필수·2일·5년·조회 허가·사무소·IT부문·시간·명세표 범위·ILM 나누기·합치기·거래명 */
class JnalRulesTest {

    /** 오늘(고정) — ILM 기준일 20260825, 조회 허가 경계 20260824, 2차 파기 경계 20211124 */
    private static final String TODAY = "20261124";

    private static JnalSearchDto search(String from, String to) {
        JnalSearchDto s = new JnalSearchDto();
        s.setOfcCode("000080");
        s.setMcnNo("001");
        s.setFromDate(from);
        s.setToDate(to);
        return s;
    }

    private static void norm(JnalSearchDto s) {
        JnalRules.normalize(s, "A", true, "000080", TODAY);
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
        JnalSearchDto noMcn = search("20261124", "20261124");
        noMcn.setMcnNo(" ");
        assertThat(code(() -> norm(noMcn))).isEqualTo("VAL_002");
        assertThat(code(() -> norm(search("2026112", "20261124")))).isEqualTo("VAL_003");
        assertThat(code(() -> norm(search("20261124", "20261123")))).isEqualTo("VAL_001");
        assertThat(code(() -> norm(search("20261122", "20261124")))).isEqualTo("VAL_001");
        JnalSearchDto ok = search("20261123", "20261124");
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
        assertThat(code(() -> JnalRules.normalize(search("20260824", "20260824"), "T", false, "000080", TODAY))).isEqualTo("JNL_002");
        assertThat(code(() -> JnalRules.normalize(search("20260824", "20260824"), "P", false, "000080", TODAY))).isEqualTo("JNL_002");
        JnalRules.normalize(search("20260824", "20260824"), "L", false, "000080", TODAY);
        JnalRules.normalize(search("20260825", "20260825"), "T", false, "000080", TODAY);
    }

    @Test
    void 사무소_덮어쓰기() {
        JnalSearchDto p = search("20261124", "20261124");
        p.setOfcCode("000099");
        JnalRules.normalize(p, "P", false, "80", TODAY);
        assertThat(p.getOfcCode()).isEqualTo("000080");
        JnalSearchDto t = search("20261124", "20261124");
        t.setOfcCode("99");
        JnalRules.normalize(t, "T", false, "000080", TODAY);
        assertThat(t.getOfcCode()).isEqualTo("000099");
        assertThat(JnalRules.canPickOffice("L")).isTrue();
        assertThat(JnalRules.canPickOffice("P")).isFalse();
        assertThat(JnalRules.canPickOffice(null)).isFalse();
    }

    @Test
    void IT부문은_검색조건_무시() {
        JnalSearchDto s = search("20261124", "20261124");
        s.setField("ACCT");
        s.setKeyword("1234");
        JnalRules.normalize(s, "T", false, "000080", TODAY);
        assertThat(s.getField()).isNull();
        assertThat(s.getSchData1()).isNull();
        JnalSearchDto a = search("20261124", "20261124");
        a.setField("ACCT");
        a.setKeyword("1234");
        JnalRules.normalize(a, "A", true, "000080", TODAY);
        assertThat(a.getField()).isEqualTo("ACCT");
        assertThat(a.getSchData1()).isEqualTo("1234");
        JnalSearchDto other = search("20261124", "20261124");
        other.setField("acct");
        other.setKeyword(" 77 ");
        JnalRules.normalize(other, "T", false, "000100", TODAY);
        assertThat(other.getField()).isEqualTo("ACCT");
        assertThat(other.getSchData1()).isEqualTo("77");
    }

    @Test
    void 저널구분에_맞지않는_검색조건은_오류() {
        JnalSearchDto s = search("20261124", "20261124");
        s.setJnalType("22");
        s.setField("CHK");
        s.setKeyword("1");
        assertThat(code(() -> norm(s))).isEqualTo("VAL_003");
        JnalSearchDto t = search("20261124", "20261124");
        t.setJnalType("99");
        assertThat(code(() -> norm(t))).isEqualTo("VAL_003");
        JnalSearchDto r = search("20261124", "20261124");
        r.setJrnlProcRest("03");
        assertThat(code(() -> norm(r))).isEqualTo("VAL_003");
    }

    @Test
    void 시간은_HHmm_시작00초_끝59초() {
        JnalSearchDto s = search("20261124", "20261124");
        s.setFromTime("0900");
        s.setToTime("1359");
        norm(s);
        assertThat(s.getFrTi()).isEqualTo("090000");
        assertThat(s.getToTi()).isEqualTo("135959");
        JnalSearchDto d = search("20261124", "20261124");
        norm(d);
        assertThat(d.getFrTi()).isEqualTo("000000");
        assertThat(d.getToTi()).isEqualTo("235959");
        JnalSearchDto bad = search("20261124", "20261124");
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
        JnalSearchDto s = search("20261124", "20261124");
        s.setField("ACCT");
        s.setKeyword("301-12-345678");
        norm(s);
        assertThat(s.getSchData1()).isEqualTo("30112345678");
        assertThat(dtls(" 0001 - 0050 ")).containsExactly("0001", "0050");
        JnalSearchDto dash = search("20261124", "20261124");
        dash.setField("ACCT");
        dash.setKeyword("--");
        norm(dash);
        assertThat(dash.getField()).isNull();
        assertThat(dash.getSchData1()).isNull();
    }

    private static List<String> dtls(String keyword) {
        JnalSearchDto s = search("20261124", "20261124");
        s.setField("DTLS");
        s.setKeyword(keyword);
        norm(s);
        List<String> out = new ArrayList<>();
        out.add(s.getSchData1());
        out.add(s.getSchData2());
        return out;
    }

    @Test
    void 기준일을_가로지르면_두_묶음_운영먼저() {
        assertThat(JnalRules.ilmStart(TODAY)).isEqualTo("20260825");
        JnalSearchDto s = search("20260824", "20260825");
        norm(s);
        List<JnalSearchDto> parts = JnalRules.split(s, TODAY);
        assertThat(parts).hasSize(2);
        assertThat(parts.get(0).isIlm()).isFalse();
        assertThat(parts.get(0).getFrDt()).isEqualTo("20260825");
        assertThat(parts.get(0).getToDt()).isEqualTo("20260825");
        assertThat(parts.get(1).isIlm()).isTrue();
        assertThat(parts.get(1).getFrDt()).isEqualTo("20260824");
        assertThat(parts.get(1).getToDt()).isEqualTo("20260824");
        JnalSearchDto recent = search("20261123", "20261124");
        norm(recent);
        assertThat(JnalRules.split(recent, TODAY)).singleElement().satisfies(p -> {
            assertThat(p.isIlm()).isFalse();
            assertThat(p.getFrDt()).isEqualTo("20261123");
        });
        JnalSearchDto old = search("20260101", "20260102");
        norm(old);
        assertThat(JnalRules.split(old, TODAY)).singleElement().satisfies(p -> assertThat(p.isIlm()).isTrue());
    }

    private static Jnal row(String type, int pRn, String bfo, String aft, long amt) {
        Jnal r = new Jnal();
        r.setJnalType(type);
        r.setPRn(pRn);
        r.setBfoComt(bfo);
        r.setAftComt(aft);
        r.setDealAmt(amt);
        return r;
    }

    @Test
    void 같은_명세표는_앞행에_합친다() {
        Jnal a = row("33", 1, "A전", "A후", 10000);
        Jnal b = row("22", 2, "B전", "B후", 50000);
        Jnal c = row("55", 2, "C전", "C후", 0);
        List<Jnal> out = JnalRules.merge(List.of(a, b, c));
        assertThat(out).containsExactly(a, c);
        assertThat(a.getRolComts()).containsExactly("A전\nA후", "B전\nB후");
        assertThat(a.getDtlComts()).containsExactly("A후", "B후");
        assertThat(a.getDealAmt()).isEqualTo(50000L);
        assertThat(c.getRolComts()).containsExactly("C전\nC후");
    }

    @Test
    void 앞행이_없으면_합치지_않는다() {
        Jnal first = row("22", 2, null, "후", 0);
        List<Jnal> out = JnalRules.merge(List.of(first));
        assertThat(out).containsExactly(first);
        assertThat(first.getRolComts()).containsExactly("\n후");
    }

    @Test
    void 수표목록_키_정리와_사무소_기간_제한() {
        kr.co.nh.atms.web.jnal.dto.JnalCheckKey k = new kr.co.nh.atms.web.jnal.dto.JnalCheckKey();
        k.setJnalDt("20261124");
        k.setOfcCode("000099");
        k.setMcnNo("7");
        k.setJnalSeqno(" 000012 ");
        JnalRules.normalizeKey(k, "P", false, "80", TODAY);
        assertThat(k.getOfcCode()).isEqualTo("000080");
        assertThat(k.getMcnNo()).isEqualTo("007");
        assertThat(k.getJnalSeqno()).isEqualTo("000012");
        assertThat(k.isIlm()).isFalse();
        k.setJnalDt("20260824");
        assertThat(code(() -> JnalRules.normalizeKey(k, "T", false, "80", TODAY))).isEqualTo("JNL_002");
        JnalRules.normalizeKey(k, "L", false, "80", TODAY);
        assertThat(k.isIlm()).isTrue();
        k.setJnalSeqno(null);
        assertThat(code(() -> JnalRules.normalizeKey(k, "A", true, "80", TODAY))).isEqualTo("VAL_002");
    }

    @Test
    void 거래명() {
        assertThat(JnalRules.scrNm("55", "1234", "코드명")).isEqualTo("기기운영관리");
        assertThat(JnalRules.scrNm("00", "9937", "코드명")).isEqualTo("타행이체");
        assertThat(JnalRules.scrNm("33", "9949", null)).isEqualTo("타행이체조회");
        assertThat(JnalRules.scrNm("22", "1001", "현금인출")).isEqualTo("현금인출");
        assertThat(JnalRules.scrNm("22", "1001", null)).isNull();
    }
}
