package kr.co.nh.atms.web.jnal.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 저널조회 조건 — API 계약 §13 {@code JnalSearch}. 페이지 없음(서버가 합친 전체 목록, 화면이 20건씩 나눠 보임).
 * 서비스(JnalRules.normalize)가 값을 정리하고, JnalRules.split 이 운영/ILM 묶음마다 복사본을 만든다.
 */
@Getter
@Setter
@ToString
public class JnalSearchDto {
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
    /** 이 묶음의 실제 저널일자 범위 */
    private String frDt;
    private String toDt;
    /** true = ILM 테이블(TB_JL_JNAL_ILM·TB_JL_JNAL_CHK_ILM) */
    private boolean ilm;

    /** 묶음 복사본(JnalRules.split) */
    public JnalSearchDto copyFor(String from, String to, boolean ilmPart) {
        JnalSearchDto c = new JnalSearchDto();
        c.ofcCode = ofcCode;
        c.mcnNo = mcnNo;
        c.fromDate = fromDate;
        c.toDate = toDate;
        c.fromTime = fromTime;
        c.toTime = toTime;
        c.jnalType = jnalType;
        c.jrnlProcRest = jrnlProcRest;
        c.field = field;
        c.keyword = keyword;
        c.frTi = frTi;
        c.toTi = toTi;
        c.schData1 = schData1;
        c.schData2 = schData2;
        c.frDt = from;
        c.toDt = to;
        c.ilm = ilmPart;
        return c;
    }
}
