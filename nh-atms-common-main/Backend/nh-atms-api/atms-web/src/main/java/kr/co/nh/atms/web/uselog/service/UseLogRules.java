package kr.co.nh.atms.web.uselog.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.web.common.Dates;
import kr.co.nh.atms.web.common.OfficeCode;
import kr.co.nh.atms.web.common.uselog.CrudLogRules;
import kr.co.nh.atms.web.uselog.dto.UseLog;
import kr.co.nh.atms.web.uselog.dto.UseLogSearchDto;

/**
 * 사용기록조회 규칙 한 곳(2026-11-17 사용자 결정) — 단위 테스트 {@code UseLogServiceTest}.
 * <ul>
 *   <li>기간 필수(VAL_002), 형식 YYYYMMDD(VAL_003), 시작 ≤ 끝·최대 {@value #MAX_DAYS}일(VAL_001)</li>
 *   <li>보이는 범위: 관리자 = 전체(사무소 칸으로 거를 수 있음), 그 밖 = 내 권한 사무소 기록만(요청 사무소 값은 버림)</li>
 *   <li>수행작업 R·C·U·D·E, 수행결과 Y·N, IP 는 숫자·점·콜론만(그 밖 VAL_003)</li>
 * </ul>
 */
public final class UseLogRules {
    private UseLogRules() {}

    /** 한 번에 볼 수 있는 최대 일수(양끝 포함) */
    public static final int MAX_DAYS = 31;

    public static void normalize(UseLogSearchDto s, boolean admin, String myOfc) {
        String from = trimToNull(s.getFromDate());
        String to = trimToNull(s.getToDate());
        if (from == null) throw new BusinessException("VAL_002", "fromDate");
        if (to == null) throw new BusinessException("VAL_002", "toDate");
        if (!isYmd(from)) throw new BusinessException("VAL_003", "fromDate");
        if (!isYmd(to)) throw new BusinessException("VAL_003", "toDate");
        long days = Dates.daysBetween(from, to);
        if (days < 1 || days > MAX_DAYS) throw new BusinessException("VAL_001", "fromDate~toDate");
        s.setFromDate(from);
        s.setToDate(to);

        String cud = upper(s.getCudType());
        if (cud != null && !CrudLogRules.isCudType(cud)) throw new BusinessException("VAL_003", "cudType");
        s.setCudType(cud);
        String succ = upper(s.getSuccYn());
        if (succ != null && !"Y".equals(succ) && !"N".equals(succ)) throw new BusinessException("VAL_003", "succYn");
        s.setSuccYn(succ);

        String ip = trimToNull(s.getIp());
        if (ip != null && !CrudLogRules.isIp(ip)) throw new BusinessException("VAL_003", "ip");
        s.setIp(ip);
        s.setIpLike(ip == null ? null : CrudLogRules.ipLike(ip));
        s.setPerNo(trimToNull(s.getPerNo()));
        s.setMenuId(trimToNull(s.getMenuId()));

        if (admin) {
            s.setOfcCode(OfficeCode.norm(s.getOfcCode()));
            s.setNone(false);
        } else {
            String mine = trimToNull(myOfc);
            s.setOfcCode(mine == null ? null : OfficeCode.norm(mine));
            s.setNone(mine == null);
        }
    }

    /** 이 사용자가 이 기록을 볼 수 있나(관리자 = 모두, 그 밖 = 내 사무소 기록) */
    public static boolean canSee(UseLog row, boolean admin, String myOfc) {
        if (admin) return true;
        String mine = trimToNull(myOfc);
        String ofc = row == null ? null : trimToNull(row.getOfcCode());
        return mine != null && ofc != null && OfficeCode.norm(mine).equals(ofc);
    }

    /** PARAMS 머리 → ip·auth·err 칸 */
    public static void splitParams(UseLog r) {
        r.setIp(CrudLogRules.headOf(r.getParams(), "ip"));
        r.setAuth(CrudLogRules.headOf(r.getParams(), "auth"));
        r.setErr(CrudLogRules.headOf(r.getParams(), "err"));
    }

    private static boolean isYmd(String v) {
        if (v.length() != 8) return false;
        try {
            Dates.parseYmd(v);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String upper(String v) {
        String t = trimToNull(v);
        return t == null ? null : t.toUpperCase(java.util.Locale.ROOT);
    }

    private static String trimToNull(String v) {
        return v == null || v.trim().isEmpty() ? null : v.trim();
    }
}
