package kr.co.nh.atms.web.common.uselog;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 실행된 SQL 잡기(MyBatis 플러그인) — 사용기록 SQL_TEXT(CLOB, 자르지 않음, 2026-11-17 사용자 결정 "모든 조회").
 * <p>
 * 컨트롤러가 실행되는 동안({@link CrudLogContext#active()} 가 있을 때) 실행된 조회·등록·수정·삭제 SQL 을
 * <b>실제로 바인딩된 값</b>을 끼워 넣은 글로 만들어 모은다. 공통 인프라 매퍼(권한 검사·현재 사용자, 패키지 {@code kr.co.nh.atms.web.common.})는 뺀다.
 * 값은 실행이 끝난 뒤 읽는다 — 감사 컬럼(AuditInterceptor 가 채움)도 보인다. 잡기 실패는 경고 로그만(업무 SQL 은 그대로).
 * mybatis-spring-boot-starter 가 Interceptor 빈을 SqlSessionFactory 에 자동으로 붙인다.
 */
@Slf4j
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class ExecutedSqlCapture implements Interceptor {

    /** 이 패키지 매퍼는 잡지 않는다(권한 검사·사용자 조회 등 공통 인프라) */
    static final String SKIP_PREFIX = "kr.co.nh.atms.web.common.";

    @Override
    public Object intercept(Invocation inv) throws Throwable {
        Object ret = inv.proceed();
        try {
            if (CrudLogContext.active() != null) {
                Object[] a = inv.getArgs();
                MappedStatement ms = (MappedStatement) a[0];
                if (!ms.getId().startsWith(SKIP_PREFIX)) {
                    BoundSql bs = a.length == 6 ? (BoundSql) a[5] : ms.getBoundSql(a[1]);
                    CrudLogContext.add("/* " + shortId(ms.getId()) + " */\n" + render(ms.getConfiguration(), bs, a[1]));
                }
            }
        } catch (Exception e) {
            log.warn("[사용기록] SQL 잡기 실패(업무는 정상): {}", e.toString());
        }
        return ret;
    }

    /** {@code kr.co.….notice.mapper.NoticeMapper.selectList} → {@code NoticeMapper.selectList} */
    static String shortId(String id) {
        int last = id.lastIndexOf('.');
        int prev = last <= 0 ? -1 : id.lastIndexOf('.', last - 1);
        return prev < 0 ? id : id.substring(prev + 1);
    }

    /** BoundSql + 실제 값 → 사람이 읽을 SQL. MyBatis DefaultParameterHandler 와 같은 순서로 값을 찾는다. 앞뒤 빈 줄은 뗀다 */
    static String render(Configuration cfg, BoundSql bs, Object param) {
        String sql = bs.getSql();
        List<ParameterMapping> mappings = bs.getParameterMappings();
        TypeHandlerRegistry thr = cfg.getTypeHandlerRegistry();
        StringBuilder out = new StringBuilder(sql.length() + 64);
        int idx = 0;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '?' && idx < mappings.size()) {
                ParameterMapping pm = mappings.get(idx++);
                if (pm.getMode() == ParameterMode.OUT) { out.append('?'); continue; }
                String prop = pm.getProperty();
                Object v;
                if (bs.hasAdditionalParameter(prop)) v = bs.getAdditionalParameter(prop);
                else if (param == null) v = null;
                else if (thr.hasTypeHandler(param.getClass())) v = param;
                else v = cfg.newMetaObject(param).getValue(prop);
                out.append(literal(v));
            } else {
                out.append(ch);
            }
        }
        return out.toString().replaceAll("^\\s*\\n|\\s+$", "");
    }

    static String literal(Object v) {
        if (v == null) return "NULL";
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        return "'" + v.toString().replace("'", "''") + "'";
    }
}
