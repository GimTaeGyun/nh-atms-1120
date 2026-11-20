package kr.co.nh.atms.web.uselog;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.common.NotFoundException;
import kr.co.nh.atms.web.common.auth.LoginUser;
import kr.co.nh.atms.web.common.uselog.CrudLogEntry;
import kr.co.nh.atms.web.common.uselog.CrudLogWriter;
import kr.co.nh.atms.web.uselog.dto.UseLog;
import kr.co.nh.atms.web.uselog.dto.UseLogExcelRequest;
import kr.co.nh.atms.web.uselog.dto.UseLogSearchDto;
import kr.co.nh.atms.web.uselog.mapper.UseLogMapper;
import kr.co.nh.atms.web.uselog.service.UseLogRules;
import kr.co.nh.atms.web.uselog.service.UseLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 사용기록조회 — 기간 필수·31일, 보이는 범위(관리자 전체·그 밖 내 사무소), PARAMS 머리 나누기, 상세 404, 엑셀 기록 E */
class UseLogServiceTest {

    private final UseLogMapper mapper = mock(UseLogMapper.class);
    private final CrudLogWriter writer = mock(CrudLogWriter.class);
    private final UseLogService service = new UseLogService(mapper, writer, new ObjectMapper());

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void login(String ofc, String adminYn) {
        LoginUser u = new LoginUser("123456789", "홍길동", "000080", "본점", "000080", ofc, "본점", "L", adminYn);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of()));
    }

    private static UseLogSearchDto search(String from, String to) {
        UseLogSearchDto s = new UseLogSearchDto();
        s.setFromDate(from);
        s.setToDate(to);
        return s;
    }

    private static String code(Throwable t) {
        return ((BusinessException) t).getCode();
    }

    @Test
    void 기간은_필수_형식_순서_최대31일() {
        assertThat(code(catchIt(() -> UseLogRules.normalize(search(null, "20261117"), true, null)))).isEqualTo("VAL_002");
        assertThat(code(catchIt(() -> UseLogRules.normalize(search("2026111", "20261117"), true, null)))).isEqualTo("VAL_003");
        assertThat(code(catchIt(() -> UseLogRules.normalize(search("20261118", "20261117"), true, null)))).isEqualTo("VAL_001");
        assertThat(code(catchIt(() -> UseLogRules.normalize(search("20261001", "20261101"), true, null)))).isEqualTo("VAL_001");
        UseLogRules.normalize(search("20261002", "20261101"), true, null); // 31일 = 통과
    }

    @Test
    void 수행작업_결과_IP_검사와_정리() {
        UseLogSearchDto s = search("20261117", "20261117");
        s.setCudType("e");
        s.setSuccYn("n");
        s.setIp(" 10.1.2.3 ");
        UseLogRules.normalize(s, true, null);
        assertThat(s.getCudType()).isEqualTo("E");
        assertThat(s.getSuccYn()).isEqualTo("N");
        assertThat(s.getIpLike()).isEqualTo("ip=10.1.2.3 %");
        UseLogSearchDto bad = search("20261117", "20261117");
        bad.setIp("1.1.1.1%");
        assertThat(code(catchIt(() -> UseLogRules.normalize(bad, true, null)))).isEqualTo("VAL_003");
        UseLogSearchDto badType = search("20261117", "20261117");
        badType.setCudType("X");
        assertThat(code(catchIt(() -> UseLogRules.normalize(badType, true, null)))).isEqualTo("VAL_003");
    }

    @Test
    void 관리자가_아니면_내_사무소로_덮어쓴다() {
        UseLogSearchDto s = search("20261117", "20261117");
        s.setOfcCode("000099");
        UseLogRules.normalize(s, false, "80");
        assertThat(s.getOfcCode()).isEqualTo("000080");
        assertThat(s.isNone()).isFalse();
        UseLogSearchDto n = search("20261117", "20261117");
        UseLogRules.normalize(n, false, null);
        assertThat(n.isNone()).isTrue();
        UseLogSearchDto a = search("20261117", "20261117");
        a.setOfcCode("99");
        UseLogRules.normalize(a, true, "000080");
        assertThat(a.getOfcCode()).isEqualTo("000099");
    }

    @Test
    void 목록은_PARAMS_머리를_칸으로_나누고_원문은_보내지_않는다() {
        login("000080", "Y");
        UseLog r = new UseLog();
        r.setHistId("2026111709300010001");
        r.setParams("ip=10.1.2.3 auth=A err=NTC_002 {}");
        when(mapper.countList(any())).thenReturn(1L);
        when(mapper.selectList(any())).thenReturn(List.of(r));
        PageResult<UseLog> p = service.search(search("20261117", "20261117"));
        UseLog got = p.getContent().get(0);
        assertThat(got.getIp()).isEqualTo("10.1.2.3");
        assertThat(got.getAuth()).isEqualTo("A");
        assertThat(got.getErr()).isEqualTo("NTC_002");
        assertThat(got.getParams()).isNull();
    }

    @Test
    void 상세는_다른_사무소_기록이면_404() {
        login("000080", "N");
        UseLog r = new UseLog();
        r.setOfcCode("000099");
        when(mapper.selectOne(anyLong())).thenReturn(r);
        assertThatThrownBy(() -> service.get("2026111709300010001")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.get("abc")).isInstanceOf(NotFoundException.class);
        r.setOfcCode("000080");
        r.setParams("ip=1.1.1.1 auth=L {}");
        assertThat(service.get("2026111709300010001").getIp()).isEqualTo("1.1.1.1");
    }

    @Test
    void 엑셀_기록은_E_로_남기고_사용자가_없으면_건너뛴다() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/web/use-log/excel");
        req.addHeader("X-Screen-Key", "sample.mcnInfo");
        req.setRemoteAddr("10.9.9.9");
        UseLogExcelRequest body = new UseLogExcelRequest();
        body.setFileName("기기정보");
        body.setRowCount(20);
        service.recordExcel(req, body);
        verify(writer, never()).write(any());

        login("000080", "N");
        service.recordExcel(req, body);
        ArgumentCaptor<CrudLogEntry> c = ArgumentCaptor.forClass(CrudLogEntry.class);
        verify(writer).write(c.capture());
        CrudLogEntry e = c.getValue();
        assertThat(e.getCudType()).isEqualTo("E");
        assertThat(e.getMenuPath()).isEqualTo("/sample/mcn-info");
        assertThat(e.getParams()).startsWith("ip=10.9.9.9 auth=L {").contains("기기정보", "\"rowCount\":20");
    }

    private static Throwable catchIt(Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            return t;
        }
        throw new AssertionError("예외가 나야 함");
    }
}
