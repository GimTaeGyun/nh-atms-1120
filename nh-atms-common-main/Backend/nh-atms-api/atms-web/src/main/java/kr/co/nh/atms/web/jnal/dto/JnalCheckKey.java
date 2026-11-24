package kr.co.nh.atms.web.jnal.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** 수표목록 조건 = 저널 키(TB_JL_JNAL PK) — API 계약 §13. ilm 은 서비스가 저널일자로 채운다 */
@Getter
@Setter
@ToString
public class JnalCheckKey {
    private String jnalDt;
    private String ofcCode;
    private String mcnNo;
    private String jnalSeqno;
    /** 서비스가 채움(요청 값은 쓰지 않음) */
    private boolean ilm;
}
