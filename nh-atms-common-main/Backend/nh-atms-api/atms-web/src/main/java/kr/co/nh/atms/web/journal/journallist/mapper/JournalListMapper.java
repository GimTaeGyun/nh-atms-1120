package kr.co.nh.atms.web.journal.journallist.mapper;

import kr.co.nh.atms.web.journal.journallist.dto.JournalListRow;
import kr.co.nh.atms.web.journal.journallist.dto.JournalListSearchDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** 저널조회 — TB_JL_JNAL + TB_JL_JNAL_CHK (SQL: resources/mapper/jnal/JournalListMapper.xml) */
@Mapper
public interface JournalListMapper {

    /** 저널 — RN 순, 합치기 전 */
    List<JournalListRow> selectList(JournalListSearchDto s);
}
