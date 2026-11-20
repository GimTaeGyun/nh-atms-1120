package kr.co.nh.atms.web.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/** 대시보드 [조회] 결과 — API 계약 §10 {@code GET /api/web/dashboard/summary} */
@Getter
@Setter
public class DashSummary {
    /** 고른 범위 이름(사무소명 / 영업본부명 / 전국) */
    private String title;
    /** BRANCH(사무소 하나) · REGION(영업본부 전체) · ALL(전국) */
    private String titleKind;
    /** 고른 사무소코드(하나일 때) 또는 영업본부 코드 */
    private String titleCode;
    /** 조회 시각 YYYYMMDDHH24MISS — ⑦ 운영 대수 '기준' */
    private String baseDtm;
    /** ⑦ 총 운영 대수(사용 중 코너의 사용 중 기기) */
    private int total;
    /** ③ 거래 기준일 YYYYMMDD(전 영업일) */
    private String dealDate;
    /** ③ 일평균 거래량 = 전 영업일 거래 건수 합 ÷ 운영 대수(반올림) */
    private long dealAvg;
    /** ⑤ 상태별 대수 {OK, ERR, FIX, OFF} */
    private Map<String, Integer> statusCount;
    /** ④⑥ 기기상태정보(범위 안 전체) */
    private List<DashMachine> machines;
}
