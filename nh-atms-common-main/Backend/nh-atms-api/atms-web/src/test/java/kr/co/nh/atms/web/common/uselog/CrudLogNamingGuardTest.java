package kr.co.nh.atms.web.common.uselog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "이름이 곧 감사 정책" 지킴이 — atms-web 컨트롤러의 API 메서드는 이름이 select·insert·update·delete 로 시작하거나
 * {@link ExcludeCrudLog}(메서드·클래스)가 있어야 한다. 아니면 조용히 기록이 빠지므로 빌드를 실패시킨다(2026-11-17).
 */
class CrudLogNamingGuardTest {

    @Test
    void 컨트롤러_API_메서드는_접두어_또는_제외표시() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<String> bad = new ArrayList<>();
        int checked = 0;
        for (BeanDefinition bd : scanner.findCandidateComponents("kr.co.nh.atms.web")) {
            Class<?> c = Class.forName(bd.getBeanClassName());
            boolean classExcluded = c.isAnnotationPresent(ExcludeCrudLog.class);
            for (Method m : c.getDeclaredMethods()) {
                if (!Modifier.isPublic(m.getModifiers()) || !AnnotatedElementUtils.hasAnnotation(m, RequestMapping.class)) continue;
                checked++;
                if (classExcluded || m.isAnnotationPresent(ExcludeCrudLog.class)) continue;
                if (CrudLogRules.cudTypeOf(m.getName()) == null) bad.add(c.getSimpleName() + "." + m.getName());
            }
        }
        assertThat(checked).isGreaterThan(30);
        assertThat(bad).as("이름을 select·insert·update·delete 로 시작하게 하거나 @ExcludeCrudLog 를 붙이세요").isEmpty();
    }
}
