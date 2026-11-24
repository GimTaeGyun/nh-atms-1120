package kr.co.nh.atms.web.jnal.mapper;

import kr.co.nh.atms.web.jnal.dto.Jnal;
import kr.co.nh.atms.web.jnal.dto.JnalCheck;
import kr.co.nh.atms.web.jnal.dto.JnalCheckKey;
import kr.co.nh.atms.web.jnal.dto.JnalSearchDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** 저널조회 — TB_JL_JNAL + TB_JL_JNAL_CHK (SQL: resources/mapper/jnal/JnalMapper.xml) */
@Mapper
public interface JnalMapper {

    /** 저널 — RN 순, 합치기 전 */
    List<Jnal> selectList(JnalSearchDto s);

    /** 저널 한 건의 수표(일련번호 순) */
    List<JnalCheck> selectChecks(JnalCheckKey k);
}
