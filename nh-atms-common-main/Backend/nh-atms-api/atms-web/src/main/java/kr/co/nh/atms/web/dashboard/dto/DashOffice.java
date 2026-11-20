package kr.co.nh.atms.web.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 대시보드 사무소 1건(콤보·제목용) — TB_OFC_REG_MGT 의 OFC_CODE·KANM, regionCode = JUNG_MOJUM(영업본부) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashOffice {
    /** 사무소코드 OFC_CODE CHAR(6) */
    private String ofcCode;
    /** 사무소명 KANM */
    private String kanm;
    /** 영업본부(중간 모점) JUNG_MOJUM — 영업본부 사무소 자신이면 null 일 수 있음 */
    private String regionCode;
}
