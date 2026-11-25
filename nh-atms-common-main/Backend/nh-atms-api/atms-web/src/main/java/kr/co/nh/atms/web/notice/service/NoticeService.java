package kr.co.nh.atms.web.notice.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.common.CurrentUser;
import kr.co.nh.atms.web.common.Dates;
import kr.co.nh.atms.web.common.DbBytes;
import kr.co.nh.atms.web.common.ForbiddenException;
import kr.co.nh.atms.web.common.NotFoundException;
import kr.co.nh.atms.web.common.OfficeCode;
import kr.co.nh.atms.web.notice.dto.Notice;
import kr.co.nh.atms.web.notice.dto.NoticeDeleteRequest;
import kr.co.nh.atms.web.notice.dto.NoticeDeleteResult;
import kr.co.nh.atms.web.notice.dto.NoticeSave;
import kr.co.nh.atms.web.notice.dto.NoticeSearchDto;
import kr.co.nh.atms.web.notice.mapper.NoticeMapper;
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
 * 공지사항(sample.notice) — API 계약 §3, 설계 §8.2 "팝업형 CRUD".
 * 응답 문구는 컨트롤러가 코드(S002·S003·S005)로, 오류는 응답코드(VAL_*·NTC_001~003)로만 낸다.
 * 공지 범위·본인 글 규칙은 {@link NoticeRules}(2026-11-13 사용자 결정): 보이지 않는 공지는 없는 것처럼 404 NTC_001,
 * 남의 글 수정·삭제 403 NTC_002, 관리자가 아닌데 전체·다른 사무소 공지 쓰기 403 NTC_003.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    /** 검색구분 허용 값(ALL 은 "검색어 안 씀") */
    private static final Set<String> FIELDS = Set.of("SUBJECT", "TEXT", "NAME");

    private final NoticeMapper mapper;

    /** 목록(페이징, NO DESC). 건수가 0 이면 목록 SQL 은 부르지 않는다 */
    public PageResult<Notice> search(NoticeSearchDto s) {
        normalize(s);
        long total = mapper.countList(s);
        List<Notice> rows = total == 0 ? List.of() : mapper.selectList(s);
        return PageResult.of(rows, total, s);
    }

    /**
     * 1건 + 앞·뒤 글. hit=true 면 조회수 +1 후 읽는다(상세 팝업을 열 때).
     * 없거나 내가 볼 수 없는 공지(다른 사무소)면 404 NTC_001 — 조회수도 올리지 않는다.
     */
    @Transactional
    public Notice get(long no, boolean hit) {
        visibleFound(no);
        if (hit && mapper.increaseVisit(no) == 0) throw new NotFoundException("NTC_001");
        Notice n = found(no);
        boolean admin = CurrentUser.isAdmin();
        for (Notice x : mapper.selectNeighbors(no, !admin, admin ? null : CurrentUser.ofcCode())) {
            if (x.getNo() < no) {
                n.setPrevNo(x.getNo());
                n.setPrevSubject(x.getSubject());
            } else if (x.getNo() > no) {
                n.setNextNo(x.getNo());
                n.setNextSubject(x.getSubject());
            }
        }
        return n;
    }

    /** 등록 — 번호 MAX+1(잠정 Q-02, 충돌 시 1회 재시도), 작성자 = 로그인 사용자, 작성일 SYSDATE, 조회수 0. 관리자가 아니면 내 사무소 공지만(NTC_003) */
    @Transactional
    public Notice create(NoticeSave req) {
        Notice n = toNotice(req);
        checkWriteTo(n.getOfcCode());
        n.setName(CurrentUser.name());
        n.setPerNo(CurrentUser.perNo());
        insertWithNextNo(n);
        log.info("[공지] 등록 no={} user={}", n.getNo(), CurrentUser.id());
        return found(n.getNo());
    }

    /** 수정 — 제목·내용·사무소만. 없거나 볼 수 없으면 404 NTC_001, 남의 글 403 NTC_002, 쓸 수 없는 사무소 403 NTC_003 */
    @Transactional
    public Notice update(long no, NoticeSave req) {
        Notice n = toNotice(req);
        checkModify(visibleFound(no));
        checkWriteTo(n.getOfcCode());
        n.setNo(no);
        if (mapper.update(n) == 0) throw new NotFoundException("NTC_001");
        log.info("[공지] 수정 no={} user={}", no, CurrentUser.id());
        return found(no);
    }

    /**
     * 다건 물리 삭제 — 1~100건(중복은 하나로), 아니면 400 VAL_001. 하나도 못 지우면 404 NTC_001.
     * 관리자가 아니면 고른 글이 모두 본인 글이어야 한다(하나라도 아니면 아무것도 지우지 않고 403 NTC_002, 볼 수 없는 글은 404 NTC_001).
     */
    @Transactional
    public NoticeDeleteResult delete(NoticeDeleteRequest req) {
        List<Long> nos = deleteTargets(req);
        if (!CurrentUser.isAdmin()) {
            for (Notice n : mapper.selectOwners(nos)) {
                if (!NoticeRules.canSee(n, false, CurrentUser.ofcCode())) throw new NotFoundException("NTC_001");
                checkModify(n);
            }
        }
        int count = mapper.deleteByNos(nos);
        if (count == 0) throw new NotFoundException("NTC_001");
        log.info("[공지] 삭제 nos={} count={} user={}", nos, count, CurrentUser.id());
        return new NoticeDeleteResult(count);
    }

    /* ---------------- 내부 ---------------- */

    private Notice found(long no) {
        Notice n = mapper.selectOne(no);
        if (n == null) throw new NotFoundException("NTC_001");
        return n;
    }

    /** 있고 내가 볼 수 있는 공지(다른 사무소 공지는 없는 것처럼 404 NTC_001) */
    private Notice visibleFound(long no) {
        Notice n = found(no);
        if (!NoticeRules.canSee(n, CurrentUser.isAdmin(), CurrentUser.ofcCode())) throw new NotFoundException("NTC_001");
        return n;
    }

    /** 남의 글이면 403 NTC_002(관리자는 통과) */
    private static void checkModify(Notice n) {
        if (!NoticeRules.canModify(n, CurrentUser.isAdmin(), CurrentUser.perNo())) throw new ForbiddenException("NTC_002");
    }

    /** 쓸 수 없는 사무소(관리자가 아닌데 전체 공지·다른 사무소)면 403 NTC_003 */
    private static void checkWriteTo(String ofcCode) {
        if (!NoticeRules.canWriteTo(ofcCode, CurrentUser.isAdmin(), CurrentUser.ofcCode())) throw new ForbiddenException("NTC_003");
    }

    /** 번호를 받아 넣는다. 다른 사람이 같은 번호를 먼저 넣었으면(PK 충돌) 한 번만 새 번호로 다시 넣는다 */
    private void insertWithNextNo(Notice n) {
        n.setNo(NoticeRules.nextNo(mapper));
        try {
            mapper.insert(n);
        } catch (DuplicateKeyException e) {
            long first = n.getNo();
            n.setNo(NoticeRules.nextNo(mapper));
            log.warn("[공지] 번호 충돌 no={} → {} 로 다시 등록", first, n.getNo());
            mapper.insert(n);
        }
    }

    /** 요청 본문 검사·정리 → 매퍼 파라미터 */
    private static Notice toNotice(NoticeSave req) {
        if (req == null) throw new BusinessException("VAL_004");
        String subject = req.getSubject() == null ? null : req.getSubject().trim();
        String text = req.getText();
        if (subject == null || subject.isEmpty()) throw new BusinessException("VAL_001", "subject");
        if (text == null || text.isBlank()) throw new BusinessException("VAL_001", "text");
        if (DbBytes.over(subject, NoticeRules.SUBJECT_MAX_BYTES)) throw new BusinessException("VAL_001", "subject");
        if (DbBytes.over(text, NoticeRules.TEXT_MAX_BYTES)) throw new BusinessException("VAL_001", "text");
        Notice n = new Notice();
        n.setSubject(subject);
        n.setText(text);
        n.setOfcCode(OfficeCode.norm(req.getOfcCode()));
        return n;
    }

    private static List<Long> deleteTargets(NoticeDeleteRequest req) {
        List<Long> raw = req == null ? null : req.getNos();
        // List.of(…) 는 contains(null) 에서 NPE 를 던지므로 하나씩 본다
        if (raw == null || raw.isEmpty() || raw.stream().anyMatch(Objects::isNull)) throw new BusinessException("VAL_001", "nos");
        List<Long> nos = new ArrayList<>(new LinkedHashSet<>(raw));
        if (nos.size() > NoticeRules.MAX_DELETE) throw new BusinessException("VAL_001", "nos");
        return nos;
    }

    /**
     * 조회조건 정리(매퍼가 그대로 쓴다).
     * 사무소 6자리(VAL_003), 일자 형식(VAL_003), 기간 순서·최대 {@value NoticeRules#MAX_SEARCH_DAYS}일(VAL_001),
     * 검색구분: 비움·ALL 이면 검색어를 쓰지 않음, 그 밖의 값은 VAL_003. 검색어가 비면 검색구분도 null.
     * 공지 범위: 비움·COMMON·OFC 만(그 밖 VAL_003). 사무소 지정은 관리자 + 범위 OFC 일 때만 쓴다(아니면 버림).
     * 보이는 범위 restrict·myOfc 는 요청 값과 상관없이 로그인 사용자로 덮어쓴다.
     */
    static void normalize(NoticeSearchDto s) {
        boolean admin = CurrentUser.isAdmin();
        String scope = trimToNull(s.getScope());
        scope = scope == null ? null : scope.toUpperCase(Locale.ROOT);
        if (scope != null && !NoticeRules.SCOPE_COMMON.equals(scope) && !NoticeRules.SCOPE_OFC.equals(scope)) throw new BusinessException("VAL_003", "scope");
        s.setScope(scope);
        s.setRestrict(!admin);
        s.setMyOfc(admin ? null : OfficeCode.norm(CurrentUser.ofcCode()));
        s.setOfcCode(admin && NoticeRules.SCOPE_OFC.equals(scope) ? OfficeCode.norm(s.getOfcCode()) : null);
        s.setPerNo(trimToNull(s.getPerNo()));
        String from = trimToNull(s.getFromDate());
        String to = trimToNull(s.getToDate());
        if (from != null && !isYmd(from)) throw new BusinessException("VAL_003", "fromDate");
        if (to != null && !isYmd(to)) throw new BusinessException("VAL_003", "toDate");
        if (from != null && to != null) {
            long days = Dates.daysBetween(from, to);
            if (days < 1 || days > NoticeRules.MAX_SEARCH_DAYS) throw new BusinessException("VAL_001", "fromDate~toDate");
        }
        s.setFromDate(from);
        s.setToDate(to);

        String field = trimToNull(s.getField());
        field = field == null ? null : field.toUpperCase(Locale.ROOT);
        if (field != null && !"ALL".equals(field) && !FIELDS.contains(field)) throw new BusinessException("VAL_003", "field");
        String keyword = trimToNull(s.getKeyword());
        if (field == null || "ALL".equals(field) || keyword == null) {
            s.setField(null);
            s.setKeyword(null);
        } else {
            s.setField(field);
            s.setKeyword(keyword);
        }
    }

    private static boolean isYmd(String v) {
        if (v.length() != 8) return false;
        try {
            Dates.parseYmd(v);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
