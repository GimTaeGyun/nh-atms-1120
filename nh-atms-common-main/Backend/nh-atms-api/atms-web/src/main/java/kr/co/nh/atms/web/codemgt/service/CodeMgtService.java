package kr.co.nh.atms.web.codemgt.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.web.codemgt.dto.CodeDetail;
import kr.co.nh.atms.web.codemgt.dto.CodeDetailSave;
import kr.co.nh.atms.web.codemgt.dto.CodeMaster;
import kr.co.nh.atms.web.codemgt.dto.CodeMasterSave;
import kr.co.nh.atms.web.codemgt.dto.CodeMgtDeleteRequest;
import kr.co.nh.atms.web.codemgt.dto.CodeMgtDeleteResult;
import kr.co.nh.atms.web.codemgt.dto.CodeMgtSearchDto;
import kr.co.nh.atms.web.codemgt.mapper.CodeMgtMapper;
import kr.co.nh.atms.web.common.CurrentUser;
import kr.co.nh.atms.web.common.DbBytes;
import kr.co.nh.atms.web.common.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * 코드관리(sample.codeMgt) — API 계약 §11. 왼쪽 코드구분(TB_CODE_MASTER) · 오른쪽 상세코드(TB_CODE_DTL), 등록·수정은 팝업(공지사항 모양).
 * <ul>
 *   <li>키(코드구분·코드)는 수정 불가 — 경로로만 받는다</li>
 *   <li>상세코드가 있는 코드구분은 삭제 금지 400 CDM_002(물리 FK 없음)</li>
 *   <li>상세코드 등록 전에 코드구분이 있는지 확인 404 CDM_001</li>
 * </ul>
 * 오류: 입력 400 VAL_001(필드명), 코드구분 없음 404 CDM_001, 코드구분 겹침 400 CDM_003, 상세 없음 404 CDM_004, 코드 겹침 400 CDM_005.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodeMgtService {

    /** 화면키(screens/screenIds.ts) — 권한 검사의 메뉴 경로 원천 */
    public static final String SCREEN = "sample.codeMgt";

    private final CodeMgtMapper mapper;

    /* ======================= 코드구분 ======================= */

    public List<CodeMaster> masters(CodeMgtSearchDto s) {
        CodeMgtSearchDto c = new CodeMgtSearchDto();
        if (s != null) {
            c.setQ(trimToNull(s.getQ()));
            c.setUsedYn(upper(s.getUsedYn()));
        }
        return mapper.selectMasters(c);
    }

    public CodeMaster master(String codeSect) {
        return foundMaster(keyOf(codeSect, "codeSect"));
    }

    /** 등록 — 응답 S002. 같은 코드구분이 있으면 400 CDM_003 */
    @Transactional
    public CodeMaster createMaster(CodeMasterSave req) {
        if (req == null) throw new BusinessException("VAL_004");
        String sect = keyOf(req.getCodeSect(), "codeSect");
        if (mapper.selectMaster(sect) != null) throw new BusinessException("CDM_003", sect);
        CodeMaster row = new CodeMaster();
        row.setCodeSect(sect);
        fillMaster(row, req);
        try {
            mapper.insertMaster(row);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("CDM_003", sect);
        }
        log.info("[코드관리] 코드구분 등록 sect={} name={} user={}", sect, row.getCodeSectNm(), CurrentUser.id());
        return foundMaster(sect);
    }

    /** 수정 — 코드구분 빼고 모두. 응답 S003. 없으면 404 CDM_001 */
    @Transactional
    public CodeMaster updateMaster(String codeSect, CodeMasterSave req) {
        if (req == null) throw new BusinessException("VAL_004");
        String sect = foundMaster(keyOf(codeSect, "codeSect")).getCodeSect();
        CodeMaster row = new CodeMaster();
        row.setCodeSect(sect);
        fillMaster(row, req);
        if (mapper.updateMaster(row) == 0) throw new NotFoundException("CDM_001", sect);
        log.info("[코드관리] 코드구분 수정 sect={} name={} used={} user={}", sect, row.getCodeSectNm(), row.getUsedYn(), CurrentUser.id());
        return foundMaster(sect);
    }

    /** 다건 삭제 1~100건 — 상세코드가 남은 코드구분이 있으면 400 CDM_002(그 코드구분들). 하나도 못 지우면 404 CDM_001. 응답 S005 */
    @Transactional
    public CodeMgtDeleteResult deleteMasters(CodeMgtDeleteRequest req) {
        List<String> keys = keysOf(req, "keys");
        List<String> blocked = mapper.selectSectsWithDetails(keys);
        if (blocked != null && !blocked.isEmpty()) throw new BusinessException("CDM_002", String.join(", ", blocked));
        int count = mapper.deleteMasters(keys);
        if (count == 0) throw new NotFoundException("CDM_001", String.join(", ", keys));
        log.info("[코드관리] 코드구분 삭제 sects={} count={} user={}", keys, count, CurrentUser.id());
        return new CodeMgtDeleteResult(count);
    }

    /* ======================= 상세코드 ======================= */

    /** 그 코드구분의 상세코드(코드 순). 코드구분이 없으면 404 CDM_001 */
    public List<CodeDetail> details(String codeSect) {
        String sect = foundMaster(keyOf(codeSect, "codeSect")).getCodeSect();
        return mapper.selectDetails(sect);
    }

    public CodeDetail detail(String codeSect, String code) {
        return foundDetail(keyOf(codeSect, "codeSect"), keyOf(code, "code"));
    }

    /** 등록 — 응답 S002. 코드구분 없으면 404 CDM_001, 같은 코드가 있으면 400 CDM_005 */
    @Transactional
    public CodeDetail createDetail(String codeSect, CodeDetailSave req) {
        if (req == null) throw new BusinessException("VAL_004");
        String sect = foundMaster(keyOf(codeSect, "codeSect")).getCodeSect();
        String code = keyOf(req.getCode(), "code");
        if (mapper.selectDetail(sect, code) != null) throw new BusinessException("CDM_005", code);
        CodeDetail row = new CodeDetail();
        row.setCodeSect(sect);
        row.setCode(code);
        fillDetail(row, req);
        try {
            mapper.insertDetail(row);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("CDM_005", code);
        }
        log.info("[코드관리] 상세코드 등록 sect={} code={} name={} user={}", sect, code, row.getCodeNm(), CurrentUser.id());
        return foundDetail(sect, code);
    }

    /** 수정 — 코드명·데이터1~5. 응답 S003. 없으면 404 CDM_004 */
    @Transactional
    public CodeDetail updateDetail(String codeSect, String code, CodeDetailSave req) {
        if (req == null) throw new BusinessException("VAL_004");
        CodeDetail cur = foundDetail(keyOf(codeSect, "codeSect"), keyOf(code, "code"));
        CodeDetail row = new CodeDetail();
        row.setCodeSect(cur.getCodeSect());
        row.setCode(cur.getCode());
        fillDetail(row, req);
        if (mapper.updateDetail(row) == 0) throw new NotFoundException("CDM_004", cur.getCode());
        log.info("[코드관리] 상세코드 수정 sect={} code={} name={} user={}", row.getCodeSect(), row.getCode(), row.getCodeNm(), CurrentUser.id());
        return foundDetail(row.getCodeSect(), row.getCode());
    }

    /** 다건 삭제 1~100건(그 코드구분 안). 하나도 못 지우면 404 CDM_004. 응답 S005 */
    @Transactional
    public CodeMgtDeleteResult deleteDetails(String codeSect, CodeMgtDeleteRequest req) {
        String sect = keyOf(codeSect, "codeSect");
        List<String> codes = keysOf(req, "keys");
        int count = mapper.deleteDetails(sect, codes);
        if (count == 0) throw new NotFoundException("CDM_004", String.join(", ", codes));
        log.info("[코드관리] 상세코드 삭제 sect={} codes={} count={} user={}", sect, codes, count, CurrentUser.id());
        return new CodeMgtDeleteResult(count);
    }

    /* ======================= 내부 ======================= */

    private CodeMaster foundMaster(String sect) {
        CodeMaster m = mapper.selectMaster(sect);
        if (m == null) throw new NotFoundException("CDM_001", sect);
        return m;
    }

    private CodeDetail foundDetail(String sect, String code) {
        CodeDetail d = mapper.selectDetail(sect, code);
        if (d == null) throw new NotFoundException("CDM_004", code);
        return d;
    }

    private static void fillMaster(CodeMaster row, CodeMasterSave req) {
        String nm = trimToNull(req.getCodeSectNm());
        if (nm == null || DbBytes.over(nm, CodeMgtRules.SECT_NM_MAX_BYTES)) throw new BusinessException("VAL_001", "codeSectNm");
        row.setCodeSectNm(nm);
        row.setDesc1(bytesOrNull(req.getDesc1(), CodeMgtRules.DESC_MAX_BYTES, "desc1"));
        row.setDesc2(bytesOrNull(req.getDesc2(), CodeMgtRules.DESC_MAX_BYTES, "desc2"));
        row.setDesc3(bytesOrNull(req.getDesc3(), CodeMgtRules.DESC_MAX_BYTES, "desc3"));
        row.setDesc4(bytesOrNull(req.getDesc4(), CodeMgtRules.DESC_MAX_BYTES, "desc4"));
        row.setDesc5(bytesOrNull(req.getDesc5(), CodeMgtRules.DESC_MAX_BYTES, "desc5"));
        row.setTaskSect(bytesOrNull(req.getTaskSect(), CodeMgtRules.TASK_SECT_MAX_BYTES, "taskSect"));
        String prc = trimToNull(req.getPrcSect());
        if (prc != null && prc.length() > CodeMgtRules.PRC_SECT_MAX_LEN) throw new BusinessException("VAL_001", "prcSect");
        row.setPrcSect(prc);
        String used = upper(req.getUsedYn());
        if (used == null || !CodeMgtRules.YN.contains(used)) throw new BusinessException("VAL_001", "usedYn");
        row.setUsedYn(used);
    }

    private static void fillDetail(CodeDetail row, CodeDetailSave req) {
        String nm = trimToNull(req.getCodeNm());
        if (nm == null || DbBytes.over(nm, CodeMgtRules.CODE_NM_MAX_BYTES)) throw new BusinessException("VAL_001", "codeNm");
        row.setCodeNm(nm);
        row.setData1(bytesOrNull(req.getData1(), CodeMgtRules.DATA_MAX_BYTES, "data1"));
        row.setData2(bytesOrNull(req.getData2(), CodeMgtRules.DATA_MAX_BYTES, "data2"));
        row.setData3(bytesOrNull(req.getData3(), CodeMgtRules.DATA_MAX_BYTES, "data3"));
        if (!CodeMgtRules.validData4(req.getData4())) throw new BusinessException("VAL_001", "data4");
        row.setData4(req.getData4());
        String d5 = trimToNull(req.getData5());
        if (d5 != null && d5.length() > CodeMgtRules.DATA5_MAX_LEN) throw new BusinessException("VAL_001", "data5");
        row.setData5(d5);
    }

    /** 키 필수·규칙(VAL_001 필드명) */
    private static String keyOf(String v, String field) {
        String k = trimToNull(v);
        if (!CodeMgtRules.validKey(k)) throw new BusinessException("VAL_001", field);
        return k;
    }

    /** 다건 키 1~100건, 중복은 하나로 */
    private static List<String> keysOf(CodeMgtDeleteRequest req, String field) {
        List<String> raw = req == null ? null : req.getKeys();
        if (raw == null || raw.isEmpty() || raw.stream().anyMatch(Objects::isNull)) throw new BusinessException("VAL_001", field);
        Set<String> keys = new LinkedHashSet<>();
        for (String r : raw) keys.add(keyOf(r, field));
        if (keys.size() > CodeMgtRules.MAX_DELETE) throw new BusinessException("VAL_001", field);
        return new ArrayList<>(keys);
    }

    private static String bytesOrNull(String v, int maxBytes, String field) {
        String t = trimToNull(v);
        if (t != null && DbBytes.over(t, maxBytes)) throw new BusinessException("VAL_001", field);
        return t;
    }

    private static String upper(String v) {
        String t = trimToNull(v);
        return t == null ? null : t.toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
