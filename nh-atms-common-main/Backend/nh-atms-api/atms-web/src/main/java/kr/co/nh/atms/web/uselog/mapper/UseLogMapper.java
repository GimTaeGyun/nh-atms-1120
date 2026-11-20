package kr.co.nh.atms.web.uselog.mapper;

import kr.co.nh.atms.web.uselog.dto.UseLog;
import kr.co.nh.atms.web.uselog.dto.UseLogSearchDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 사용기록조회 — TB_SCR_CUD_HIST (SQL: resources/mapper/uselog/UseLogMapper.xml). 저장은 common/uselog/CrudLogMapper */
@Mapper
public interface UseLogMapper {

    /** 목록 한 페이지(SQL_TEXT 제외, HIST_ID DESC) */
    List<UseLog> selectList(UseLogSearchDto s);

    /** 목록 건수 */
    long countList(UseLogSearchDto s);

    /** 1건(PARAMS·SQL_TEXT 포함). 없으면 null */
    UseLog selectOne(@Param("histId") long histId);
}
