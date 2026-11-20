package kr.co.nh.atms.web.common.uselog;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.web.common.CurrentUser;
import kr.co.nh.atms.web.common.auth.LoginUser;
import kr.co.nh.atms.web.common.auth.MenuAuthService;
import kr.co.nh.atms.web.common.auth.RequireMenuAuth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

/**
 * 사용기록 자동 기록 — TB_SCR_CUD_HIST (2026-11-17 사용자 결정, 예전 T_USER_ACTION_LOG 행위이력을 대체).
 *
 * <pre>
 *   대상 = kr.co.nh.atms.web 아래 *Controller 의 public 메서드 중 이름이 select·insert·update·delete 로 시작하는 것
 *          (이름이 곧 정책 — 접두어가 아니면 기록되지 않는다), {@link ExcludeCrudLog} 가 붙은 메서드·클래스는 뺀다
 *   요청 ─▶ [사용자 없음 → 기록 없이 통과] ─▶ 인자 JSON 을 미리 떠 둠 ─▶ SQL 모으기 시작 ─▶ 컨트롤러(권한 검사 포함)
 *        ─▶ 성공 Y / 예외 N + 오류코드(원래 예외 그대로 다시 던짐) ─▶ SQL 모으기 끝 ─▶ 다른 트랜잭션으로 INSERT(실패해도 업무 응답은 그대로)
 * </pre>
 * 메뉴ID = 메서드의 {@link RequireMenuAuth#screen()} 화면키 우선, 없으면 요청 헤더 {@code X-Screen-Key} → 메뉴경로 → TB_SCR_MENU.
 * 맨 바깥에서 감싼다({@code HIGHEST_PRECEDENCE}) — 권한 없음(403)도 실패로 남는다.
 * 입력검사(@Valid) 실패는 컨트롤러에 들어오기 전이라 기록되지 않는다.
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class CrudLogAspect {

    /** 화면키 헤더 모양(영문·숫자·점) */
    private static final Pattern SCREEN_KEY = Pattern.compile("^[A-Za-z0-9.]{1,60}$");

    private final CrudLogWriter writer;
    private final ObjectMapper objectMapper;

    @Value("${atms.uselog.enabled:true}")
    private boolean enabled;

    @Around("(execution(public * kr.co.nh.atms.web..*Controller.select*(..))"
            + " || execution(public * kr.co.nh.atms.web..*Controller.insert*(..))"
            + " || execution(public * kr.co.nh.atms.web..*Controller.update*(..))"
            + " || execution(public * kr.co.nh.atms.web..*Controller.delete*(..)))"
            + " && !@annotation(kr.co.nh.atms.web.common.uselog.ExcludeCrudLog)"
            + " && !@within(kr.co.nh.atms.web.common.uselog.ExcludeCrudLog)")
    public Object around(ProceedingJoinPoint jp) throws Throwable {
        if (!enabled) return jp.proceed();
        HttpServletRequest req = currentRequest();
        LoginUser user = CurrentUser.get();
        if (req == null || user == null) return jp.proceed();

        Method method = ((MethodSignature) jp.getSignature()).getMethod();
        String cudType = CrudLogRules.cudTypeOf(method.getName());
        if (cudType == null) return jp.proceed();

        String args = null;
        try {
            args = CrudLogRules.argsJson(objectMapper, req.getParameterMap(), requestBodyArg(jp, method));
        } catch (Exception e) {
            log.warn("[사용기록] 인자 직렬화 실패: {}", e.toString());
        }
        RequireMenuAuth ma = AnnotatedElementUtils.findMergedAnnotation(method, RequireMenuAuth.class);
        String screenKey = ma != null ? ma.screen() : null;

        String succ = "Y";
        String err = null;
        CrudLogContext.start();
        try {
            return jp.proceed();
        } catch (Throwable t) {
            succ = "N";
            err = t instanceof BusinessException b ? b.getCode() : t.getClass().getSimpleName();
            throw t;
        } finally {
            String sql = CrudLogContext.stop();
            try {
                CrudLogEntry e = entryOf(req, user, cudType, screenKey, err, args);
                e.setSuccYn(succ);
                e.setSqlText(sql);
                writer.write(e);
            } catch (Exception ex) {
                log.error("[사용기록] 저장 실패(업무 응답은 정상): uri={} user={} err={}", req.getRequestURI(), user.perNo(), ex.toString());
            }
        }
    }

    /**
     * 기록 한 행의 공통 부분(사용자·메뉴경로·URI·PARAMS). 엑셀 기록 API 도 쓴다.
     *
     * @param screenKey 화면키(비면 요청 헤더 X-Screen-Key)
     */
    public static CrudLogEntry entryOf(HttpServletRequest req, LoginUser user, String cudType, String screenKey, String err, String argsJson) {
        CrudLogEntry e = new CrudLogEntry();
        e.setDeptCode(user.deptCode());
        e.setOfcCode(user.ofcCode());
        e.setPerNo(user.perNo());
        String key = screenKey != null && !screenKey.isBlank() ? screenKey : req.getHeader("X-Screen-Key");
        e.setMenuPath(key != null && SCREEN_KEY.matcher(key.trim()).matches() ? MenuAuthService.menuPathOf(key) : null);
        e.setCudType(cudType);
        e.setSuccYn("Y");
        e.setUri(CrudLogRules.cutBytes(pathOf(req), CrudLogRules.URI_MAX_BYTES));
        e.setParams(CrudLogRules.params(req.getRemoteAddr(), user.auth(), err, argsJson));
        return e;
    }

    /** @RequestBody 인자(파일·서블릿 객체는 뺌) */
    private static Object requestBodyArg(ProceedingJoinPoint jp, Method method) {
        Annotation[][] anns = method.getParameterAnnotations();
        Object[] args = jp.getArgs();
        for (int i = 0; i < anns.length && i < args.length; i++) {
            for (Annotation a : anns[i]) {
                if (a instanceof RequestBody) {
                    Object v = args[i];
                    if (v instanceof MultipartFile || v instanceof HttpServletRequest || v instanceof HttpServletResponse) return null;
                    return v;
                }
            }
        }
        return null;
    }

    private static String pathOf(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String ctx = req.getContextPath();
        return (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) ? uri.substring(ctx.length()) : uri;
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a ? a.getRequest() : null;
    }
}
