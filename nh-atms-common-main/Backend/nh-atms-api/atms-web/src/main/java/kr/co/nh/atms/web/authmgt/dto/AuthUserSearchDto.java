package kr.co.nh.atms.web.authmgt.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 사용자 권한 조회조건 — API 계약 §8 {@code AuthUserSearch extends PageParams}. 컨트롤러가 {@code @ModelAttribute} 로 받는다.
 * 서비스가 값을 정리(사무소 6자리, 빈 값 null)해 그대로 매퍼에 넘긴다. 정렬은 사무소·개인번호·부서 고정(sort 무시).
 */
@Getter
@Setter
@ToString(callSuper = true)
public class AuthUserSearchDto extends PageRequest {
    /** 구분 DEPT(부서권한, PER_NO='1') · PERSON(개인권한) — 비우면 둘 다 */
    private String kind;
    /** 사무소코드(1~6자리 숫자, 앞 0 채움) */
    private String ofcCode;
    /** 권한(일치) */
    private String auth;
    /** 승인상태(일치) */
    private String apvSts;
    /** 검색어 — 개인번호·성명·부서코드·부서명 부분일치 (AS-IS "개인사번") */
    private String q;
}
