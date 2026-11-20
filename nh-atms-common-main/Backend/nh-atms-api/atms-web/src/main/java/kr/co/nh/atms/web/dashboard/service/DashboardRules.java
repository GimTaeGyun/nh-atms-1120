package kr.co.nh.atms.web.dashboard.service;

import kr.co.nh.atms.web.common.Dates;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * 대시보드 규칙 한 곳 — 2026-11-16 사용자 결정(추후 바뀔 수 있음 → 이 파일과 설정만 고친다).
 * <ul>
 *   <li>레벨: 로그인 권한코드(개인권한, 없으면 부서권한)로 — 기본 A = 중앙본부, T = 영업본부, L = 영업점, 그 밖 = 영업점.
 *       대응표는 설정 {@code atms.dashboard.level.central/region/branch}(쉼표로 여러 개). 잠정 Q-25</li>
 *   <li>내 사무소 = 인사 소속(TB_INSA.SAMUSO → TB_OFC_RAW.ONLINE_CODE). 영업본부 사용자의 본부 = 내 사무소,
 *       본부 소속 영업점 = TB_OFC_REG_MGT.JUNG_MOJUM 이 그 본부인 사무소(매퍼 scope). 잠정 Q-25</li>
 *   <li>기기상태(AS-IS 6000 을 4개로): 통신단절 = 전원 OFF(POWER_OFF='Y') → 보수중 = 중지모드(USE_STOP='Y') → 장애 = 장애모드(ERR_MODE='Y') → 정상.
 *       상태 행이 없으면 정상으로 본다. 잠정 Q-26</li>
 * </ul>
 */
public final class DashboardRules {
    private DashboardRules() {}

    public static final String CENTRAL = "CENTRAL";
    public static final String REGION = "REGION";
    public static final String BRANCH = "BRANCH";

    public static final String OK = "OK";
    public static final String ERR = "ERR";
    public static final String FIX = "FIX";
    public static final String OFF = "OFF";

    /** 전 영업일을 찾을 때 거슬러 올라갈 최대 일수 */
    public static final int MAX_LOOKBACK_DAYS = 14;

    /**
     * 권한코드 → 레벨(잠정 Q-25). 설정 값은 쉼표로 여러 코드를 줄 수 있다. 어느 것에도 없으면 영업점(가장 좁은 범위).
     * @param auth 로그인 권한코드(없으면 null)
     */
    public static String levelOf(String auth, String centralCodes, String regionCodes) {
        String a = auth == null ? "" : auth.trim();
        if (!a.isEmpty() && contains(centralCodes, a)) return CENTRAL;
        if (!a.isEmpty() && contains(regionCodes, a)) return REGION;
        return BRANCH;
    }

    private static boolean contains(String csv, String v) {
        if (csv == null) return false;
        for (String s : csv.split(",")) if (s.trim().equals(v)) return true;
        return false;
    }

    /** 기기상태 판정(잠정 Q-26) — 위에서부터 먼저 맞는 것 */
    public static String statusOf(String powerOff, String useStop, String errMode) {
        if (yes(powerOff)) return OFF;
        if (yes(useStop)) return FIX;
        if (yes(errMode)) return ERR;
        return OK;
    }

    private static boolean yes(String v) {
        return v != null && "Y".equals(v.trim());
    }

    /** 일평균 거래량 = 거래 건수 합 ÷ 운영 대수(반올림), 운영 대수 0 이면 0 */
    public static long dealAvg(long dealCount, int total) {
        return total <= 0 ? 0 : Math.round((double) dealCount / total);
    }

    /**
     * 전 영업일 — 오늘 바로 전날부터 거슬러 올라가 토·일요일과 휴일(TB_NMS_HDAY HDAY_YN='Y')이 아닌 첫날.
     * {@value #MAX_LOOKBACK_DAYS}일 안에 없으면 어제.
     * @param holidays 휴일 YYYYMMDD 모음
     */
    public static LocalDate prevBizDay(LocalDate today, Set<String> holidays) {
        LocalDate d = today.minusDays(1);
        for (int i = 0; i < MAX_LOOKBACK_DAYS; i++, d = d.minusDays(1)) {
            boolean weekend = d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY;
            if (!weekend && !holidays.contains(ymd(d))) return d;
        }
        return today.minusDays(1);
    }

    /** 'YYYYMMDD' — 공통 {@link Dates#YMD} 를 쓴다(같은 포맷을 다시 만들지 않음, 2026-11-16) */
    public static String ymd(LocalDate d) {
        return d.format(Dates.YMD);
    }
}
