package kr.co.nh.atms.web.codemgt.mapper;

import kr.co.nh.atms.web.codemgt.dto.CodeDetail;
import kr.co.nh.atms.web.codemgt.dto.CodeMaster;
import kr.co.nh.atms.web.codemgt.dto.CodeMgtSearchDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 코드관리 — TB_CODE_MASTER · TB_CODE_DTL (mapper/codemgt/CodeMgtMapper.xml) */
@Mapper
public interface CodeMgtMapper {

    List<CodeMaster> selectMasters(@Param("s") CodeMgtSearchDto s);

    CodeMaster selectMaster(@Param("codeSect") String codeSect);

    int insertMaster(CodeMaster row);

    int updateMaster(CodeMaster row);

    /** 지울 코드구분 중 상세코드가 남은 것 */
    List<String> selectSectsWithDetails(@Param("codeSects") List<String> codeSects);

    int deleteMasters(@Param("codeSects") List<String> codeSects);

    List<CodeDetail> selectDetails(@Param("codeSect") String codeSect);

    CodeDetail selectDetail(@Param("codeSect") String codeSect, @Param("code") String code);

    int insertDetail(CodeDetail row);

    int updateDetail(CodeDetail row);

    int deleteDetails(@Param("codeSect") String codeSect, @Param("codes") List<String> codes);
}
