package kr.co.nh.atms.web.dashboard.service;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.common.CurrentUser;
import kr.co.nh.atms.web.common.Dates;
import kr.co.nh.atms.web.common.ForbiddenException;
import kr.co.nh.atms.web.common.OfficeCode;
import kr.co.nh.atms.web.common.auth.LoginUser;
import kr.co.nh.atms.web.dashboard.dto.DashMachine;
import kr.co.nh.atms.web.dashboard.dto.DashOffice;
import kr.co.nh.atms.web.dashboard.dto.DashScope;
import kr.co.nh.atms.web.dashboard.dto.DashSummary;
import kr.co.nh.atms.web.dashboard.mapper.DashboardMapper;
import kr.co.nh.atms.web.lookup.service.LookupRules;
import kr.co.nh.atms.web.notice.dto.Notice;
import kr.co.nh.atms.web.notice.dto.NoticeSearchDto;
import kr.co.nh.atms.web.notice.service.NoticeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 대시보드(main.dashboard, 홈) — API 계약 §10. 규칙은 {@link DashboardRules}(2026-11-16 사용자 결정, 잠정 Q-25·26·27).
 * <ul>
 *   <li>범위: 중앙본부 = 아무 영업본부·사무소, 영업본부 = 내 본부 안(지역 고정), 영업점 = 내 사무소만. 범위 밖 요청은 403 DSH_001</li>
 *   <li>공지 = 공지사항 모듈(NoticeService)을 그대로 불러 같은 '보이는 범위' 규칙(전체 공지 + 내 사무소, 관리자 모두)</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    /** 대시보드 공지 건수(사진 설계 6건) */
    public static final int NOTICE_COUNT = 6;

    private final DashboardMapper mapper;
    private final NoticeService noticeService;
    private final String centralCodes;
    private final String regionCodes;
    private final String vendorCodeType;
    /** 지금 시각(테스트에서 setClock 으로 고정) */
    private Clock clock = Clock.systemDefaultZone();

    public DashboardService(DashboardMapper mapper, NoticeService noticeService,
                            @Value("${atms.dashboard.level.central:A}") String centralCodes,
                            @Value("${atms.dashboard.level.region:T}") String regionCodes,
                            @Value("${atms.lookup.vendor-code-type:}") String vendorCodeTypeSetting) {
        this.mapper = mapper;
        this.noticeService = noticeService;
        this.centralCodes = centralCodes;
        this.regionCodes = regionCodes;
        this.vendorCodeType = LookupRules.vendorCodeType(vendorCodeTypeSetting);
    }

    /** 테스트용 — 지금 시각 고정 */
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    /** 내 레벨·내 사무소·지역 콤보 목록·처음 고를 값 */
    public DashScope scope() {
        Ctx c = ctx();
        DashScope s = new DashScope();
        s.setLevel(c.level);
        s.setMyOffice(c.my);
        switch (c.level) {
            case DashboardRules.CENTRAL -> {
                s.setRegions(mapper.selectRegions());
                s.setRegionEditable(true);
                s.setOfficeEditable(true);
            }
            case DashboardRules.REGION -> {
                s.setRegions(c.region == null ? List.of() : List.of(c.region));
                s.setDefaultRegion(c.region == null ? null : c.region.getOfcCode());
                s.setOfficeEditable(true);
            }
            default -> {
                DashOffice r = c.my == null || c.my.getRegionCode() == null ? null : mapper.selectOffice(c.my.getRegionCode());
                s.setRegions(r == null ? List.of() : List.of(r));
                s.setDefaultRegion(r == null ? null : r.getOfcCode());
                s.setDefaultOfcCode(c.my == null ? null : c.my.getOfcCode());
            }
        }
        return s;
    }

    /** 사무소 구분 콤보 — 영업본부 소속 사무소(범위 밖 본부는 403) */
    public List<DashOffice> offices(String region) {
        String r = OfficeCode.norm(region);
        if (r == null) return List.of();
        Ctx c = ctx();
        if (DashboardRules.REGION.equals(c.level) && (c.region == null || !r.equals(c.region.getOfcCode()))) throw new ForbiddenException("DSH_001", r);
        if (DashboardRules.BRANCH.equals(c.level)) throw new ForbiddenException("DSH_001", r);
        return mapper.selectRegionOffices(r);
    }

    /** [조회] — 운영 대수·거래량·상태·기기 목록 */
    public DashSummary summary(String regionParam, String ofcParam) {
        Ctx c = ctx();
        String region = OfficeCode.norm(regionParam);
        String ofc = OfficeCode.norm(ofcParam);
        switch (c.level) {
            case DashboardRules.CENTRAL -> { /* 모두 허용 */ }
            case DashboardRules.REGION -> {
                if (c.region == null) throw new ForbiddenException("DSH_001", "-");
                region = c.region.getOfcCode();
                if (ofc != null && !ofc.equals(region) && !inRegion(region, ofc)) throw new ForbiddenException("DSH_001", ofc);
            }
            default -> {
                if (c.my == null) throw new ForbiddenException("DSH_001", "-");
                if (ofc != null && !ofc.equals(c.my.getOfcCode())) throw new ForbiddenException("DSH_001", ofc);
                ofc = c.my.getOfcCode();
                region = null;
            }
        }
        if (ofc != null) region = null; // 사무소 하나를 고르면 그 사무소 기준

        DashSummary s = new DashSummary();
        if (ofc != null) {
            DashOffice o = mapper.selectOffice(ofc);
            s.setTitle(o == null ? ofc : o.getKanm());
            s.setTitleKind("BRANCH");
            s.setTitleCode(ofc);
        } else if (region != null) {
            DashOffice o = mapper.selectOffice(region);
            s.setTitle(o == null ? region : o.getKanm());
            s.setTitleKind("REGION");
            s.setTitleCode(region);
        } else {
            s.setTitle("전국");
            s.setTitleKind("ALL");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        s.setBaseDtm(now.format(Dates.DT14));

        List<DashMachine> list = mapper.selectMachines(region, ofc, vendorCodeType);
        Map<String, Integer> cnt = new LinkedHashMap<>();
        for (String k : List.of(DashboardRules.OK, DashboardRules.ERR, DashboardRules.FIX, DashboardRules.OFF)) cnt.put(k, 0);
        for (DashMachine m : list) {
            String st = DashboardRules.statusOf(m.getPowerOff(), m.getUseStop(), m.getErrMode());
            m.setStatus(st);
            if (!DashboardRules.ERR.equals(st)) m.setStatusDetail(null);
            m.setPowerOff(null);
            m.setUseStop(null);
            m.setErrMode(null);
            cnt.merge(st, 1, Integer::sum);
        }
        s.setMachines(list);
        s.setStatusCount(cnt);
        s.setTotal(list.size());

        LocalDate today = now.toLocalDate();
        LocalDate from = today.minusDays(DashboardRules.MAX_LOOKBACK_DAYS);
        LocalDate deal = DashboardRules.prevBizDay(today, new HashSet<>(mapper.selectHolidays(DashboardRules.ymd(from), DashboardRules.ymd(today))));
        String dealDt = DashboardRules.ymd(deal);
        s.setDealDate(dealDt);
        s.setDealAvg(DashboardRules.dealAvg(list.isEmpty() ? 0 : mapper.selectDealCount(region, ofc, dealDt), list.size()));
        return s;
    }

    /** 공지 최신 6건 — 공지사항 모듈의 목록(보이는 범위 규칙 그대로) */
    public List<Notice> notices() {
        NoticeSearchDto q = new NoticeSearchDto();
        q.setPage(1);
        q.setSize(NOTICE_COUNT);
        PageResult<Notice> r = noticeService.search(q);
        return r.getContent();
    }

    /** 공지 상세(조회수 +1, 앞뒤 글) — 공지사항 모듈 그대로 */
    @Transactional
    public Notice notice(long no) {
        return noticeService.get(no, true);
    }

    /* ---------------- 내부 ---------------- */

    private boolean inRegion(String region, String ofc) {
        return mapper.selectRegionOffices(region).stream().anyMatch(o -> ofc.equals(o.getOfcCode()));
    }

    private record Ctx(String level, DashOffice my, DashOffice region) {}

    private Ctx ctx() {
        LoginUser u = CurrentUser.get();
        String level = DashboardRules.levelOf(u == null ? null : u.auth(), centralCodes, regionCodes);
        DashOffice my = u == null || u.samuso() == null ? null : mapper.selectOfficeOfDept(u.samuso());
        DashOffice region = DashboardRules.REGION.equals(level) && my != null ? my : null; // 영업본부 사용자의 본부 = 내 사무소(잠정 Q-25)
        return new Ctx(level, my, region);
    }
}
