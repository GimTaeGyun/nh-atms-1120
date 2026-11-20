package kr.co.nh.atms.web.menumgt.mapper;

import kr.co.nh.atms.web.menumgt.dto.MenuMgtRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 메뉴관리(sample.menuMgt) — TB_SCR_MENU · TB_SCR_AUTH_MENU (SQL: resources/mapper/menumgt/MenuMgtMapper.xml).
 * 트리 순서·레벨·조회조건 거르기는 서비스가 한다(메뉴는 수백 건 이하라 전체를 한 번에 읽는다).
 */
@Mapper
public interface MenuMgtMapper {

    /** 메뉴 전체(SORT_NO·MENU_ID 순) */
    List<MenuMgtRow> selectAll();

    /** 1건. 없으면 null */
    MenuMgtRow selectMenu(@Param("menuId") String menuId);

    /** 그 상위의 바로 아래 메뉴ID 들 */
    List<String> selectChildIds(@Param("parentMenuId") String parentMenuId);

    /** 메뉴ID 가 이 접두어(영문 3자)로 시작하는 행 수 — 대메뉴 접두어 겹침 검사 */
    int countIdPrefix(@Param("prefix") String prefix);

    /** 같은 메뉴경로를 쓰는 다른 메뉴 수(excludeId 는 빼고 셈, null 이면 전부) */
    int countPath(@Param("menuPath") String menuPath, @Param("excludeId") String excludeId);

    /** 등록 — 감사 4컬럼은 AuditInterceptor */
    int insertMenu(MenuMgtRow row);

    /** 메뉴명·메뉴경로·순서·사용여부 수정. 반영 건수(0 = 없음) */
    int updateMenu(MenuMgtRow row);

    /** 그 메뉴들의 권한별 메뉴(TB_SCR_AUTH_MENU) 행 삭제. 지운 건수 */
    int deleteAuthMenus(@Param("menuIds") List<String> menuIds);

    /** 메뉴 다건 물리 삭제. 지운 건수 */
    int deleteMenus(@Param("menuIds") List<String> menuIds);
}
