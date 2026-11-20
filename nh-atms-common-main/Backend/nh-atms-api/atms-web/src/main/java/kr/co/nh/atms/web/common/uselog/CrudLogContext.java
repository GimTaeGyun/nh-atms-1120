package kr.co.nh.atms.web.common.uselog;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * 요청 하나 동안 실행된 SQL 보관소(요청 범위 속성 — 요청이 끝나면 함께 사라진다).
 * {@link CrudLogAspect} 가 컨트롤러 실행 직전에 {@link #start()}, 끝나면 {@link #stop()} 으로 꺼낸다.
 * 그 사이 {@link ExecutedSqlCapture} 가 실행된 SQL 을 {@link #add} 한다.
 */
final class CrudLogContext {
    private CrudLogContext() {}

    private static final String KEY = CrudLogContext.class.getName() + ".SQL";

    static void start() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        if (ra != null) ra.setAttribute(KEY, new ArrayList<String>(), RequestAttributes.SCOPE_REQUEST);
    }

    /** 모으는 중이면 목록, 아니면 null */
    @SuppressWarnings("unchecked")
    static List<String> active() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        if (ra == null) return null;
        Object v = ra.getAttribute(KEY, RequestAttributes.SCOPE_REQUEST);
        return v instanceof List ? (List<String>) v : null;
    }

    static void add(String sql) {
        List<String> list = active();
        if (list != null) list.add(sql);
    }

    /** 모으기를 끝내고 모은 SQL 을 한 글로(없으면 null). 여러 개면 빈 줄로 띄운다 */
    static String stop() {
        List<String> list = active();
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        if (ra != null) ra.removeAttribute(KEY, RequestAttributes.SCOPE_REQUEST);
        return list == null || list.isEmpty() ? null : String.join("\n\n", list);
    }
}
