package kr.co.nh.atms.web.common.uselog;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.web.common.auth.LoginUser;
import kr.co.nh.atms.web.common.auth.Perm;
import kr.co.nh.atms.web.common.auth.RequireMenuAuth;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 사용기록 Aspect — 성공 Y·실패 N(원래 예외 그대로)·저장 실패는 업무에 영향 없음·사용자 없으면 기록 없음·메뉴경로 */
class CrudLogAspectTest {

    private final CrudLogWriter writer = mock(CrudLogWriter.class);
    private final CrudLogAspect aspect = new CrudLogAspect(writer, new ObjectMapper());
    private MockHttpServletRequest req;

    /** 견본 컨트롤러 메서드(어노테이션 읽기용) */
    static class SampleController {
        @RequireMenuAuth(screen = "sample.notice", need = Perm.QRY)
        public Object selectList() { return null; }
        public Object deleteAll() { return null; }
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aspect, "enabled", true);
        req = new MockHttpServletRequest("GET", "/api/web/notice");
        req.setRemoteAddr("10.1.2.3");
        req.setParameter("ofcCode", "000080");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
        LoginUser u = new LoginUser("123456789", "홍길동", "000080", "본점", "000080", "000080", "본점", "L", "N");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    private ProceedingJoinPoint jp(String methodName) throws Exception {
        Method m = SampleController.class.getMethod(methodName);
        MethodSignature sig = mock(MethodSignature.class);
        when(sig.getMethod()).thenReturn(m);
        ProceedingJoinPoint jp = mock(ProceedingJoinPoint.class);
        when(jp.getSignature()).thenReturn(sig);
        when(jp.getArgs()).thenReturn(new Object[0]);
        return jp;
    }

    private CrudLogEntry written() {
        ArgumentCaptor<CrudLogEntry> c = ArgumentCaptor.forClass(CrudLogEntry.class);
        verify(writer).write(c.capture());
        return c.getValue();
    }

    @Test
    void 성공이면_Y_와_사용자_메뉴경로_IP_권한() throws Throwable {
        ProceedingJoinPoint jp = jp("selectList");
        when(jp.proceed()).thenReturn("ok");
        assertThat(aspect.around(jp)).isEqualTo("ok");
        CrudLogEntry e = written();
        assertThat(e.getCudType()).isEqualTo("R");
        assertThat(e.getSuccYn()).isEqualTo("Y");
        assertThat(e.getPerNo()).isEqualTo("123456789");
        assertThat(e.getOfcCode()).isEqualTo("000080");
        assertThat(e.getMenuPath()).isEqualTo("/sample/notice");
        assertThat(e.getUri()).isEqualTo("/api/web/notice");
        assertThat(e.getParams()).startsWith("ip=10.1.2.3 auth=L {").contains("\"ofcCode\":\"000080\"");
    }

    @Test
    void 실패면_N_과_오류코드_원래_예외를_그대로_던진다() throws Throwable {
        ProceedingJoinPoint jp = jp("deleteAll");
        req.addHeader("X-Screen-Key", "sample.mcnInfo");
        BusinessException boom = new BusinessException("NTC_002");
        when(jp.proceed()).thenThrow(boom);
        assertThatThrownBy(() -> aspect.around(jp)).isSameAs(boom);
        CrudLogEntry e = written();
        assertThat(e.getCudType()).isEqualTo("D");
        assertThat(e.getSuccYn()).isEqualTo("N");
        assertThat(e.getParams()).contains("err=NTC_002");
        assertThat(e.getMenuPath()).isEqualTo("/sample/mcn-info");
    }

    @Test
    void 저장이_실패해도_업무_응답은_그대로() throws Throwable {
        ProceedingJoinPoint jp = jp("selectList");
        when(jp.proceed()).thenReturn("ok");
        doThrow(new RuntimeException("DB down")).when(writer).write(any());
        assertThat(aspect.around(jp)).isEqualTo("ok");
    }

    @Test
    void 사용자가_없으면_기록하지_않는다() throws Throwable {
        SecurityContextHolder.clearContext();
        ProceedingJoinPoint jp = jp("selectList");
        when(jp.proceed()).thenReturn("ok");
        assertThat(aspect.around(jp)).isEqualTo("ok");
        verify(writer, never()).write(any());
    }

    @Test
    void 화면키_헤더가_이상하면_메뉴경로_없음() throws Throwable {
        ProceedingJoinPoint jp = jp("deleteAll");
        req.addHeader("X-Screen-Key", "../x'");
        when(jp.proceed()).thenReturn(null);
        aspect.around(jp);
        assertThat(written().getMenuPath()).isNull();
    }
}
