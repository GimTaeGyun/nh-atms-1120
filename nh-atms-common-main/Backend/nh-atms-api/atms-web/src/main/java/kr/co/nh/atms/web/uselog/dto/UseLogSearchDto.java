package kr.co.nh.atms.web.uselog.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 사용기록 조회조건 — API 계약 §12 {@code UseLogSearch extends PageParams}. 정렬은 이력번호(= 시각) 내림차순 고정.
 * 서비스(UseLogRules.normalize)가 값을 정리하고 보이는 범위(관리자 = 전체, 그 밖 = 내 사무소)를 덮어쓴다.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class UseLogSearchDto extends PageRequest {
    /** 일자 시작 YYYYMMDD (필수) */
    private String fromDate;
    /** 일자 끝 YYYYMMDD (필수, 그날 포함, 최대 31일) */
    private String toDate;
    /** 수행작업 R·C·U·D·E (비면 전체) */
    private String cudType;
    /** 수행결과 Y·N */
    private String succYn;
    /** 사무소코드(관리자만 쓴다 — 그 밖은 내 사무소로 덮어씀) */
    private String ofcCode;
    /** 개인번호 */
    private String perNo;
    /** 메뉴ID */
    private String menuId;
    /** 사용자 IP(완전일치) */
    private String ip;

    /* ---- 아래는 서비스가 채운다(요청 값은 쓰지 않음) ---- */
    /** PARAMS LIKE 값({@code ip=<IP> %}) */
    private String ipLike;
    /** true = 볼 수 있는 기록이 없음(관리자가 아닌데 권한 사무소가 없음) */
    private boolean none;
}
