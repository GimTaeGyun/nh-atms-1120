package kr.co.nh.atms.web.authmgt.mapper;

import kr.co.nh.atms.web.authmgt.dto.AuthCode;
import kr.co.nh.atms.web.authmgt.dto.AuthDept;
import kr.co.nh.atms.web.authmgt.dto.AuthMenu;
import kr.co.nh.atms.web.authmgt.dto.AuthUser;
import kr.co.nh.atms.web.authmgt.dto.AuthUserKey;
import kr.co.nh.atms.web.authmgt.dto.AuthUserSearchDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 권한관리(sample.authMgt) — TB_SCR_AUTH · TB_SCR_AUTH_MENU · TB_SCR_MENU (SQL: resources/mapper/authmgt/AuthMgtMapper.xml).
 * authCodeType = 권한명 원천 코드구분(설정 atms.auth-mgt.auth-code-type, 없으면 null → 권한명 null, 잠정 Q-20).
 */
@Mapper
public interface AuthMgtMapper {

    /* ---------- 사용자 권한 (TB_SCR_AUTH) ---------- */

    /** 목록 한 페이지(사무소·개인번호·부서 순) */
    List<AuthUser> selectUserList(@Param("s") AuthUserSearchDto s, @Param("authCodeType") String authCodeType);

    /** 목록 건수 */
    long countUserList(@Param("s") AuthUserSearchDto s);

    /** 1건. 없으면 null */
    AuthUser selectUser(@Param("k") AuthUserKey key, @Param("authCodeType") String authCodeType);

    /** 같은 키가 있나(0/1) */
    int countUserByKey(AuthUserKey key);

    /** 부서 목록(TB_OFC_RAW, 부서코드 순, 최대 limit 건). q = 부서코드·부서명 부분일치 */
    List<AuthDept> selectDepts(@Param("q") String q, @Param("limit") int limit);

    /** 부서 1건(TB_OFC_RAW.SAMUSO). 없으면 null */
    AuthDept selectDept(@Param("samuso") String samuso);

    /** 그 사람의 소속 부서(TB_INSA.SAMUSO = TB_OFC_RAW.SAMUSO). 없으면 null */
    AuthDept selectPersonDept(@Param("perNo") String perNo);

    /** 인사정보에 있는 개인번호인가(0/1) */
    int countInsa(@Param("perNo") String perNo);

    /** 등록 — INS_DATE·INS_ID 는 서비스가 채움, 감사 4컬럼은 AuditInterceptor */
    int insertUser(AuthUser row);

    /** 권한·승인상태·만료일시·메모 수정. 반영 건수(0 = 없음) */
    int updateUser(AuthUser row);

    /** 다건 물리 삭제. 지운 건수 */
    int deleteUsers(@Param("keys") List<AuthUserKey> keys);

    /* ---------- 권한코드 · 메뉴 권한 (TB_SCR_AUTH_MENU) ---------- */

    /** 권한코드 목록(AUTH 오름차순) */
    List<AuthCode> selectAuthCodes(@Param("authCodeType") String authCodeType);

    /** 메뉴 전체 + 이 권한의 플래그(상위메뉴·순서·ID 순 — 트리 순서는 서비스가 만든다) */
    List<AuthMenu> selectAuthMenus(@Param("auth") String auth);

    /** 같은 키(AUTH+MENU_ID)가 있나(0/1) */
    int countAuthMenu(AuthMenu key);

    /** 메뉴가 TB_SCR_MENU 에 있나(0/1) */
    int countMenu(@Param("menuId") String menuId);

    int insertAuthMenu(AuthMenu row);

    /** 플래그 5개·사용여부 수정. 반영 건수 (행 삭제는 없다 — 사용 해제 = USE_YN 'N') */
    int updateAuthMenu(AuthMenu row);
}
