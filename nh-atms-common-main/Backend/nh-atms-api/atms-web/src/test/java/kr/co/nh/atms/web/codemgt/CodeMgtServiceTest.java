package kr.co.nh.atms.web.codemgt;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.web.codemgt.dto.CodeDetail;
import kr.co.nh.atms.web.codemgt.dto.CodeDetailSave;
import kr.co.nh.atms.web.codemgt.dto.CodeMaster;
import kr.co.nh.atms.web.codemgt.dto.CodeMasterSave;
import kr.co.nh.atms.web.codemgt.dto.CodeMgtDeleteRequest;
import kr.co.nh.atms.web.codemgt.dto.CodeMgtDeleteResult;
import kr.co.nh.atms.web.codemgt.dto.CodeMgtSearchDto;
import kr.co.nh.atms.web.codemgt.mapper.CodeMgtMapper;
import kr.co.nh.atms.web.codemgt.service.CodeMgtRules;
import kr.co.nh.atms.web.codemgt.service.CodeMgtService;
import kr.co.nh.atms.web.common.NotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 코드관리 — 코드구분(등록 검사·겹침·수정·삭제 막힘), 상세코드(구분 확인·겹침·데이터4/5 검사·삭제), 조회조건 정리 */
class CodeMgtServiceTest {

    private final CodeMgtMapper mapper = mock(CodeMgtMapper.class);
    private final CodeMgtService service = new CodeMgtService(mapper);

    private static String code(Throwable t) {
        return ((BusinessException) t).getCode();
    }

    private static CodeMaster master(String sect) {
        CodeMaster m = new CodeMaster();
        m.setCodeSect(sect);
        m.setCodeSectNm("이름");
        m.setUsedYn("Y");
        return m;
    }

    private static CodeDetail detail(String sect, String code) {
        CodeDetail d = new CodeDetail();
        d.setCodeSect(sect);
        d.setCode(code);
        d.setCodeNm("코드명");
        return d;
    }

    private static CodeMasterSave msave(String sect, String nm, String used) {
        CodeMasterSave s = new CodeMasterSave();
        s.setCodeSect(sect);
        s.setCodeSectNm(nm);
        s.setUsedYn(used);
        return s;
    }

    private static CodeDetailSave dsave(String code, String nm) {
        CodeDetailSave s = new CodeDetailSave();
        s.setCode(code);
        s.setCodeNm(nm);
        return s;
    }

    private static CodeMgtDeleteRequest keys(String... k) {
        CodeMgtDeleteRequest r = new CodeMgtDeleteRequest();
        r.setKeys(List.of(k));
        return r;
    }

    @Test
    void 조회조건은_공백을_떼고_사용여부는_대문자() {
        CodeMgtSearchDto s = new CodeMgtSearchDto();
        s.setQ("  MCN ");
        s.setUsedYn("y");
        service.masters(s);
        ArgumentCaptor<CodeMgtSearchDto> c = ArgumentCaptor.forClass(CodeMgtSearchDto.class);
        verify(mapper).selectMasters(c.capture());
        assertThat(c.getValue().getQ()).isEqualTo("MCN");
        assertThat(c.getValue().getUsedYn()).isEqualTo("Y");
    }

