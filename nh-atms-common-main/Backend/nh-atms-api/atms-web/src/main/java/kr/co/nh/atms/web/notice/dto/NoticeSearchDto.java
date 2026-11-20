package kr.co.nh.atms.web.notice.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 공지사항 조회조건 — API 계약 §3 {@code NoticeSearch extends PageParams}. 컨트롤러가 {@code @ModelAttribute} 로 받는다.
 * 서비스가 값을 정리(사무소 6자리, 검색구분·범위 대문자, 빈 값 null, 보이는 범위 restrict·myOfc)해 그대로 매퍼에 넘긴다. 정렬은 {@code NO DESC} 고정(sort 무시).
 */
@Getter
@Setter
@ToString(callSuper = true)
public class NoticeSearchDto extends PageRequest {
    /** 작성일 시작 YYYYMMDD */
    private String fromDate;
    /** 작성일 끝 YYYYMMDD (그날 포함) */
    private String toDate;
    /** 사무소코드(1~6자리 숫자, 앞 0 채움) */
    private String ofcCode;
    /** 작성자 개인번호 */
    private String perNo;
    /** 검색구분 ALL·SUBJECT·TEXT·NAME (ALL·비움이면 검색어를 쓰지 않음) */
    private String field;
    /** 검색어(부분일치) */
    private String keyword;
    /**
     * 공지 범위 — 비움 = 볼 수 있는 공지 모두(관리자 = 모든 공지, 그 밖 = 전체 공지 + 내 사무소),
     * {@code COMMON} = 전체 공지(사무소 빈 것)만, {@code OFC} = 사무소 공지만(관리자는 {@code ofcCode} 로 사무소 지정, 그 밖은 내 사무소).
     */
    private String scope;

    /* ---- 아래 둘은 서비스가 로그인 사용자로 늘 덮어쓴다(요청 값은 쓰지 않음) ---- */
    /** true = 관리자가 아니라서 "전체 공지 + 내 사무소" 만 보인다 */
    private boolean restrict;
    /** 내 사무소(권한 사무소 6자리). restrict 일 때만 쓴다, 없으면 null(= 전체 공지만 보임) */
    private String myOfc;
}
