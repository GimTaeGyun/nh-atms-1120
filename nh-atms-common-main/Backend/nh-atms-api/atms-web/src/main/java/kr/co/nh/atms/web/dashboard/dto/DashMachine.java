package kr.co.nh.atms.web.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 기기상태정보 1행 — 6302 코너 마스터 기준(2026-11-16 사용자 결정).
 * 관리점 = 기기·코너 OFC_CODE, 관리모점 = 코너 BLT_OLD_OFC_CODE(소속영업점, 비면 관리점).
 * 상태 원천 값(powerOff·useStop·errMode)은 서비스가 DashboardRules.statusOf 로 status 를 만든 뒤 응답에서 뺀다.
 */
@Getter
@Setter
public class DashMachine {
    private String mgrOfcCode;
    private String mgrOfcNm;
    private String parentOfcCode;
    private String parentOfcNm;
    /** 코너명 TB_SCRT_INST_PST.INST_PST_NM */
    private String instPstNm;
    private String mcnNo;
    /** 기종 TB_NMS_SRC.SRC_NM (없으면 SRC) */
    private String srcNm;
    /** 제조사 — 기기 VEND_CODE 의 이름(설정 코드구분, 잠정 Q6). 없으면 코드 */
    private String vendNm;
    /** 모델명 — TB_MCN_INFO 에 모델 컬럼이 없어 KOM_TYPE(기기유형)을 보인다(잠정 Q-27) */
    private String modelNm;
    /** OK 정상 · ERR 장애 · FIX 보수중 · OFF 통신단절 */
    private String status;
    /** 기기상태상세 — 장애면 최근 장애명(TB_EE_MCN_ERR_HIST.ERR_NM), 보수중이면 null */
    private String statusDetail;
    /** 주소 — 코너에 주소 컬럼이 없어 관리모점(없으면 관리점) 사무소 주소(잠정 Q-27) */
    private String address;
    /** 관리업체 — 코너 VEND_CODE(일괄용역이면 IAL_SEV_VEND_CODE) 코드. 이름 원천 미정(잠정 Q-27) */
    private String mgmtVendCode;

    /* ---- 판정용 원천 값(응답에는 null 로 비움) ---- */
    private String powerOff;
    private String useStop;
    private String errMode;
}
