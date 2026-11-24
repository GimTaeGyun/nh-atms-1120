package kr.co.nh.atms.web.jnal.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * 저널 한 행 — API 계약 §13 {@code Jnal}. TB_JL_JNAL(또는 _ILM) + 수표 묶음(TB_JL_JNAL_CHK) + 거래명(TB_CODE_DTL '01').
 * 같은 명세표의 여러 저널은 서버가 한 행으로 합친다(JnalRules.merge) — 인자 글은 rolComts·dtlComts 목록.
 */
@Getter
@Setter
@ToString
public class Jnal {
    private String jnalDt;
    private String jnalTime;
    private String jnalSeqno;
    private String ofcCode;
    private String mcnNo;
    /** 저널구분 00·22·33 거래, 11 개국, 44 합계, 55 운영 */
    private String jnalType;
    /** 거래결과 01 정상·02 불능·97 취소정상·98 취소불능·99 확인요망·77 고객조작취소 */
    private String jrnlProcRest;
    private String dealDate;
    private String scrNo;
    /** 거래명 — JnalRules.scrNm */
    private String scrNm;
    private String dtlsSeqno;
    private String acctNo;
    private String estbBankCode;
    /** 거래금액(합친 행 = 가장 큰 값) */
    private Long dealAmt;
    private String mdnType;
    /** 수표 건수·첫 수표번호·수표번호 묶음(TB_JL_JNAL_CHK) */
    private Integer chkCnt;
    private String chkNo;
    private String chkNos;
    private String bprProcStat;
    private String faceImgYn;
    private String acptImgYn;
    private String cardImgYn;
    /** 전인자·후인자 원문(이 행 자신) */
    private String bfoComt;
    private String aftComt;
    /** 롤방식 글 — '전인자\n후인자' 를 합친 저널마다 하나 */
    private List<String> rolComts;
    /** 후인자만 — 합친 저널마다 하나 */
    private List<String> dtlComts;
    /** true = ILM 테이블에서 읽은 행 */
    private boolean ilm;

    /** SQL 순번·명세표 안 순번(합치기에만 씀, 응답에서 뺌) */
    @JsonIgnore
    private Integer rn;
    @JsonIgnore
    private Integer pRn;
}
