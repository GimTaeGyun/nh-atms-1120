package kr.co.nh.atms.web.dashboard;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.web.common.ForbiddenException;
import kr.co.nh.atms.web.common.auth.LoginUser;
import kr.co.nh.atms.web.dashboard.dto.DashMachine;
import kr.co.nh.atms.web.dashboard.dto.DashOffice;
import kr.co.nh.atms.web.dashboard.dto.DashScope;
import kr.co.nh.atms.web.dashboard.dto.DashSummary;
import kr.co.nh.atms.web.dashboard.mapper.DashboardMapper;
import kr.co.nh.atms.web.dashboard.service.DashboardRules;
import kr.co.nh.atms.web.dashboard.service.DashboardService;
import kr.co.nh.atms.web.notice.service.NoticeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 대시보드 — 레벨(A 중앙본부·T 영업본부·L 영업점)별 범위, 상태 판정, 전 영업일·일평균 거래량(2026-11-16 사용자 결정, 잠정 Q-25·26).
 * 2026-11-16(월) 10:00 기준: 전 영업일 = 11-13(금).
 */
class DashboardServiceTest {

    private final DashboardMapper mapper = mock(DashboardMapper.class);
    private final DashboardService service = new DashboardService(mapper, mock(NoticeService.class), "A", "T", "");

