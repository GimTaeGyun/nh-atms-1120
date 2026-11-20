package kr.co.nh.atms.web.menumgt.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 메뉴 목록 조회조건 — API 계약 §9 {@code MenuMgtSearch}. 메뉴는 수백 건 이하라 페이징 없이 트리 순서 전체를 내린다.
 * 모두 비우면 전체(등록 팝업 콤보도 이 전체 목록을 쓴다).
 */
@Getter
@Setter
public class MenuMgtSearchDto {
    /** 대메뉴ID — 그 대메뉴와 아래 메뉴만 */
    private String topMenuId;
    /** 레벨 1 대·2 중·3 소 */
    private Integer menuLevel;
    /** 사용여부 Y/N */
    private String useYn;
    /** 메뉴ID·메뉴명·메뉴경로 부분일치(대소문자 무시) */
    private String q;
}
