package kr.co.nh.atms.web.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 대시보드 조회 범위 — API 계약 §10 {@code GET /api/web/dashboard/scope}.
 * 레벨 = 로그인 권한코드(개인권한, 없으면 부서권한)로 정한다(2026-11-16 사용자 결정: A=중앙본부·T=영업본부·L=영업점, 설정으로 바꿈).
 * 내 사무소 = 인사 소속(TB_INSA.SAMUSO → TB_OFC_RAW.ONLINE_CODE).
 */
@Getter
@Setter
public class DashScope {
    /** CENTRAL(중앙본부) · REGION(영업본부) · BRANCH(영업점) */
    private String level;
    /** 내 사무소(인사 소속). 못 찾으면 null */
    private DashOffice myOffice;
    /** 지역 구분 콤보 목록 — 중앙본부 = 모든 영업본부, 영업본부 = 내 본부 하나, 영업점 = 내 본부 하나(잠금) */
    private List<DashOffice> regions;
    /** 처음 고를 지역(영업본부) — 중앙본부 = null(전국) */
    private String defaultRegion;
    /** 처음 고를 사무소 — 영업점 = 내 사무소, 그 밖 = null(전체) */
    private String defaultOfcCode;
    /** 지역 구분 콤보를 바꿀 수 있나(중앙본부만) */
    private boolean regionEditable;
    /** 사무소 구분 콤보를 바꿀 수 있나(중앙본부·영업본부) */
    private boolean officeEditable;
}
