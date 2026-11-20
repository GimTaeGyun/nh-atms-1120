package kr.co.nh.atms.web.notice;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.common.DbBytes;
import kr.co.nh.atms.web.common.ForbiddenException;
import kr.co.nh.atms.web.common.NotFoundException;
import kr.co.nh.atms.web.common.auth.LoginUser;
import kr.co.nh.atms.web.notice.dto.Notice;
import kr.co.nh.atms.web.notice.dto.NoticeDeleteRequest;
import kr.co.nh.atms.web.notice.dto.NoticeSave;
import kr.co.nh.atms.web.notice.dto.NoticeSearchDto;
import kr.co.nh.atms.web.notice.mapper.NoticeMapper;
import kr.co.nh.atms.web.notice.service.NoticeRules;
import kr.co.nh.atms.web.notice.service.NoticeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 공지사항 서비스 — 조회조건 정리·응답코드(VAL_001·VAL_003·NTC_001~003)·채번 재시도·작성자·바이트 길이(잠정 Q-07),
 * 공지 범위·본인 글 규칙(2026-11-13 사용자 결정: 남의 글·전체 공지 = 관리자만, 사무소 공지는 그 사무소 + 관리자만 보임).
 * 기본 로그인 = 관리자(사무소 000080). 관리자가 아닌 경우는 {@code login(false)} 로 바꿔서 본다.
 */
class NoticeServiceTest {

    private final NoticeMapper mapper = mock(NoticeMapper.class);
    private final NoticeService service = new NoticeService(mapper);

    private static final String ME = "123456789";

    @BeforeEach
    void loginAdmin() {
        login(true);
    }

