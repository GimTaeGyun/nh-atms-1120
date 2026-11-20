package kr.co.nh.atms.web.common.uselog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kr.co.nh.atms.web.common.DbBytes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 사용기록 규칙 한 곳(순수 함수 — 단위 테스트 {@code CrudLogRulesTest}). 2026-11-17 사용자 결정.
 *
 * <ul>
 *   <li><b>수행작업(CUD_TYPE)</b> = 컨트롤러 메서드 이름 접두어: select → R, insert → C, update → U, delete → D. 엑셀 기록 API → E.
 *       <b>잠정 Q-30</b>: 정본 주석은 'C등록 U수정 D삭제' 3가지 — R(조회)·E(엑셀)는 사용자 결정으로 더 쓴다.</li>
 *   <li><b>PARAMS</b>(VARCHAR(500)) = 머리 {@code ip=<IP> auth=<권한> [err=<오류코드>]} + 공백 + 인자 JSON({@code {"query":{…},"body":…}}).
 *       IP·권한은 정본에 컬럼이 없어 여기에 적는다(같은 날 결정). 비밀번호·토큰 류 키는 값을 지우고(***), 나머지는 원문.
 *       DB 바이트({@link DbBytes#length}) 500 을 넘으면 표시 없이 자른다. 머리를 앞에 두어 잘려도 IP·권한·오류코드는 남는다.</li>
 *   <li><b>HIST_ID</b>(NUMBER(19)) = 시각 yyyyMMddHHmmss(14) + 서버번호(1) + 초당 일련(4) — {@link HistIdGenerator}.</li>
 * </ul>
 */
public final class CrudLogRules {
    private CrudLogRules() {}

    public static final String READ = "R";
    public static final String CREATE = "C";
    public static final String UPDATE = "U";
    public static final String DELETE = "D";
    public static final String EXCEL = "E";

    /** PARAMS VARCHAR(500) — 바이트 */
    public static final int PARAMS_MAX_BYTES = 500;
    /** URI VARCHAR(200) — 바이트 */
    public static final int URI_MAX_BYTES = 200;

    /** 값 자체를 남기면 안 되는 키(비밀번호·토큰 류) */
    private static final Pattern SECRET_KEY = Pattern.compile("(?i)(password|passwd|pwd|secret|token|ticket|authorization|otp|pin)");
    /** IP 검색어: 숫자·점·콜론(IPv6)·a~f 만 */
    private static final Pattern IP = Pattern.compile("^[0-9A-Fa-f.:]{1,45}$");

    /** 메서드 이름 → 수행작업. 접두어가 아니면 null(기록 안 함) */
    public static String cudTypeOf(String methodName) {
        if (methodName == null) return null;
        if (methodName.startsWith("select")) return READ;
        if (methodName.startsWith("insert")) return CREATE;
        if (methodName.startsWith("update")) return UPDATE;
        if (methodName.startsWith("delete")) return DELETE;
        return null;
    }

    /** 수행작업 코드 → 화면 글자 */
    public static final Map<String, String> CUD_NAME = new LinkedHashMap<>();
    static {
        CUD_NAME.put(READ, "조회");
        CUD_NAME.put(CREATE, "등록");
        CUD_NAME.put(UPDATE, "수정");
        CUD_NAME.put(DELETE, "삭제");
        CUD_NAME.put(EXCEL, "엑셀");
    }

    public static boolean isCudType(String v) {
        return v != null && CUD_NAME.containsKey(v);
    }

    public static boolean isIp(String v) {
        return v != null && IP.matcher(v).matches();
    }

    /** PARAMS 문자열. ip·auth 가 없으면 '-', err 는 있을 때만, 인자가 없으면 {} */
    public static String params(String ip, String auth, String err, String argsJson) {
        StringBuilder sb = new StringBuilder();
        sb.append("ip=").append(blank(ip) ? "-" : ip.trim());
        sb.append(" auth=").append(blank(auth) ? "-" : auth.trim());
        if (!blank(err)) sb.append(" err=").append(err.trim());
        sb.append(' ').append(blank(argsJson) ? "{}" : argsJson);
        return cutBytes(sb.toString(), PARAMS_MAX_BYTES);
    }

    /** PARAMS 머리에서 값 하나(ip·auth·err). 없거나 '-' 면 null */
    public static String headOf(String params, String key) {
        if (params == null) return null;
        int json = params.indexOf(" {");
        String head = json < 0 ? params : params.substring(0, json);
        for (String t : head.split(" ")) {
            if (t.startsWith(key + "=")) {
                String v = t.substring(key.length() + 1);
                return v.isEmpty() || "-".equals(v) ? null : v;
            }
        }
        return null;
    }

    /** IP 검색 LIKE 값 — PARAMS 가 {@code ip=<값> } 로 시작하는 행(완전일치) */
    public static String ipLike(String ip) {
        return "ip=" + ip.trim() + " %";
    }

    /** 쿼리 파라미터 + 본문을 한 JSON 으로. {"query":{...},"body":...}, 둘 다 없으면 null */
    public static String argsJson(ObjectMapper om, Map<String, String[]> query, Object body) {
        ObjectNode root = om.createObjectNode();
        if (query != null && !query.isEmpty()) {
            ObjectNode q = root.putObject("query");
            for (Map.Entry<String, String[]> e : query.entrySet()) {
                String[] vs = e.getValue();
                if (vs == null || vs.length == 0) { q.putNull(e.getKey()); continue; }
                if (vs.length == 1) q.put(e.getKey(), hide(e.getKey(), vs[0]));
                else { ArrayNode a = q.putArray(e.getKey()); for (String v : vs) a.add(hide(e.getKey(), v)); }
            }
        }
        if (body != null) {
            try {
                root.set("body", hideTree(om.valueToTree(body)));
            } catch (Exception ex) {
                root.put("body", body.getClass().getSimpleName());
            }
        }
        return root.isEmpty() ? null : root.toString();
    }

    /** 비밀번호·토큰 류 키면 값을 지운다(***), 나머지는 원문 */
    public static String hide(String key, String value) {
        if (value == null) return null;
        return key != null && SECRET_KEY.matcher(key).find() ? "***" : value;
    }

    /** JSON 트리를 돌며 비밀 키 값 지움(원본을 바꾼다) */
    static JsonNode hideTree(JsonNode node) {
        if (node == null) return null;
        if (node.isObject()) {
            ObjectNode o = (ObjectNode) node;
            for (Map.Entry<String, JsonNode> e : o.properties()) {
                JsonNode v = e.getValue();
                if (v.isValueNode()) {
                    if (!v.isNull() && SECRET_KEY.matcher(e.getKey()).find()) o.put(e.getKey(), "***");
                } else {
                    hideTree(v);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) hideTree(child);
        }
        return node;
    }

    /** DB 바이트 기준으로 자른다(글자 중간에서 자르지 않음, 표시 없음) */
    public static String cutBytes(String s, int maxBytes) {
        if (s == null || DbBytes.length(s) <= maxBytes) return s;
        int bytes = 0;
        int end = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            int len = cp < 0x80 ? 1 : 2;
            if (bytes + len > maxBytes) break;
            bytes += len;
            i += Character.charCount(cp);
            end = i;
        }
        return s.substring(0, end);
    }

    /** 이력번호 = 시각 14 + 서버번호 1 + 일련 4 (19자리 숫자) */
    public static long histId(String now14, int node, int seq) {
        if (now14 == null || now14.length() != 14) throw new IllegalArgumentException("now14");
        if (node < 0 || node > 9) throw new IllegalArgumentException("node");
        if (seq < 0 || seq > 9999) throw new IllegalArgumentException("seq");
        return Long.parseLong(now14 + node + String.format("%04d", seq));
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
