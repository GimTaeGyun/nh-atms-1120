/**
 * 공지사항 견본(`sample.notice`, TB_NOTICE) 타입 — API 계약 §3 과 글자 하나까지 같다.
 * 필드 = 정본 컬럼 camelCase. `wrtDate` 는 DB DATE 를 서버가 'YYYYMMDD' 문자열로 내린다.
 */
import type { Audit, PageParams } from '@/types/api';

/** 검색구분 */
export type NoticeField = 'ALL' | 'SUBJECT' | 'TEXT' | 'NAME';

/**
 * 공지 범위(조회조건) — 비움 = 볼 수 있는 공지 모두(관리자 = 모든 공지, 그 밖 = 전체 공지 + 내 사무소),
 * COMMON = 전체 공지(사무소 빈 것)만, OFC = 사무소 공지만(관리자는 ofcCode 로 사무소 지정, 그 밖은 내 사무소)
 */
export type NoticeScope = 'COMMON' | 'OFC';

/** `GET /api/notice` 조회조건 */
export interface NoticeSearch extends PageParams {
  /** 작성일 시작 YYYYMMDD */
  fromDate?: string;
  /** 작성일 끝 YYYYMMDD */
  toDate?: string;
  /** 사무소 지정 — 관리자 + scope 'OFC' 일 때만 서버가 쓴다 */
  ofcCode?: string;
  perNo?: string;
  field?: NoticeField;
  keyword?: string;
  scope?: NoticeScope;
}

/** 공지 1건 — 목록 응답에서는 text 가 null */
export interface Notice extends Audit {
  no: number;
  /** null = 전체 공지(모든 사무소에 보임, 잠정 Q-24) */
  ofcCode: string | null;
  /** 사무소명(TB_OFC_REG_MGT.KANM) */
  kanm: string | null;
  subject: string | null;
  text: string | null;
  /** 조회수 */
  visit: number | null;
  /** 작성자 성명 */
  name: string | null;
  perNo: string | null;
  /** 작성일 YYYYMMDD */
  wrtDate: string | null;
  /** 상세에서만 — 이전 글(번호가 바로 작은 것, 내가 볼 수 있는 공지 안). 목록·없음 = null */
  prevNo: number | null;
  prevSubject: string | null;
  /** 상세에서만 — 다음 글(번호가 바로 큰 것) */
  nextNo: number | null;
  nextSubject: string | null;
}

/** `POST /api/notice`, `PUT /api/notice/{no}` 본문 */
export interface NoticeSave {
  /** 필수, 200바이트 이내 */
  subject: string;
  /** 필수, 4000바이트 이내 */
  text: string;
  /** 6자리. 비우면 전체 공지(관리자만, 아니면 403 NTC_003) */
  ofcCode?: string;
}

/** `POST /api/notice/delete` 본문 (1~100건) */
export interface NoticeDeleteRequest {
  nos: number[];
}

/** `POST /api/notice/delete` 응답 */
export interface NoticeDeleteResult {
  count: number;
}
