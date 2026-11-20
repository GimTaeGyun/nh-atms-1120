package kr.co.nh.atms.web.common.uselog;

import org.apache.ibatis.annotations.Mapper;

/** 사용기록 저장 — mapper/common/CrudLogMapper.xml */
@Mapper
public interface CrudLogMapper {
    int insert(CrudLogEntry e);
}
