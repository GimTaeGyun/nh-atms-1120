package kr.co.nh.atms.web.jnal.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** 저널 수표 한 장 — API 계약 §13 {@code JnalCheck}. TB_JL_JNAL_CHK(또는 _ILM) */
@Getter
@Setter
@ToString
public class JnalCheck {
    /** 일련번호 SEQNO */
    private String seqno;
    /** 수표번호 CHK_NO */
    private String chkNo;
}
