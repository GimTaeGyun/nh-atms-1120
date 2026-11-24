package kr.co.nh.atms.web.jnal.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.web.common.Dates;
import kr.co.nh.atms.web.common.OfficeCode;
import kr.co.nh.atms.web.jnal.dto.Jnal;
import kr.co.nh.atms.web.jnal.dto.JnalCheckKey;
import kr.co.nh.atms.web.jnal.dto.JnalSearchDto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 저널조회 규칙 한 곳(2026-11-24 사용자 결정, As-Is 저널조회 화면 스크립트·인클루드 근거) — 단위 테스트 {@code JnalRulesTest}.
 * <ul>
 *   <li>사무소·기번·저널일자 필수(VAL_002), 형식(VAL_003), 시작 ≤ 끝·최대 {@value #MAX_DAYS}일(VAL_001) — As-Is "사무소,기번 및 일자는 필수", "최대 2일"</li>
 *   <li>2차 파기: 시작일 ≤ 오늘 − 5년이면 JNL_001 — 인클루드 {@code chkLim}</li>
 *   <li>조회 허가: 권한이 A·L 이 아니고 시작일 ≤ 오늘 − 3개월(1차 파기)이면 JNL_002 — As-Is {@code chkPermitINQ}(사유 + 책임자 승인 + 기록)는
 *       TO-BE 승인 방식·기록 테이블이 없어 <b>막기만</b> 한다(잠정 Q-35)</li>
 *   <li>사무소: 권한 A·T·L 은 고른 사무소, 그 밖은 내 사무소(As-Is "T, L 권한 유저에게 사무소 변경 권한 부여", 관리자 A 는 공통 콤보가 엶)</li>
 *   <li>IT부문(내 사무소 {@value #IT_OFC_CODE}) 이고 관리자가 아니면 조회조건(계좌·수표·명세표)을 쓰지 않는다 — As-Is 2020.09 개인정보 조회 제한</li>
 *   <li>테이블: TB_JL_JNAL(+_CHK)만 읽는다(2026-11-24 사용자 결정). As-Is 의 ILM 분기({@code chkIlmDate}, 주석 '2차 파기 대상 관련 쿼리 분리')는
 *       2차 파기(5년) 경계라 위 JNL_001 에 먼저 막힌다 — TB_JL_JNAL_ILM 은 저널 2차 파기 조회 화면 몫(Q-34)</li>
 * </ul>
 */
public final class JnalRules {
    private JnalRules() {}

    /** 한 번에 볼 수 있는 최대 일수(양끝 포함) */
    public static final int MAX_DAYS = 2;
    /** 1차 파기(조회 허가) 개월 수 — 잠정 Q-35 */
    public static final int DESTROY1_MONTHS = 3;
    /** 2차 파기 년 수 */
    public static final int DESTROY2_YEARS = 5;
    /** IT부문 사무소코드(As-Is 개인정보 조회 제한 대상) */
    public static final String IT_OFC_CODE = "000080";

    public static final String FIELD_ACCT = "ACCT";
    public static final String FIELD_CHK = "CHK";
    public static final String FIELD_DTLS = "DTLS";

    private static final Set<String> JNAL_TYPES = Set.of("22", "33", "11", "44", "55");
    private static final Set<String> PROC_RESTS = Set.of("01", "02", "97", "98", "99");
    private static final Set<String> TRANSFER = Set.of("9927", "9937", "9947", "9957");
    private static final Set<String> TRANSFER_INQ = Set.of("9929", "9939", "9949", "9959");
    private static final Pattern DTLS_RANGE = Pattern.compile("^(\\d{0,4})(?:-(\\d{0,4}))?$");
    private static final Pattern KEYWORD = Pattern.compile("^[0-9A-Za-z-]+$");
    private static final Pattern HHMM = Pattern.compile("^([01]\\d|2[0-3])[0-5]\\d$");

    /** 권한코드 첫 글자가 A·T·L 이면 사무소를 고를 수 있다 */
    public static boolean canPickOffice(String auth) {
        String a = first(auth);
        return "A".equals(a) || "T".equals(a) || "L".equals(a);
    }

    /** 3개월 넘은 저널을 사유·승인 없이 볼 수 있는 권한(A·L) */
    public static boolean canSeeOld(String auth, boolean admin) {
        String a = first(auth);
        return admin || "A".equals(a) || "L".equals(a);
    }

    /** IT부문 비관리자 — 조회조건(계좌·수표·명세표)을 쓰지 않는다 */
    public static boolean itRestricted(String myOfc, boolean admin) {
        if (admin || trimToNull(myOfc) == null) return false;
        return IT_OFC_CODE.equals(OfficeCode.norm(myOfc));
    }

    public static void normalize(JnalSearchDto s, String auth, boolean admin, String myOfc, String today) {
        s.setOfcCode(office(s.getOfcCode(), auth, admin, myOfc));
        s.setMcnNo(mcnNo(s.getMcnNo()));

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

        destroyLimits(from, auth, admin, today);

        s.setFrTi(hhmm(s.getFromTime(), "0000", "fromTime") + "00");
        s.setToTi(hhmm(s.getToTime(), "2359", "toTime") + "59");

        String type = trimToNull(s.getJnalType());
        if (type != null && !JNAL_TYPES.contains(type)) throw new BusinessException("VAL_003", "jnalType");
        s.setJnalType(type);
        String rest = trimToNull(s.getJrnlProcRest());
        if (rest != null && !PROC_RESTS.contains(rest)) throw new BusinessException("VAL_003", "jrnlProcRest");
        s.setJrnlProcRest(rest);

        search(s, itRestricted(myOfc, admin));
    }

    /** 수표목록 키 정리 — 사무소·기번·일자 규칙은 목록과 같다(다른 사무소·파기 대상 저널을 키로 직접 읽지 못하게) */
    public static void normalizeKey(JnalCheckKey k, String auth, boolean admin, String myOfc, String today) {
        k.setOfcCode(office(k.getOfcCode(), auth, admin, myOfc));
        k.setMcnNo(mcnNo(k.getMcnNo()));
        String dt = trimToNull(k.getJnalDt());
        if (dt == null) throw new BusinessException("VAL_002", "jnalDt");
        if (!isYmd(dt)) throw new BusinessException("VAL_003", "jnalDt");
        String seq = trimToNull(k.getJnalSeqno());
        if (seq == null) throw new BusinessException("VAL_002", "jnalSeqno");
        if (seq.length() > 6) throw new BusinessException("VAL_003", "jnalSeqno");
        destroyLimits(dt, auth, admin, today);
        k.setJnalDt(dt);
        k.setJnalSeqno(seq);
    }

    /** 2차 파기(5년) JNL_001, 1차 파기(3개월) 조회 허가 JNL_002 — 인클루드 chkLim·chkPermitINQ */
    private static void destroyLimits(String from, String auth, boolean admin, String today) {
        LocalDate t = Dates.parseYmd(today);
        String limit2 = t.minusYears(DESTROY2_YEARS).format(Dates.YMD);
        if (from.compareTo(limit2) <= 0) throw new BusinessException("JNL_001", Dates.dash10(limit2));
        String limit1 = t.minusMonths(DESTROY1_MONTHS).format(Dates.YMD);
        if (from.compareTo(limit1) <= 0 && !canSeeOld(auth, admin)) throw new BusinessException("JNL_002", Dates.dash10(limit1));
    }

    private static String office(String requested, String auth, boolean admin, String myOfc) {
        String ofc = canPickOffice(auth) || admin ? OfficeCode.norm(requested) : OfficeCode.norm(myOfc);
        if (ofc == null) throw new BusinessException("VAL_002", "ofcCode");
        return ofc;
    }

    private static String mcnNo(String v) {
        String mcn = trimToNull(v);
        if (mcn == null) throw new BusinessException("VAL_002", "mcnNo");
        if (mcn.length() > 3 || !mcn.chars().allMatch(Character::isDigit)) throw new BusinessException("VAL_003", "mcnNo");
        return "0".repeat(3 - mcn.length()) + mcn;
    }

    /** 조회조건·검색어 → schData1·2. 저널구분에 없는 조회조건은 VAL_003(As-Is 콤보 setCB_search_if) */
    private static void search(JnalSearchDto s, boolean restricted) {
        String field = upper(s.getField());
        // 공백은 늘 떼고(As-Is 는 앞뒤 trim), 계좌는 하이픈도 뗀다 — 목록에 보이는 '301-12-345678' 을 그대로 붙여 넣어도 찾게
        String raw = s.getKeyword() == null ? null : s.getKeyword().replaceAll("\\s+", "");
        String keyword = trimToNull(FIELD_ACCT.equals(field) && raw != null ? raw.replace("-", "") : raw);
        s.setSchData1(null);
        s.setSchData2(null);
        if (restricted || field == null || "ALL".equals(field) || keyword == null) {
            s.setField(null);
            s.setKeyword(null);
            return;
        }
        if (!fieldsOf(s.getJnalType()).contains(field)) throw new BusinessException("VAL_003", "field");
        s.setField(field);
        s.setKeyword(keyword);
        if (FIELD_DTLS.equals(field)) {
            Matcher m = DTLS_RANGE.matcher(keyword);
            if (!m.matches()) throw new BusinessException("VAL_003", "keyword");
            String a = pad4(m.group(1));
            String b = m.group(2) == null ? a : pad4(m.group(2));
            if (a == null && b == null) throw new BusinessException("VAL_003", "keyword");
            if (a != null && b != null && a.compareTo(b) > 0) throw new BusinessException("VAL_003", "keyword");
            s.setSchData1(a);
            s.setSchData2(b);
        } else {
            if (!KEYWORD.matcher(keyword).matches()) throw new BusinessException("VAL_003", "keyword");
            s.setSchData1(keyword);
        }
    }

    /** 저널구분별 조회조건 — 전체·33 = 계좌·수표·명세표, 22 = 계좌·명세표, 11·44·55 = 명세표 */
    public static List<String> fieldsOf(String jnalType) {
        if (jnalType == null || "33".equals(jnalType)) return List.of(FIELD_ACCT, FIELD_CHK, FIELD_DTLS);
        if ("22".equals(jnalType)) return List.of(FIELD_ACCT, FIELD_DTLS);
        return List.of(FIELD_DTLS);
    }

    /**
     * As-Is rconcat — SQL 순서대로 돌며 저널구분이 55 가 아니고 명세표 안 순번(P_RN)이 1 이 아니면 바로 앞에 남긴 행에 합친다
     * (인자 글 목록에 '전인자\n후인자', 후인자 목록에 후인자, 거래금액 = 큰 값). 앞에 남긴 행이 없으면 그대로 한 행(As-Is 는 이때 오류).
     */
    public static List<Jnal> merge(List<Jnal> rows) {
        List<Jnal> out = new ArrayList<>(rows.size());
        Jnal prev = null;
        for (Jnal r : rows) {
            String rol = nz(r.getBfoComt()) + "\n" + nz(r.getAftComt());
            String dtl = nz(r.getAftComt());
            boolean follow = !"55".equals(r.getJnalType()) && r.getPRn() != null && r.getPRn() != 1;
            if (follow && prev != null) {
                prev.getRolComts().add(rol);
                prev.getDtlComts().add(dtl);
                long a = prev.getDealAmt() == null ? 0L : prev.getDealAmt();
                long b = r.getDealAmt() == null ? 0L : r.getDealAmt();
                prev.setDealAmt(Math.max(a, b));
                continue;
            }
            r.setRolComts(new ArrayList<>(List.of(rol)));
            r.setDtlComts(new ArrayList<>(List.of(dtl)));
            out.add(r);
            prev = r;
        }
        return out;
    }

    /** 거래명 — 55 = 기기운영관리, 타행이체·타행이체조회 화면번호, 그 밖 = TB_CODE_DTL(CODE_SECT '01') 코드명 */
    public static String scrNm(String jnalType, String scrNo, String codeNm) {
        if ("55".equals(jnalType)) return "기기운영관리";
        String no = trimToNull(scrNo);
        if (no != null && TRANSFER.contains(no)) return "타행이체";
        if (no != null && TRANSFER_INQ.contains(no)) return "타행이체조회";
        return codeNm;
    }

    private static String hhmm(String v, String def, String name) {
        String t = trimToNull(v);
        if (t == null) return def;
        if (!HHMM.matcher(t).matches()) throw new BusinessException("VAL_003", name);
        return t;
    }

    private static String pad4(String v) {
        if (v == null || v.isEmpty()) return null;
        return "0".repeat(4 - v.length()) + v;
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

    private static String first(String auth) {
        String a = trimToNull(auth);
        return a == null ? null : a.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private static String upper(String v) {
        String t = trimToNull(v);
        return t == null ? null : t.toUpperCase(Locale.ROOT);
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }

    private static String trimToNull(String v) {
        return v == null || v.trim().isEmpty() ? null : v.trim();
    }
}
