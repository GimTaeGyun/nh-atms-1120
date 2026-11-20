/**
 * 코드관리(`sample.codeMgt`) 타입 — API 계약 §11 과 글자 하나까지 같다.
 * TB_CODE_MASTER(코드구분) · TB_CODE_DTL(상세코드). 코드구분은 마스터·상세 모두 10자리(2026-11-16 사용자 확인).
 */
import type { Audit } from '@/types/api';
import type { Yn } from '@/types/menu';

/** `GET /api/web/code-mgt/masters` 조회조건 — 모두 비우면 전체(페이징 없음) */
export interface CodeMgtSearch {
  /** 코드구분·코드명 부분일치 */
  q?: string;
  usedYn?: Yn;
}

/** 코드구분 1행 */
export interface CodeMaster extends Audit {
  codeSect: string;
  /** 화면 이름 '코드명' (CODE_SECT_NM) */
  codeSectNm: string;
  desc1: string | null;
  desc2: string | null;
  desc3: string | null;
  desc4: string | null;
  desc5: string | null;
  /** 업무구분 — 값 목록 미정, 직접 입력(잠정 Q-28) */
  taskSect: string | null;
  /** 처리구분 한 글자 — 값 목록 미정(잠정 Q-28) */
  prcSect: string | null;
  usedYn: Yn;
  /** 상세코드 수 */
  detailCount: number;
}

/** 상세코드 1행 */
export interface CodeDetail extends Audit {
  codeSect: string;
  code: string;
  codeNm: string;
  data1: string | null;
  data2: string | null;
  data3: string | null;
  /** NUMBER(4) */
  data4: number | null;
  /** CHAR(1) */
  data5: string | null;
}

/** 코드구분 등록(POST)·수정(PUT …/{codeSect}, codeSect 무시) 본문 */
export interface CodeMasterSave {
  codeSect?: string;
  codeSectNm: string;
  desc1?: string;
  desc2?: string;
  desc3?: string;
  desc4?: string;
  desc5?: string;
  taskSect?: string;
  prcSect?: string;
  usedYn: Yn;
}

/** 상세코드 등록(POST …/codes)·수정(PUT …/codes/{code}, code 무시) 본문 */
export interface CodeDetailSave {
  code?: string;
  codeNm: string;
  data1?: string;
  data2?: string;
  data3?: string;
  data4?: number;
  data5?: string;
}

/** 다건 삭제 본문 — 코드구분 삭제는 코드구분, 상세코드 삭제는 코드 (1~100건) */
export interface CodeMgtDeleteRequest {
  keys: string[];
}

export interface CodeMgtDeleteResult {
  count: number;
}
