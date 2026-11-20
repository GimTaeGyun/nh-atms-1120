/**
 * 대시보드(`main.dashboard`, 홈) 타입 — API 계약 §10 과 글자 하나까지 같다.
 * 레벨·범위·상태 판정 규칙은 서버 DashboardRules(2026-11-16 사용자 결정, 잠정 Q-25·26·27).
 */

/** CENTRAL 중앙본부(권한 A) · REGION 영업본부(T) · BRANCH 영업점(L·그 밖) */
export type DashLevel = 'CENTRAL' | 'REGION' | 'BRANCH';
/** OK 정상 · ERR 장애 · FIX 보수중 · OFF 통신단절 */
export type DashStatus = 'OK' | 'ERR' | 'FIX' | 'OFF';

export interface DashOffice {
  ofcCode: string;
  kanm: string | null;
  /** 영업본부(JUNG_MOJUM) */
  regionCode: string | null;
}

/** `GET /api/web/dashboard/scope` */
export interface DashScope {
  level: DashLevel;
  /** 내 사무소(인사 소속). 못 찾으면 null */
  myOffice: DashOffice | null;
  /** 지역 구분 콤보 목록 */
  regions: DashOffice[];
  defaultRegion: string | null;
  defaultOfcCode: string | null;
  regionEditable: boolean;
  officeEditable: boolean;
}

/** 기기상태정보 1행 */
export interface DashMachine {
  mgrOfcCode: string;
  mgrOfcNm: string | null;
  parentOfcCode: string;
  parentOfcNm: string | null;
  instPstNm: string | null;
  mcnNo: string;
  srcNm: string | null;
  vendNm: string | null;
  modelNm: string | null;
  status: DashStatus;
  statusDetail: string | null;
  address: string | null;
  mgmtVendCode: string | null;
}

/** `GET /api/web/dashboard/summary` */
export interface DashSummary {
  title: string;
  titleKind: 'BRANCH' | 'REGION' | 'ALL';
  titleCode: string | null;
  /** 조회 시각 YYYYMMDDHH24MISS */
  baseDtm: string;
  total: number;
  /** 거래 기준일(전 영업일) YYYYMMDD */
  dealDate: string;
  dealAvg: number;
  statusCount: Record<DashStatus, number>;
  machines: DashMachine[];
}

/** `GET /api/web/dashboard/summary` 조건 — 둘 다 없으면 전국 */
export interface DashSearch {
  region?: string;
  ofcCode?: string;
}