    @BeforeEach
    void setUp() {
        service.setClock(Clock.fixed(ZonedDateTime.of(2026, 11, 16, 10, 0, 0, 0, ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul")));
        when(mapper.selectOfficeOfDept("D080")).thenReturn(new DashOffice("000080", "테크부문", "000100"));
        when(mapper.selectOfficeOfDept("D100")).thenReturn(new DashOffice("000100", "서울본부", null));
        when(mapper.selectOffice("000100")).thenReturn(new DashOffice("000100", "서울본부", null));
        when(mapper.selectOffice("000080")).thenReturn(new DashOffice("000080", "테크부문", "000100"));
        when(mapper.selectRegionOffices("000100")).thenReturn(List.of(new DashOffice("000080", "테크부문", "000100"), new DashOffice("000120", "종로지점", "000100")));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void login(String auth, String samuso) {
        LoginUser u = new LoginUser("123456789", "홍길동", samuso, "부서", "000080", "000080", "테크부문", auth, "A".equals(auth) ? "Y" : "N");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of()));
    }

    private static DashMachine m(String powerOff, String useStop, String errMode) {
        DashMachine x = new DashMachine();
        x.setPowerOff(powerOff);
        x.setUseStop(useStop);
        x.setErrMode(errMode);
        x.setStatusDetail("현금걸림");
        return x;
    }

    private static String code(Throwable t) {
        return ((BusinessException) t).getCode();
    }

    @Test
    void levelOfAuthCode() {
        assertThat(DashboardRules.levelOf("A", "A", "T")).isEqualTo(DashboardRules.CENTRAL);
        assertThat(DashboardRules.levelOf(" T ", "A", "T,R")).isEqualTo(DashboardRules.REGION);
        assertThat(DashboardRules.levelOf("R", "A", "T,R")).isEqualTo(DashboardRules.REGION);
        assertThat(DashboardRules.levelOf("L", "A", "T")).isEqualTo(DashboardRules.BRANCH);
        assertThat(DashboardRules.levelOf(null, "A", "T")).isEqualTo(DashboardRules.BRANCH);
        assertThat(DashboardRules.levelOf("X", "A", "T")).isEqualTo(DashboardRules.BRANCH);
    }

    @Test
    void statusOrderPowerOffThenStopThenError() {
        assertThat(DashboardRules.statusOf("Y", "Y", "Y")).isEqualTo(DashboardRules.OFF);
        assertThat(DashboardRules.statusOf("N", "Y", "Y")).isEqualTo(DashboardRules.FIX);
        assertThat(DashboardRules.statusOf(null, null, "Y")).isEqualTo(DashboardRules.ERR);
        assertThat(DashboardRules.statusOf(null, null, null)).isEqualTo(DashboardRules.OK);
    }

    @Test
    void prevBizDaySkipsWeekendAndHolidays() {
        LocalDate mon = LocalDate.of(2026, 11, 16);
        assertThat(DashboardRules.prevBizDay(mon, Set.of())).isEqualTo(LocalDate.of(2026, 11, 13));
        assertThat(DashboardRules.prevBizDay(mon, Set.of("20261113"))).isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(DashboardRules.dealAvg(125, 2)).isEqualTo(63);
        assertThat(DashboardRules.dealAvg(10, 0)).isZero();
    }

    @Test
    void centralSeesAllRegionsAndAnyOffice() {
        login("A", "D080");
        when(mapper.selectRegions()).thenReturn(List.of(new DashOffice("000100", "서울본부", null)));
        DashScope s = service.scope();
        assertThat(s.getLevel()).isEqualTo(DashboardRules.CENTRAL);
        assertThat(s.isRegionEditable()).isTrue();
        assertThat(s.isOfficeEditable()).isTrue();
        assertThat(s.getRegions()).hasSize(1);

        when(mapper.selectMachines(isNull(), isNull(), isNull())).thenReturn(List.of(m("Y", null, null), m(null, "Y", null), m(null, null, "Y"), m(null, null, null)));
        when(mapper.selectHolidays(anyString(), anyString())).thenReturn(List.of());
        when(mapper.selectDealCount(isNull(), isNull(), eq("20261113"))).thenReturn(250L);
        DashSummary r = service.summary(null, null);
        assertThat(r.getTitle()).isEqualTo("전국");
        assertThat(r.getTotal()).isEqualTo(4);
        assertThat(r.getStatusCount()).containsEntry("OK", 1).containsEntry("ERR", 1).containsEntry("FIX", 1).containsEntry("OFF", 1);
        assertThat(r.getDealDate()).isEqualTo("20261113");
        assertThat(r.getDealAvg()).isEqualTo(63);
        assertThat(r.getBaseDtm()).isEqualTo("20261116100000");
        // 장애가 아니면 상태상세를 비우고, 판정용 원천 값은 응답에서 뺀다
        assertThat(r.getMachines().get(2).getStatusDetail()).isEqualTo("현금걸림");
        assertThat(r.getMachines().get(0).getStatusDetail()).isNull();
        assertThat(r.getMachines().get(0).getPowerOff()).isNull();
    }

    @Test
    void regionUserIsFixedToOwnRegion() {
        login("T", "D100");
        DashScope s = service.scope();
        assertThat(s.getLevel()).isEqualTo(DashboardRules.REGION);
        assertThat(s.isRegionEditable()).isFalse();
        assertThat(s.isOfficeEditable()).isTrue();
        assertThat(s.getDefaultRegion()).isEqualTo("000100");

        when(mapper.selectMachines(any(), any(), any())).thenReturn(List.of());
        // 다른 본부를 보내도 내 본부로
        DashSummary r = service.summary("000200", null);
        assertThat(r.getTitle()).isEqualTo("서울본부");
        verify(mapper).selectMachines(eq("000100"), isNull(), isNull());
        // 본부 밖 사무소 403
        assertThatThrownBy(() -> service.summary(null, "000310")).isInstanceOf(ForbiddenException.class)
                .satisfies(t -> assertThat(code(t)).isEqualTo("DSH_001"));
        assertThatThrownBy(() -> service.offices("000200")).isInstanceOf(ForbiddenException.class);
        // 소속 사무소는 됨
        service.summary(null, "120");
        verify(mapper).selectMachines(isNull(), eq("000120"), isNull());
    }

    @Test
    void branchUserSeesOnlyOwnOffice() {
        login("L", "D080");
        DashScope s = service.scope();
        assertThat(s.getLevel()).isEqualTo(DashboardRules.BRANCH);
        assertThat(s.isRegionEditable()).isFalse();
        assertThat(s.isOfficeEditable()).isFalse();
        assertThat(s.getDefaultOfcCode()).isEqualTo("000080");
        assertThat(s.getDefaultRegion()).isEqualTo("000100");

        when(mapper.selectMachines(any(), any(), any())).thenReturn(List.of());
        service.summary("000100", null);
        verify(mapper).selectMachines(isNull(), eq("000080"), isNull());
        assertThatThrownBy(() -> service.summary(null, "000120")).isInstanceOf(ForbiddenException.class)
                .satisfies(t -> assertThat(code(t)).isEqualTo("DSH_001"));
        assertThatThrownBy(() -> service.offices("000100")).isInstanceOf(ForbiddenException.class);
    }
}
