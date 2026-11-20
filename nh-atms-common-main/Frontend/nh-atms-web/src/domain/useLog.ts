/**
 * 사용기록조회(`sample.useLog`, TB_SCR_CUD_HIST) 타입 — API 계약 §12 과 글자 하나까지 같다.
 * 이력번호는 19자리라 JSON 숫자로는 잘려서 글자(string)로 받는다.
 */
import type { PageParams } from '@/types/api';

/** 수행작업 — R조회 C등록 U수정 D삭제 E엑셀 (정본 주석은 C·U·D, R·E 는 잠정 Q-30) */
export type CudType = 'R' | 'C' | 'U' | 'D' | 'E';

/** `GET /api/web/use-log` 조회조건 — 기간 필수(최대 31일) */
export interface UseLogSearch extends PageParams {
  fromDate: string;
  toDate: string;
  cudType?: CudType;
  succYn?: 'Y' | 'N';
  /** 관리자만 쓴다(그 밖은 서버가 내 사무소로 덮어씀) */
  ofcCode?: string;
  perNo?: string;
  menuId?: string;
  /** 사용자 IP — 완전일치 */
  ip?: string;
}

/** 사용기록 1행 — 목록에서는 params·sqlText 가 null */
export interface UseLog {
  histId: string;
  /** 일시 YYYYMMDDHH24MISS */
  crtDtm: string | null;
  deptCode: string | null;
  ofcCode: string | null;
  kanm: string | null;
  perNo: string | null;
  name: string | null;
  menuId: string | null;
  menuNm: string | null;
  cudType: CudType | null;
  succYn: 'Y' | 'N' | null;
  uri: string | null;
  ip: string | null;
  /** 기록 당시 권한코드 */
  auth: string | null;
  /** 실패 때 오류코드 */
  err: string | null;
  params: string | null;
  sqlText: string | null;
}

/** `POST /api/web/use-log/excel` — 공통 downloadExcel 이 내려받기 직전에 보낸다 */
export interface UseLogExcelRequest {
  fileName: string;
  rowCount: number;
}