    @Test
    void 코드구분_등록_검사와_겹침() {
        assertThatThrownBy(() -> service.createMaster(msave("TOO_LONG_SECT", "이름", "Y"))).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));
        assertThatThrownBy(() -> service.createMaster(msave("A B", "이름", "Y"))).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));
        assertThatThrownBy(() -> service.createMaster(msave("MCN", " ", "Y"))).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));
        assertThatThrownBy(() -> service.createMaster(msave("MCN", "이름", "X"))).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));

        when(mapper.selectMaster("MCN")).thenReturn(master("MCN"));
        assertThatThrownBy(() -> service.createMaster(msave("MCN", "이름", "Y"))).satisfies(t -> assertThat(code(t)).isEqualTo("CDM_003"));
        verify(mapper, never()).insertMaster(any());
    }

    @Test
    void 코드구분_등록_성공과_동시등록() {
        when(mapper.selectMaster("NEW")).thenReturn(null, master("NEW"));
        CodeMasterSave s = msave(" NEW ", " 새 구분 ", "y");
        s.setPrcSect("B");
        CodeMaster r = service.createMaster(s);
        ArgumentCaptor<CodeMaster> c = ArgumentCaptor.forClass(CodeMaster.class);
        verify(mapper).insertMaster(c.capture());
        assertThat(c.getValue().getCodeSect()).isEqualTo("NEW");
        assertThat(c.getValue().getCodeSectNm()).isEqualTo("새 구분");
        assertThat(c.getValue().getUsedYn()).isEqualTo("Y");
        assertThat(r.getCodeSect()).isEqualTo("NEW");

        when(mapper.selectMaster("DUP")).thenReturn(null);
        doThrow(new DuplicateKeyException("pk")).when(mapper).insertMaster(any());
        assertThatThrownBy(() -> service.createMaster(msave("DUP", "이름", "Y"))).satisfies(t -> assertThat(code(t)).isEqualTo("CDM_003"));
    }

    @Test
    void 코드구분_수정은_없으면_404_처리구분은_한글자() {
        assertThatThrownBy(() -> service.updateMaster("NONE", msave(null, "이름", "Y"))).isInstanceOf(NotFoundException.class);

        when(mapper.selectMaster("MCN")).thenReturn(master("MCN"));
        CodeMasterSave s = msave(null, "이름", "N");
        s.setPrcSect("AB");
        assertThatThrownBy(() -> service.updateMaster("MCN", s)).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));

        s.setPrcSect("C");
        when(mapper.updateMaster(any())).thenReturn(1);
        service.updateMaster("MCN", s);
        ArgumentCaptor<CodeMaster> c = ArgumentCaptor.forClass(CodeMaster.class);
        verify(mapper).updateMaster(c.capture());
        assertThat(c.getValue().getCodeSect()).isEqualTo("MCN");
        assertThat(c.getValue().getUsedYn()).isEqualTo("N");
    }

    @Test
    void 상세가_남은_코드구분은_삭제_막힘() {
        when(mapper.selectSectsWithDetails(anyList())).thenReturn(List.of("MCN"));
        assertThatThrownBy(() -> service.deleteMasters(keys("MCN", "OLD"))).satisfies(t -> assertThat(code(t)).isEqualTo("CDM_002"));
        verify(mapper, never()).deleteMasters(anyList());

        when(mapper.selectSectsWithDetails(anyList())).thenReturn(List.of());
        when(mapper.deleteMasters(anyList())).thenReturn(2);
        CodeMgtDeleteResult r = service.deleteMasters(keys("OLD", "OLD2", "OLD"));
        assertThat(r.count()).isEqualTo(2);
        verify(mapper).deleteMasters(List.of("OLD", "OLD2"));

        assertThatThrownBy(() -> service.deleteMasters(keys())).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));
    }

    @Test
    void 상세코드_등록은_코드구분_확인과_겹침_데이터4_5_검사() {
        assertThatThrownBy(() -> service.createDetail("NONE", dsave("01", "이름"))).isInstanceOf(NotFoundException.class);

        when(mapper.selectMaster("MCN")).thenReturn(master("MCN"));
        when(mapper.selectDetail("MCN", "01")).thenReturn(detail("MCN", "01"));
        assertThatThrownBy(() -> service.createDetail("MCN", dsave("01", "이름"))).satisfies(t -> assertThat(code(t)).isEqualTo("CDM_005"));

        CodeDetailSave big = dsave("02", "이름");
        big.setData4(CodeMgtRules.DATA4_MAX + 1);
        assertThatThrownBy(() -> service.createDetail("MCN", big)).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));
        CodeDetailSave two = dsave("02", "이름");
        two.setData5("YN");
        assertThatThrownBy(() -> service.createDetail("MCN", two)).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));

        when(mapper.selectDetail("MCN", "02")).thenReturn(null, detail("MCN", "02"));
        CodeDetailSave ok = dsave("02", " 새 코드 ");
        ok.setData1(" 기기 ");
        ok.setData4(3);
        ok.setData5("Y");
        service.createDetail("MCN", ok);
        ArgumentCaptor<CodeDetail> c = ArgumentCaptor.forClass(CodeDetail.class);
        verify(mapper).insertDetail(c.capture());
        assertThat(c.getValue().getCodeSect()).isEqualTo("MCN");
        assertThat(c.getValue().getCodeNm()).isEqualTo("새 코드");
        assertThat(c.getValue().getData1()).isEqualTo("기기");
        assertThat(c.getValue().getData4()).isEqualTo(3);
    }

    @Test
    void 상세코드_수정_삭제() {
        assertThatThrownBy(() -> service.updateDetail("MCN", "09", dsave(null, "이름"))).isInstanceOf(NotFoundException.class);

        when(mapper.selectDetail("MCN", "01")).thenReturn(detail("MCN", "01"));
        when(mapper.updateDetail(any())).thenReturn(1);
        service.updateDetail("MCN", "01", dsave("바꿔도무시", "새 이름"));
        ArgumentCaptor<CodeDetail> c = ArgumentCaptor.forClass(CodeDetail.class);
        verify(mapper).updateDetail(c.capture());
        assertThat(c.getValue().getCode()).isEqualTo("01");
        assertThat(c.getValue().getCodeNm()).isEqualTo("새 이름");

        when(mapper.deleteDetails(eq("MCN"), anyList())).thenReturn(0);
        assertThatThrownBy(() -> service.deleteDetails("MCN", keys("99"))).isInstanceOf(NotFoundException.class);
        when(mapper.deleteDetails(eq("MCN"), anyList())).thenReturn(2);
        assertThat(service.deleteDetails("MCN", keys("01", "02")).count()).isEqualTo(2);
    }
}
