package kr.co.nh.atms.web.jnal.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** 수표목록 조건 = 저널 키(TB_JL_JNAL PK) — API 계약 §13 */
@Getter
@Setter
@ToString
public class JnalCheckKey {
    private String jnalDt;
    private String ofcCode;
    private String mcnNo;
    private String jnalSeqno;
}
