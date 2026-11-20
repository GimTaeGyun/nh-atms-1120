package kr.co.nh.atms.web.dashboard.mapper;

import kr.co.nh.atms.web.dashboard.dto.DashMachine;
import kr.co.nh.atms.web.dashboard.dto.DashOffice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 대시보드 — SQL: resources/mapper/dashboard/DashboardMapper.xml (범위 조건은 region·ofcCode 둘 다 null 이면 전국) */
@Mapper
public interface DashboardMapper {

    /** 인사 소속 부서(TB_INSA.SAMUSO) → 사무소(TB_OFC_RAW.ONLINE_CODE) 1건. 없으면 null */
    DashOffice selectOfficeOfDept(@Param("samuso") String samuso);

    /** 사무소 1건(이름·영업본부). 없으면 null */
    DashOffice selectOffice(@Param("ofcCode") String ofcCode);

    /** 영업본부 목록 = 다른 사무소의 JUNG_MOJUM 으로 쓰인 사무소 */
    List<DashOffice> selectRegions();

    /** 영업본부 소속 사무소(JUNG_MOJUM = region) */
    List<DashOffice> selectRegionOffices(@Param("region") String region);

    /** 범위 안 기기(사용 중 코너의 사용 중 기기) + 상태 원천 값 */
    List<DashMachine> selectMachines(@Param("region") String region, @Param("ofcCode") String ofcCode, @Param("vendorCodeType") String vendorCodeType);

    /** 범위 안 기기의 그날 거래 건수 합(현금·수표 지급·입금) */
    long selectDealCount(@Param("region") String region, @Param("ofcCode") String ofcCode, @Param("dealDt") String dealDt);

    /** 휴일(HDAY_YN='Y') YYYYMMDD — from~to */
    List<String> selectHolidays(@Param("from") String from, @Param("to") String to);
}
