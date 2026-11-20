package kr.co.nh.atms.web.common.uselog;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.nh.atms.web.common.DbBytes;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 사용기록 규칙 — 접두어 → 수행작업, PARAMS 머리·자르기·비밀값 지움, 이력번호, SQL 글 만들기 */
class CrudLogRulesTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void 메서드_이름_접두어가_곧_수행작업() {
        assertThat(CrudLogRules.cudTypeOf("selectList")).isEqualTo("R");
        assertThat(CrudLogRules.cudTypeOf("insert")).isEqualTo("C");
        assertThat(CrudLogRules.cudTypeOf("updateBatch")).isEqualTo("U");
        assertThat(CrudLogRules.cudTypeOf("deleteMasters")).isEqualTo("D");
        assertThat(CrudLogRules.cudTypeOf("list")).isNull();
        assertThat(CrudLogRules.cudTypeOf("saveBatch")).isNull();
        assertThat(CrudLogRules.isCudType("E")).isTrue();
        assertThat(CrudLogRules.isCudType("X")).isFalse();
    }

    @Test
    void PARAMS_머리는_ip_auth_err_뒤에_인자_JSON() {
        String p = CrudLogRules.params("10.1.2.3", "A", "NTC_002", "{\"q\":1}");
        assertThat(p).isEqualTo("ip=10.1.2.3 auth=A err=NTC_002 {\"q\":1}");
        assertThat(CrudLogRules.headOf(p, "ip")).isEqualTo("10.1.2.3");
        assertThat(CrudLogRules.headOf(p, "auth")).isEqualTo("A");
        assertThat(CrudLogRules.headOf(p, "err")).isEqualTo("NTC_002");
        String none = CrudLogRules.params(null, null, null, null);
        assertThat(none).isEqualTo("ip=- auth=- {}");
        assertThat(CrudLogRules.headOf(none, "ip")).isNull();
        assertThat(CrudLogRules.headOf(none, "err")).isNull();
        // IP 검색은 'ip=<값> ' 으로 시작하는 행 — 10.1.2.3 이 10.1.2.30 과 섞이지 않는다
        assertThat(CrudLogRules.ipLike("10.1.2.3")).isEqualTo("ip=10.1.2.3 %");
        assertThat(CrudLogRules.isIp("10.1.2.3")).isTrue();
        assertThat(CrudLogRules.isIp("10.1.2.3%")).isFalse();
    }

    @Test
    void PARAMS_는_500바이트로_표시_없이_자른다_한글은_2바이트() {
        String json = "{\"t\":\"" + "가".repeat(400) + "\"}";
        String p = CrudLogRules.params("1.1.1.1", "A", null, json);
        assertThat(DbBytes.length(p)).isLessThanOrEqualTo(500);
        assertThat(p).startsWith("ip=1.1.1.1 auth=A {").doesNotEndWith("…");
        assertThat(CrudLogRules.cutBytes("ab가", 3)).isEqualTo("ab");
        assertThat(CrudLogRules.cutBytes("ab가", 4)).isEqualTo("ab가");
    }

    @Test
    void 비밀번호_토큰만_지우고_나머지는_원문() {
        Map<String, String[]> q = new LinkedHashMap<>();
        q.put("ofcCode", new String[]{"000080"});
        q.put("token", new String[]{"abc"});
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("perNo", "123456789");
        body.put("password", "pw!");
        body.put("inner", Map.of("accessToken", "t"));
        String j = CrudLogRules.argsJson(om, q, body);
        assertThat(j).contains("\"ofcCode\":\"000080\"", "\"token\":\"***\"", "\"perNo\":\"123456789\"", "\"password\":\"***\"", "\"accessToken\":\"***\"")
                .doesNotContain("pw!", "\"abc\"");
        assertThat(CrudLogRules.argsJson(om, Map.of(), null)).isNull();
    }

    @Test
    void 이력번호는_시각14_서버1_일련4() {
        assertThat(CrudLogRules.histId("20261117093000", 2, 7)).isEqualTo(2026111709300020007L);
        assertThatThrownBy(() -> CrudLogRules.histId("20261117093000", 10, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrudLogRules.histId("20261117093000", 1, 10000)).isInstanceOf(IllegalArgumentException.class);
        HistIdGenerator g = new HistIdGenerator(1);
        long a = g.next();
        long b = g.next();
        assertThat(b).isGreaterThan(a);
        assertThat(String.valueOf(a)).hasSize(19).matches("\\d{14}1\\d{4}");
    }

    @Test
    void 실행_SQL_에_실제_값을_끼운다() {
        Configuration cfg = new Configuration();
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("ofcCode", "000080");
        param.put("limit", 20);
        param.put("subject", "it's");
        String sql = "\n  SELECT * FROM T WHERE A = ? AND B = ? AND C = ?  \n";
        List<ParameterMapping> pms = List.of(
                new ParameterMapping.Builder(cfg, "ofcCode", Object.class).build(),
                new ParameterMapping.Builder(cfg, "limit", Object.class).build(),
                new ParameterMapping.Builder(cfg, "subject", Object.class).build());
        BoundSql bs = new BoundSql(cfg, sql, pms, param);
        assertThat(ExecutedSqlCapture.render(cfg, bs, param)).isEqualTo("  SELECT * FROM T WHERE A = '000080' AND B = 20 AND C = 'it''s'");
        assertThat(ExecutedSqlCapture.literal(null)).isEqualTo("NULL");
        assertThat(ExecutedSqlCapture.shortId("kr.co.nh.atms.web.notice.mapper.NoticeMapper.selectList")).isEqualTo("NoticeMapper.selectList");
    }
}
