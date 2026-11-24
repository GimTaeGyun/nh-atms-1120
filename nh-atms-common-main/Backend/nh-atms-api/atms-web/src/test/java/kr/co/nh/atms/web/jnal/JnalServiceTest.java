package kr.co.nh.atms.web.jnal;

import kr.co.nh.atms.web.common.Dates;
import kr.co.nh.atms.web.common.auth.LoginUser;
import kr.co.nh.atms.web.jnal.dto.Jnal;
import kr.co.nh.atms.web.jnal.dto.JnalCheck;
import kr.co.nh.atms.web.jnal.dto.JnalCheckKey;
import kr.co.nh.atms.web.jnal.dto.JnalSearchDto;
import kr.co.nh.atms.web.jnal.mapper.JnalMapper;
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

/** 저널조회 서비스 — TB_JL_JNAL 한 번 읽고 합친다(ILM 은 2차 파기 조회 화면 몫), 거래명 채움, 수표목록 키 정리 */
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
    void 기간이_3개월_전에_걸쳐도_TB_JL_JNAL_한번만_부르고_합친다() {
        LocalDate t = LocalDate.now();
        String to = t.minusMonths(3).plusDays(1).format(Dates.YMD);
        String from = t.minusMonths(3).format(Dates.YMD);
        Jnal a = row("22", 1, "1001", "현금인출");
        Jnal b = row("33", 2, "9937", null);
        Jnal c = row("55", 1, "1234", null);
        when(mapper.selectList(any())).thenReturn(new ArrayList<>(List.of(a, b, c)));
        JnalSearchDto s = new JnalSearchDto();
        s.setOfcCode("000080");
        s.setMcnNo("1");
        s.setFromDate(from);
        s.setToDate(to);

        List<Jnal> got = service.search(s);

        ArgumentCaptor<JnalSearchDto> cap = ArgumentCaptor.forClass(JnalSearchDto.class);
        verify(mapper, times(1)).selectList(cap.capture());
        assertThat(cap.getValue().getFromDate()).isEqualTo(from);
        assertThat(cap.getValue().getToDate()).isEqualTo(to);
        assertThat(got).containsExactly(a, c);
        assertThat(a.getRolComts()).hasSize(2);
        assertThat(a.getScrNm()).isEqualTo("현금인출");
        assertThat(c.getScrNm()).isEqualTo("기기운영관리");
    }

    @Test
    void 수표목록은_키를_정리해서_부른다() {
        when(mapper.selectChecks(any())).thenReturn(List.of(new JnalCheck()));
        service.checks(key(LocalDate.now().minusYears(1).format(Dates.YMD)));
        ArgumentCaptor<JnalCheckKey> c = ArgumentCaptor.forClass(JnalCheckKey.class);
        verify(mapper).selectChecks(c.capture());
        assertThat(c.getValue().getMcnNo()).isEqualTo("001");
        assertThat(c.getValue().getOfcCode()).isEqualTo("000080");
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
