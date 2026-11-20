package kr.co.nh.atms.web.codemgt.dto;

/** 다건 삭제 응답 {@code { count }} — 지운 행 수(S005). API 계약 §11 */
public record CodeMgtDeleteResult(int count) {}
