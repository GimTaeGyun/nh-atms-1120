package kr.co.nh.atms.web.journal.journallist.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 저널조회 조건 — API 계약 §13 {@code JournalListSearch}. 페이지 없음(서버가 합친 전체 목록, 화면이 20건씩 나눠 보임).
 * 서비스(JournalListRules.normalize)가 값을 정리한다. 테이블은 TB_JL_JNAL(+_CHK)만.
 */
@Getter
@Setter
@ToString
public class JournalListSearchDto {
    /** 사무소코드(필수). 권한 A·T·L 이 아니면 내 사무소로 덮어씀 */
    private String ofcCode;
    /** 기번(필수, 3자리) */
    private String mcnNo;
    /** 저널일자 시작·끝 YYYYMMDD(필수, 최대 2일) */
    private String fromDate;
    private String toDate;
    /** 저널시간 HHmm(비면 0000~2359). 시작은 그 분 00초, 끝은 59초부터 */
    private String fromTime;
    private String toTime;
    /** 저널구분 22 일반·33 수표·11 개국·44 합계·55 운영(비면 전체) */
    private String jnalType;
    /** 거래결과 01 정상·02 불능·97 취소정상·98 취소불능·99 확인요망(비면 전체) */
    private String jrnlProcRest;
    /** 조회조건 ACCT 계좌번호·CHK 수표번호·DTLS 명세표일련번호(비면 없음) */
    private String field;
    /** 검색어 — 계좌·수표 = 부분일치, 명세표 = '0001-0050' 범위 또는 번호 하나 */
    private String keyword;

    /* ---- 아래는 서비스가 채운다(요청 값은 쓰지 않음) ---- */
    /** JNAL_TIME 범위 HHmmss */
    private String frTi;
    private String toTi;
    /** 검색값(계좌·수표 = 1만, 명세표 = 시작·끝, 빈 쪽 null) */
    private String schData1;
    private String schData2;
}
