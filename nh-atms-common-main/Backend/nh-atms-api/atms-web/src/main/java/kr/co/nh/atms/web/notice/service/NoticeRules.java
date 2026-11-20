package kr.co.nh.atms.web.notice.service;

import kr.co.nh.atms.web.notice.dto.Notice;
import kr.co.nh.atms.web.notice.mapper.NoticeMapper;

import java.util.Objects;

/**
 * 공지사항 규칙 모음 — 잠정 규칙은 메서드 하나씩(정본·현업 답이 오면 여기만 고친다).
 * <p>
 * 공지 범위·권한(2026-11-13 사용자 결정):
 * <ul>
 *   <li>사무소코드가 빈 공지 = <b>전체 공지</b>(모든 사무소에 보임, 잠정 Q-24). 사무소가 있는 공지 = 그 사무소 사람 + 관리자에게만 보임.</li>
 *   <li>전체 공지·다른 사무소 공지 등록 = 관리자만. 관리자가 아니면 내 사무소 공지만 쓴다(403 {@code NTC_003}).</li>
 *   <li>남의 글 수정·삭제 = 관리자만. 관리자가 아니면 본인 글만, 그것도 화면 권한 UPD·DEL 이 있을 때만(403 {@code NTC_002}).</li>
 * </ul>
 * 관리자 = {@code CurrentUser.isAdmin()}(권한코드 'A', 잠정 Q-09). 내 사무소 = {@code CurrentUser.ofcCode()}(권한 사무소).
 */
public final class NoticeRules {
    private NoticeRules() {}

    /** 제목 최대 바이트 — SUBJECT VARCHAR2(200), 바이트 계산은 공통 DbBytes(잠정 Q-07) */
    public static final int SUBJECT_MAX_BYTES = 200;

    /** 내용 최대 바이트 — TEXT VARCHAR2(4000) */
    public static final int TEXT_MAX_BYTES = 4000;

    /** 한 번에 지울 수 있는 최대 건수(API 계약 §3, 1~100건) */
    public static final int MAX_DELETE = 100;

    /** 작성일 기간 최대 일수(양끝 포함, 설계 §8.2 "최대 1년") */
    public static final int MAX_SEARCH_DAYS = 366;

    /**
     * 다음 공지 번호.
     * <p>
     * <b>잠정 Q-02</b>: TO-BE 정본 TB_NOTICE 에 시퀀스가 없어 원본 선례대로 {@code NVL(MAX(NO),0)+1}
     * (매퍼 {@code selectNextNo}). 동시에 등록하면 같은 번호가 나와 PK 충돌이 날 수 있다 →
     * NoticeService 가 DuplicateKeyException 이면 한 번만 다시 번호를 받아 넣는다. 채번 규칙은 현업 확인 대기.
     */
    public static long nextNo(NoticeMapper mapper) {
        return mapper.selectNextNo();
    }

    /** 조회조건 공지 범위 — 전체 공지만 */
    public static final String SCOPE_COMMON = "COMMON";

    /** 조회조건 공지 범위 — 사무소 공지만(관리자는 ofcCode 로 사무소를 고름, 아니면 내 사무소) */
    public static final String SCOPE_OFC = "OFC";

    /**
     * 이 공지를 볼 수 있나 — 관리자 = 모두, 아니면 전체 공지(사무소 빈 것) 또는 내 사무소 공지.
     * <p><b>잠정 Q-24</b>: "사무소코드가 빈 공지 = 전체 공지" 는 정본에 없는 해석이다. 현업 답이 다르면 이 메서드와 매퍼 {@code visible} 을 같이 고친다.
     */
    public static boolean canSee(Notice n, boolean admin, String myOfc) {
        return admin || n.getOfcCode() == null || Objects.equals(n.getOfcCode(), myOfc);
    }

    /** 이 공지를 고치거나 지울 수 있나(화면 권한 UPD·DEL 은 컨트롤러가 따로 본다) — 관리자 = 모두, 아니면 본인 글(개인번호 같음)만 */
    public static boolean canModify(Notice n, boolean admin, String myPerNo) {
        return admin || (myPerNo != null && myPerNo.equals(n.getPerNo()));
    }

    /**
     * 등록·수정 때 쓸 사무소인가 — 관리자 = 아무 사무소·빈 값(전체 공지) 모두, 아니면 내 사무소만.
     * @param ofcCode 정리된(6자리) 요청 사무소, 비면 null(= 전체 공지)
     */
    public static boolean canWriteTo(String ofcCode, boolean admin, String myOfc) {
        return admin || (ofcCode != null && ofcCode.equals(myOfc));
    }
}
