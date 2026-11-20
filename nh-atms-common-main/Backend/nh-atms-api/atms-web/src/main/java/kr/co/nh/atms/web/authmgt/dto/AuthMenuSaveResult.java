package kr.co.nh.atms.web.authmgt.dto;

/** 메뉴 권한 일괄 저장 응답 — 새로 넣은 행·고친 행 건수 (API 계약 §8). 행 삭제는 없다(사용 해제 = USE_YN 'N' 으로 고침) */
public record AuthMenuSaveResult(int inserted, int updated) {}
