/**
 * 저널조회(`jnal.jnalList`, TB_JL_JNAL·_ILM) 타입 — API 계약 §13 과 글자 하나까지 같다.
 */

/** 저널구분 조건 — 22 일반·33 수표·11 개국·44 합계·55 운영 (응답에는 00 도 있음) */
export type JnalTypeCode = '22' | '33' | '11' | '44' | '55';
/** 거래결과 조건 — 01 정상·02 불능·97 취소정상·98 취소불능·99 확인요망 (응답에는 77 고객조작취소도 있음) */
export type JrnlProcRestCode = '01' | '02' | '97' | '98' | '99';
/** 조회조건 — 계좌번호·수표번호·명세표일련번호 */
export type JnalField = 'ACCT' | 'CHK' | 'DTLS';

/** `GET /api/web/jnal` 조건 — 페이지 없음(서버가 합친 전체 목록) */
export interface JnalSearch {
  ofcCode: string;
  mcnNo: string;
  /** 저널일자 YYYYMMDD(최대 2일) */
  fromDate: string;
  toDate: string;
  /** 저널시간 HHmm(비면 0000~2359, 서버가 시작 00초·끝 59초) */
  fromTime?: string;
  toTime?: string;
  jnalType?: JnalTypeCode;
  jrnlProcRest?: JrnlProcRestCode;
  field?: JnalField;
  /** 계좌·수표 = 부분일치, 명세표 = '0001-0050' 또는 번호 하나 */
  keyword?: string;
}

/** 저널 한 행 — 같은 명세표의 여러 저널은 서버가 합침(rolComts·dtlComts 목록) */
export interface Jnal {
  jnalDt: string;
  jnalTime: string | null;
  jnalSeqno: string;
  ofcCode: string;
  mcnNo: string;
  jnalType: string | null;
  jrnlProcRest: string | null;
  dealDate: string | null;
  scrNo: string | null;
  scrNm: string | null;
  dtlsSeqno: string | null;
  acctNo: string | null;
  estbBankCode: string | null;
  dealAmt: number | null;
  mdnType: string | null;
  chkCnt: number | null;
  chkNo: string | null;
  chkNos: string | null;
  bprProcStat: string | null;
  faceImgYn: string | null;
  acptImgYn: string | null;
  cardImgYn: string | null;
  bfoComt: string | null;
  aftComt: string | null;
  /** 롤방식 글 — '전인자\n후인자' 를 합친 저널마다 하나 */
  rolComts: string[];
  /** 후인자만 — 합친 저널마다 하나 */
  dtlComts: string[];
  /** true = ILM 테이블에서 읽은 행 */
  ilm: boolean;
}

/** `GET /api/web/jnal/checks` 조건 = 저널 키 */
export interface JnalCheckKey {
  jnalDt: string;
  ofcCode: string;
  mcnNo: string;
  jnalSeqno: string;
}

/** 저널 수표 한 장 */
export interface JnalCheck {
  seqno: string | null;
  chkNo: string | null;
}