    /** 고정 사용자 홍길동(사무소 000080). admin=false 면 권한 'T'(관리자 아님) */
    private static void login(boolean admin) {
        LoginUser u = new LoginUser(ME, "홍길동", "000080", "본점", "000080", "000080", "본점", admin ? "A" : "T", admin ? "Y" : "N");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of()));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    private static String code(Throwable t) {
        return ((BusinessException) t).getCode();
    }

    private static String arg0(Throwable t) {
        return String.valueOf(((BusinessException) t).getArgs()[0]);
    }

    private static Notice notice(long no) {
        return notice(no, null, ME);
    }

    /** ofcCode null = 전체 공지 */
    private static Notice notice(long no, String ofcCode, String perNo) {
        Notice n = new Notice();
        n.setNo(no);
        n.setSubject("제목" + no);
        n.setOfcCode(ofcCode);
        n.setPerNo(perNo);
        return n;
    }

    private static NoticeSearchDto search(String from, String to, String ofc, String field, String keyword) {
        NoticeSearchDto s = new NoticeSearchDto();
        s.setFromDate(from);
        s.setToDate(to);
        s.setOfcCode(ofc);
        s.setField(field);
        s.setKeyword(keyword);
        return s;
    }

    /* ---------- 조회 ---------- */

    @Test
    void searchNormalizesConditions() {
        when(mapper.countList(any())).thenReturn(1L);
        when(mapper.selectList(any())).thenReturn(List.of(notice(3)));
        NoticeSearchDto s = search(" 20261001 ", "20261031", "80", "subject", " 점검 ");
        s.setScope("ofc");
        s.setPerNo(" ");
        PageResult<Notice> r = service.search(s);
        assertThat(r.getTotalCount()).isEqualTo(1);
        assertThat(r.getContent()).hasSize(1);
        assertThat(s.getFromDate()).isEqualTo("20261001");
        assertThat(s.getOfcCode()).isEqualTo("000080");
        assertThat(s.getPerNo()).isNull();
        assertThat(s.getField()).isEqualTo("SUBJECT");
        assertThat(s.getKeyword()).isEqualTo("점검");
        assertThat(s.getScope()).isEqualTo("OFC");
        assertThat(s.isRestrict()).isFalse();
        assertThat(s.getMyOfc()).isNull();
    }

    @Test
    void searchOfficeOnlyWhenAdminPicksOfficeScope() {
        NoticeSearchDto mine = search(null, null, "120", null, null);
        service.search(mine);
        assertThat(mine.getOfcCode()).isNull(); // 범위가 '사무소 공지' 가 아니면 사무소 칸은 버린다
        assertThat(mine.isRestrict()).isFalse();
    }

    @Test
    void searchRestrictsNonAdminToCommonAndOwnOffice() {
        login(false);
        NoticeSearchDto s = search(null, null, "120", null, null);
        s.setScope("OFC");
        s.setRestrict(false); // 요청으로 보낸 값은 무시
        s.setMyOfc("000120");
        service.search(s);
        assertThat(s.isRestrict()).isTrue();
        assertThat(s.getMyOfc()).isEqualTo("000080");
        assertThat(s.getOfcCode()).isNull(); // 다른 사무소 지정은 관리자만
        assertThat(s.getScope()).isEqualTo("OFC");
    }

    @Test
    void searchAllOrEmptyKeywordDropsKeyword() {
        NoticeSearchDto all = search(null, null, null, "ALL", "무시");
        service.search(all);
        assertThat(all.getField()).isNull();
        assertThat(all.getKeyword()).isNull();

        NoticeSearchDto blank = search(null, null, null, "TEXT", "  ");
        service.search(blank);
        assertThat(blank.getField()).isNull();
        assertThat(blank.getKeyword()).isNull();
    }

    @Test
    void searchSkipsListWhenCountIsZero() {
        when(mapper.countList(any())).thenReturn(0L);
        PageResult<Notice> r = service.search(new NoticeSearchDto());
        assertThat(r.getContent()).isEmpty();
        verify(mapper, never()).selectList(any());
    }

    @Test
    void searchRejectsBadConditions() {
        assertThatThrownBy(() -> service.search(search("2026101", null, null, null, null)))
                .satisfies(t -> assertThat(code(t)).isEqualTo("VAL_003")).satisfies(t -> assertThat(arg0(t)).isEqualTo("fromDate"));
        assertThatThrownBy(() -> service.search(search(null, "2026-10-31", null, null, null)))
                .satisfies(t -> assertThat(code(t)).isEqualTo("VAL_003")).satisfies(t -> assertThat(arg0(t)).isEqualTo("toDate"));
        // 순서가 거꾸로 / 366일 초과
        assertThatThrownBy(() -> service.search(search("20261031", "20261001", null, null, null)))
                .satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001")).satisfies(t -> assertThat(arg0(t)).isEqualTo("fromDate~toDate"));
        assertThatThrownBy(() -> service.search(search("20250101", "20260102", null, null, null)))
                .satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));
        NoticeSearchDto badOfc = search(null, null, "0000801", null, null);
        badOfc.setScope("OFC");
        assertThatThrownBy(() -> service.search(badOfc))
                .satisfies(t -> assertThat(code(t)).isEqualTo("VAL_003")).satisfies(t -> assertThat(arg0(t)).isEqualTo("ofcCode"));
        NoticeSearchDto badScope = new NoticeSearchDto();
        badScope.setScope("MINE");
        assertThatThrownBy(() -> service.search(badScope))
                .satisfies(t -> assertThat(code(t)).isEqualTo("VAL_003")).satisfies(t -> assertThat(arg0(t)).isEqualTo("scope"));
        assertThatThrownBy(() -> service.search(search(null, null, null, "WRITER", "홍")))
                .satisfies(t -> assertThat(code(t)).isEqualTo("VAL_003")).satisfies(t -> assertThat(arg0(t)).isEqualTo("field"));
        verify(mapper, never()).countList(any());
    }

    /* ---------- 1건 ---------- */

    @Test
    void getWithHitIncreasesVisitThenReads() {
        when(mapper.increaseVisit(7L)).thenReturn(1);
        when(mapper.selectOne(7L)).thenReturn(notice(7));
        assertThat(service.get(7L, true).getNo()).isEqualTo(7L);
        verify(mapper).increaseVisit(7L);

        service.get(7L, false);
        verify(mapper, times(1)).increaseVisit(7L);
    }

    @Test
    void getMissingIsNtc001NotFound() {
        assertThatThrownBy(() -> service.get(9L, false)).isInstanceOf(NotFoundException.class)
                .satisfies(t -> assertThat(code(t)).isEqualTo("NTC_001"));
        // hit 인데 없는 번호: 먼저 읽어 보고 없으면 조회수를 올리지 않는다
        assertThatThrownBy(() -> service.get(9L, true)).isInstanceOf(NotFoundException.class)
                .satisfies(t -> assertThat(code(t)).isEqualTo("NTC_001"));
        verify(mapper, never()).increaseVisit(anyLong());
    }

    @Test
    void getFillsPrevAndNextWithinVisibleNotices() {
        login(false);
        when(mapper.selectOne(7L)).thenReturn(notice(7, "000080", "999"));
        when(mapper.selectNeighbors(7L, true, "000080")).thenReturn(List.of(notice(9), notice(4)));
        Notice n = service.get(7L, false);
        assertThat(n.getPrevNo()).isEqualTo(4L);
        assertThat(n.getPrevSubject()).isEqualTo("제목4");
        assertThat(n.getNextNo()).isEqualTo(9L);
        assertThat(n.getNextSubject()).isEqualTo("제목9");
    }

    @Test
    void getOtherOfficeNoticeIsHiddenForNonAdmin() {
        login(false);
        when(mapper.selectOne(7L)).thenReturn(notice(7, "000120", "999"));
        assertThatThrownBy(() -> service.get(7L, true)).isInstanceOf(NotFoundException.class)
                .satisfies(t -> assertThat(code(t)).isEqualTo("NTC_001"));
        verify(mapper, never()).increaseVisit(anyLong());
        // 관리자는 다른 사무소 공지도 본다(앞뒤 글도 사무소 제한 없이)
        login(true);
        when(mapper.increaseVisit(7L)).thenReturn(1);
        assertThat(service.get(7L, true).getNo()).isEqualTo(7L);
        verify(mapper).selectNeighbors(7L, false, null);
    }

    /* ---------- 등록 ---------- */

    @Test
    void createUsesNextNoAndCurrentUser() {
        when(mapper.selectNextNo()).thenReturn(42L);
        when(mapper.selectOne(42L)).thenReturn(notice(42));
        Notice r = service.create(new NoticeSave("  새 공지  ", "내용", "80"));
        assertThat(r.getNo()).isEqualTo(42L);
        ArgumentCaptor<Notice> c = ArgumentCaptor.forClass(Notice.class);
        verify(mapper).insert(c.capture());
        Notice n = c.getValue();
        assertThat(n.getNo()).isEqualTo(42L);
        assertThat(n.getSubject()).isEqualTo("새 공지");
        assertThat(n.getOfcCode()).isEqualTo("000080");
        assertThat(n.getName()).isEqualTo("홍길동");
        assertThat(n.getPerNo()).isEqualTo("123456789");
    }

    @Test
    void nonAdminWritesOnlyOwnOfficeNotice() {
        login(false);
        // 전체 공지(사무소 비움)·다른 사무소 = 관리자만
        assertThatThrownBy(() -> service.create(new NoticeSave("제목", "내용", null))).isInstanceOf(ForbiddenException.class)
                .satisfies(t -> assertThat(code(t)).isEqualTo("NTC_003"));
        assertThatThrownBy(() -> service.create(new NoticeSave("제목", "내용", "120"))).isInstanceOf(ForbiddenException.class)
                .satisfies(t -> assertThat(code(t)).isEqualTo("NTC_003"));
        verify(mapper, never()).insert(any());
        when(mapper.selectNextNo()).thenReturn(3L);
        when(mapper.selectOne(3L)).thenReturn(notice(3, "000080", ME));
        assertThat(service.create(new NoticeSave("제목", "내용", "80")).getNo()).isEqualTo(3L);
    }

    @Test
    void createRetriesOnceOnDuplicateNo() {
        when(mapper.selectNextNo()).thenReturn(5L, 6L);
        doThrow(new DuplicateKeyException("PK_TB_NOTICE")).doReturn(1).when(mapper).insert(any());
        when(mapper.selectOne(6L)).thenReturn(notice(6));
        assertThat(service.create(new NoticeSave("제목", "내용", null)).getNo()).isEqualTo(6L);
        verify(mapper, times(2)).insert(any());
    }

    @Test
    void createGivesUpAfterSecondDuplicate() {
        when(mapper.selectNextNo()).thenReturn(5L, 6L);
        doThrow(new DuplicateKeyException("PK_TB_NOTICE")).when(mapper).insert(any());
        assertThatThrownBy(() -> service.create(new NoticeSave("제목", "내용", null))).isInstanceOf(DuplicateKeyException.class);
        verify(mapper, times(2)).insert(any());
    }

    @Test
    void createValidatesRequiredAndBytes() {
        assertThatThrownBy(() -> service.create(new NoticeSave(" ", "내용", null)))
                .satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001")).satisfies(t -> assertThat(arg0(t)).isEqualTo("subject"));
        assertThatThrownBy(() -> service.create(new NoticeSave("제목", "\n ", null)))
                .satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001")).satisfies(t -> assertThat(arg0(t)).isEqualTo("text"));
        // 한글 100자 = 200바이트 통과, 101자 = 202바이트 실패
        when(mapper.selectNextNo()).thenReturn(1L);
        when(mapper.selectOne(1L)).thenReturn(notice(1));
        service.create(new NoticeSave("가".repeat(100), "내용", null));
        assertThatThrownBy(() -> service.create(new NoticeSave("가".repeat(101), "내용", null)))
                .satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001")).satisfies(t -> assertThat(arg0(t)).isEqualTo("subject"));
        assertThatThrownBy(() -> service.create(new NoticeSave("제목", "a".repeat(3999) + "가", null)))
                .satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001")).satisfies(t -> assertThat(arg0(t)).isEqualTo("text"));
        assertThatThrownBy(() -> service.create(new NoticeSave("제목", "내용", "12a")))
                .satisfies(t -> assertThat(code(t)).isEqualTo("VAL_003"));
        assertThatThrownBy(() -> service.create(null)).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_004"));
    }

    /* ---------- 수정 ---------- */

    @Test
    void updateChangesOnlyEditableFields() {
        when(mapper.update(any())).thenReturn(1);
        when(mapper.selectOne(3L)).thenReturn(notice(3));
        service.update(3L, new NoticeSave("수정", "내용", ""));
        ArgumentCaptor<Notice> c = ArgumentCaptor.forClass(Notice.class);
        verify(mapper).update(c.capture());
        assertThat(c.getValue().getNo()).isEqualTo(3L);
        assertThat(c.getValue().getOfcCode()).isNull();
        assertThat(c.getValue().getName()).isNull(); // 작성자는 바꾸지 않는다
    }

    @Test
    void nonAdminUpdatesOnlyOwnNotice() {
        login(false);
        when(mapper.selectOne(3L)).thenReturn(notice(3, "000080", "999"));
        assertThatThrownBy(() -> service.update(3L, new NoticeSave("수정", "내용", "80"))).isInstanceOf(ForbiddenException.class)
                .satisfies(t -> assertThat(code(t)).isEqualTo("NTC_002"));
        // 본인 글이어도 전체 공지로 바꾸는 것은 관리자만
        when(mapper.selectOne(4L)).thenReturn(notice(4, "000080", ME));
        assertThatThrownBy(() -> service.update(4L, new NoticeSave("수정", "내용", null))).isInstanceOf(ForbiddenException.class)
                .satisfies(t -> assertThat(code(t)).isEqualTo("NTC_003"));
        verify(mapper, never()).update(any());
        when(mapper.update(any())).thenReturn(1);
        service.update(4L, new NoticeSave("수정", "내용", "80"));
        verify(mapper).update(any());
    }

    @Test
    void adminUpdatesAnyNotice() {
        when(mapper.selectOne(3L)).thenReturn(notice(3, "000120", "999"));
        when(mapper.update(any())).thenReturn(1);
        service.update(3L, new NoticeSave("수정", "내용", null));
        verify(mapper).update(any());
    }

    @Test
    void updateMissingIsNtc001() {
        when(mapper.update(any())).thenReturn(0);
        assertThatThrownBy(() -> service.update(3L, new NoticeSave("수정", "내용", null))).isInstanceOf(NotFoundException.class)
                .satisfies(t -> assertThat(code(t)).isEqualTo("NTC_001"));
    }

    /* ---------- 삭제 ---------- */

    @Test
    @SuppressWarnings("unchecked")
    void deleteDeduplicatesAndCounts() {
        when(mapper.deleteByNos(any())).thenReturn(2);
        assertThat(service.delete(new NoticeDeleteRequest(List.of(3L, 1L, 3L))).getCount()).isEqualTo(2);
        ArgumentCaptor<List<Long>> c = ArgumentCaptor.forClass(List.class);
        verify(mapper).deleteByNos(c.capture());
        assertThat(c.getValue()).containsExactly(3L, 1L);
    }

    @Test
    void deleteRejectsEmptyNullOrTooMany() {
        assertThatThrownBy(() -> service.delete(new NoticeDeleteRequest(List.of())))
                .satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001")).satisfies(t -> assertThat(arg0(t)).isEqualTo("nos"));
        assertThatThrownBy(() -> service.delete(new NoticeDeleteRequest(null))).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));
        assertThatThrownBy(() -> service.delete(null)).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));
        assertThatThrownBy(() -> service.delete(new NoticeDeleteRequest(new ArrayList<>(Arrays.asList(1L, null)))))
                .satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));
        List<Long> many = LongStream.rangeClosed(1, NoticeRules.MAX_DELETE + 1).boxed().toList();
        assertThatThrownBy(() -> service.delete(new NoticeDeleteRequest(many))).satisfies(t -> assertThat(code(t)).isEqualTo("VAL_001"));
        verify(mapper, never()).deleteByNos(any());
    }

    @Test
    void nonAdminDeletesOnlyOwnNotices() {
        login(false);
        when(mapper.selectOwners(any())).thenReturn(List.of(notice(1, "000080", ME), notice(2, null, "999")));
        assertThatThrownBy(() -> service.delete(new NoticeDeleteRequest(List.of(1L, 2L)))).isInstanceOf(ForbiddenException.class)
                .satisfies(t -> assertThat(code(t)).isEqualTo("NTC_002"));
        when(mapper.selectOwners(any())).thenReturn(List.of(notice(5, "000120", ME)));
        assertThatThrownBy(() -> service.delete(new NoticeDeleteRequest(List.of(5L)))).isInstanceOf(NotFoundException.class);
        verify(mapper, never()).deleteByNos(any());
        when(mapper.selectOwners(any())).thenReturn(List.of(notice(1, "000080", ME)));
        when(mapper.deleteByNos(any())).thenReturn(1);
        assertThat(service.delete(new NoticeDeleteRequest(List.of(1L))).getCount()).isEqualTo(1);
    }

    @Test
    void adminDeleteSkipsOwnerCheck() {
        when(mapper.deleteByNos(any())).thenReturn(1);
        service.delete(new NoticeDeleteRequest(List.of(1L)));
        verify(mapper, never()).selectOwners(any());
        verify(mapper, never()).selectNeighbors(anyLong(), anyBoolean(), any());
    }

    @Test
    void deleteNothingIsNtc001() {
        when(mapper.deleteByNos(any())).thenReturn(0);
        assertThatThrownBy(() -> service.delete(new NoticeDeleteRequest(List.of(99L)))).isInstanceOf(NotFoundException.class)
                .satisfies(t -> assertThat(code(t)).isEqualTo("NTC_001"));
    }

    /* ---------- 규칙 ---------- */

    @Test
    void rulesForScopeAndOwner() {
        assertThat(NoticeRules.canSee(notice(1, null, "9"), false, "000080")).isTrue();
        assertThat(NoticeRules.canSee(notice(1, "000080", "9"), false, "000080")).isTrue();
        assertThat(NoticeRules.canSee(notice(1, "000120", "9"), false, "000080")).isFalse();
        assertThat(NoticeRules.canSee(notice(1, "000120", "9"), false, null)).isFalse();
        assertThat(NoticeRules.canSee(notice(1, "000120", "9"), true, null)).isTrue();
        assertThat(NoticeRules.canModify(notice(1, null, ME), false, ME)).isTrue();
        assertThat(NoticeRules.canModify(notice(1, null, "9"), false, ME)).isFalse();
        assertThat(NoticeRules.canModify(notice(1, null, null), false, null)).isFalse();
        assertThat(NoticeRules.canModify(notice(1, null, "9"), true, ME)).isTrue();
        assertThat(NoticeRules.canWriteTo(null, false, "000080")).isFalse();
        assertThat(NoticeRules.canWriteTo("000080", false, "000080")).isTrue();
        assertThat(NoticeRules.canWriteTo("000120", false, "000080")).isFalse();
        assertThat(NoticeRules.canWriteTo(null, true, null)).isTrue();
    }

    @Test
    void byteLengthCountsNonAsciiAsTwo() {
        assertThat(DbBytes.length(null)).isZero();
        assertThat(DbBytes.length("abc 123")).isEqualTo(7);
        assertThat(DbBytes.length("공지")).isEqualTo(4);
        assertThat(DbBytes.length("A가\n")).isEqualTo(4);
        assertThat(DbBytes.length("😀")).isEqualTo(2); // 서로게이트 쌍 = 글자 하나
    }
}
