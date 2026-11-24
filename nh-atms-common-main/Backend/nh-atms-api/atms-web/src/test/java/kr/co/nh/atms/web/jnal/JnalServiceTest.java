package kr.co.nh.atms.web.jnal;

import kr.co.nh.atms.web.common.Dates;
import kr.co.nh.atms.web.common.auth.LoginUser;
import kr.co.nh.atms.web.jnal.dto.Jnal;
import kr.co.nh.atms.web.jnal.dto.JnalCheck;
import kr.co.nh.atms.web.jnal.dto.JnalCheckKey;
import kr.co.nh.atms.web.jnal.dto.JnalSearchDto;
import kr.co.nh.atms.web.jnal.mapper.JnalMapper;
import kr.co.nh.atms.web.jnal.service.JnalRules;
import kr.co.nh.atms.web.jnal.service.JnalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 저널조회 서비스 — 운영/ILM 묶음마다 매퍼를 부르고 묶음마다 합친다, 거래명 채움, 수표목록 테이블 고름 */
class JnalServiceTest {

    private final JnalMapper mapper = mock(JnalMapper.class);
    private final JnalService service = new JnalService(mapper);

    @BeforeEach
    void login() {
        LoginUser u = new LoginUser("123456789", "홍길동", "000080", "본점", "000080", "000080", "본점", "A", "Y");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static Jnal row(String type, int pRn, String scrNo, String codeNm) {
        Jnal r = new Jnal();
        r.setJnalType(type);
        r.setPRn(pRn);
        r.setScrNo(scrNo);
        r.setScrNm(codeNm);
        r.setBfoComt("전");
        r.setAftComt("후");
        r.setDealAmt(1000L);
        return r;
    }

    @Test
    void 기준일을_가로지르면_묶음마다_부르고_운영묶음이_먼저() {
        String start = JnalRules.ilmStart(Dates.today());
        String before = Dates.parseYmd(start).minusDays(1).format(Dates.YMD);
        Jnal op = row("22", 1, "1001", "현금인출");
        Jnal ilm = row("55", 1, "1234", null);
        when(mapper.selectList(any())).thenAnswer(inv -> {
            JnalSearchDto p = inv.getArgument(0);
            return new ArrayList<>(List.of(p.isIlm() ? ilm : op));
        });
        JnalSearchDto s = new JnalSearchDto();
        s.setOfcCode("000080");
        s.setMcnNo("1");
        s.setFromDate(before);
        s.setToDate(start);

        List<Jnal> got = service.search(s);

        ArgumentCaptor<JnalSearchDto> c = ArgumentCaptor.forClass(JnalSearchDto.class);
        verify(mapper, times(2)).selectList(c.capture());
        assertThat(c.getAllValues()).extracting(JnalSearchDto::isIlm).containsExactly(false, true);
        assertThat(got).containsExactly(op, ilm);
        assertThat(got.get(0).isIlm()).isFalse();
        assertThat(got.get(1).isIlm()).isTrue();
        assertThat(got.get(0).getScrNm()).isEqualTo("현금인출");
        assertThat(got.get(1).getScrNm()).isEqualTo("기기운영관리");
        assertThat(got.get(0).getRolComts()).containsExactly("전\n후");
    }

    @Test
    void 수표목록은_저널일자로_ILM_테이블을_고른다() {
        when(mapper.selectChecks(any())).thenReturn(List.of(new JnalCheck()));
        JnalCheckKey old = key(LocalDate.now().minusYears(1).format(Dates.YMD));
        service.checks(old);
        JnalCheckKey recent = key(Dates.today());
        service.checks(recent);
        ArgumentCaptor<JnalCheckKey> c = ArgumentCaptor.forClass(JnalCheckKey.class);
        verify(mapper, times(2)).selectChecks(c.capture());
        assertThat(c.getAllValues()).extracting(JnalCheckKey::isIlm).containsExactly(true, false);
        assertThat(c.getAllValues().get(1).getMcnNo()).isEqualTo("001");
    }

    private static JnalCheckKey key(String jnalDt) {
        JnalCheckKey k = new JnalCheckKey();
        k.setJnalDt(jnalDt);
        k.setOfcCode("80");
        k.setMcnNo("1");
        k.setJnalSeqno("000001");
        return k;
    }
}
