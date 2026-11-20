package kr.co.nh.atms.web.common.auth;

import kr.co.nh.atms.web.common.uselog.CrudLogRules;
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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 화면별 서버 권한 지킴이(2026-11-17 반입 전 점검) — atms-web 컨트롤러의 API 메서드마다
 * <ul>
 *   <li>{@link RequireMenuAuth} 가 있고, 필요한 플래그가 이름 접두어와 맞는다: select = QRY, insert = REG, update = UPD, delete = DEL</li>
 *   <li>또는 아래 "권한 검사 없음" 목록(로그인 사용자 누구나 — 공통 콤보·나·내 메뉴·로그인·엑셀 기록), "서비스에서 검사" 목록(일괄 저장 — 들어온 종류별 REG·UPD·DEL)</li>
 * </ul>
 * 목록 밖에서 권한 표시가 빠지거나 플래그가 이름과 어긋나면 빌드가 실패한다. 화면키는 컨트롤러마다 하나.
 */
class MenuAuthCoverageTest {

    /** 권한 검사 없음(로그인 사용자면 누구나) — 계약 §0 */
    private static final Set<String> OPEN = Set.of("LookupController", "MenuController", "MeController", "LoginController", "UseLogController.insertExcel");
    /** 한 요청에 등록·수정·삭제가 섞여 서비스에서 MenuAuthService.check 로 종류별 검사 */
    private static final Set<String> CHECKED_IN_SERVICE = Set.of("McnModelController.updateBatch", "AuthMgtController.updateAuthMenus");

    private static final Map<String, Perm> NEED = Map.of("R", Perm.QRY, "C", Perm.REG, "U", Perm.UPD, "D", Perm.DEL);

    @Test
    void 모든_API_는_화면_권한_검사_또는_예외_목록() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<String> bad = new ArrayList<>();
        Map<String, Set<String>> screensByController = new TreeMap<>();
        int checked = 0;
        for (BeanDefinition bd : scanner.findCandidateComponents("kr.co.nh.atms.web")) {
            Class<?> c = Class.forName(bd.getBeanClassName());
            for (Method m : c.getDeclaredMethods()) {
                if (!Modifier.isPublic(m.getModifiers()) || !AnnotatedElementUtils.hasAnnotation(m, RequestMapping.class)) continue;
                checked++;
                String name = c.getSimpleName() + "." + m.getName();
                RequireMenuAuth ra = m.getAnnotation(RequireMenuAuth.class);
                if (OPEN.contains(c.getSimpleName()) || OPEN.contains(name) || CHECKED_IN_SERVICE.contains(name)) {
                    if (ra != null) bad.add(name + " : 예외 목록인데 @RequireMenuAuth 가 있음(목록을 고치세요)");
                    continue;
                }
                if (ra == null) {
                    bad.add(name + " : @RequireMenuAuth 없음");
                    continue;
                }
                String cud = CrudLogRules.cudTypeOf(m.getName());
                // 접두어가 없는 이름은 기록 제외(@ExcludeCrudLog) 조회 API 뿐(대시보드) — CrudLogNamingGuardTest 가 따로 지킴 → QRY
                Perm want = cud == null ? Perm.QRY : NEED.get(cud);
                if (want != ra.need()) bad.add(name + " : need=" + ra.need() + ", 이름으로는 " + want);
                if (!ra.screen().matches("^[a-z]+\\.[a-z][A-Za-z0-9]*$")) bad.add(name + " : 화면키 모양 " + ra.screen());
                screensByController.computeIfAbsent(c.getSimpleName(), k -> new java.util.TreeSet<>()).add(ra.screen());
            }
        }
        screensByController.forEach((ctl, screens) -> {
            if (screens.size() != 1) bad.add(ctl + " : 화면키가 여럿 " + screens);
        });
        assertThat(checked).isGreaterThan(30);
        assertThat(bad).isEmpty();
        assertThat(screensByController).containsEntry("AuthMgtController", Set.of("sample.authMgt"))
                .containsEntry("UseLogController", Set.of("sample.useLog"))
                .containsEntry("DashboardController", Set.of("main.dashboard"));
    }
}
