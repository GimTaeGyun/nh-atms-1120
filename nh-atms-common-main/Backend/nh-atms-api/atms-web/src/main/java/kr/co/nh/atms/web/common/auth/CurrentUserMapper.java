package kr.co.nh.atms.web.common.auth;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 현재 사용자 조회 — mapper/common/CurrentUserMapper.xml */
@Mapper
public interface CurrentUserMapper {
    /** TB_INSA 1건(없으면 null) */
    InsaRow selectInsa(@Param("perNo") String perNo);

    /** TB_SCR_AUTH 의 그 개인번호 행 전부 = 개인권한(채택은 CurrentUserLoader.pickAuth) */
    List<ScrAuthRow> selectAuths(@Param("perNo") String perNo);

    /**
     * 그 사람의 부서권한 행 전부 — TB_INSA.SAMUSO → TB_OFC_RAW(SAMUSO 부서코드·ONLINE_CODE 사무소코드) 와
     * DEPT_CODE·OFC_CODE 가 둘 다 같은 PER_NO='1' 행(CurrentUserLoader.DEPT_PER_NO)
     */
    List<ScrAuthRow> selectDeptAuths(@Param("perNo") String perNo);

    /** TB_OFC_REG_MGT.KANM (없으면 null) */
    String selectKanm(@Param("ofcCode") String ofcCode);
}
