package kr.co.nh.atms.web.common.uselog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 사용기록(TB_SCR_CUD_HIST)에 남기지 않을 컨트롤러 — 메서드 또는 클래스에 붙인다(클래스면 그 안 메서드 모두).
 * <p>
 * 이름 접두어(select·insert·update·delete)가 맞아도 이 표시가 있으면 {@link CrudLogAspect} 가 기록하지 않는다.
 * 2026-11-17 사용자 결정 대상: 공통 콤보(lookup)·내 정보(me)·내 메뉴(menu/my)·로그인·공개키(auth)·대시보드 조회.
 * 접두어가 아닌 이름의 컨트롤러 메서드는 이 표시가 있어야 한다({@code CrudLogNamingGuardTest} 가 검사).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ExcludeCrudLog {
    /** 제외 사유(문서용, 선택) */
    String value() default "";
}
