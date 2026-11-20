/**
 * 권한관리(`sample.authMgt`) 타입 — API 계약 §8 과 글자 하나까지 같다.
 * 사용자 권한 = TB_SCR_AUTH(키 deptCode+ofcCode+perNo), 메뉴 권한 = TB_SCR_AUTH_MENU(키 auth+menuId).
 * 2026-11-11: perNo '1' = 부서권한, 그 밖 = 개인권한. 로그인 권한은 유효한 개인권한 우선, 없으면 내 부서(TB_OFC_RAW)의 부서권한.
 * 일시는 14자리 문자열(YYYYMMDDHH24MISS). 플래그는 'Y' | 'N'.
 */
import type { Audit, BatchChanges, PageParams } from '@/types/api';
import type { Yn } from '@/types/menu';

/* ---------- 사용자 권한 (TB_SCR_AUTH) ---------- */

/** 부서권한 표시 개인번호(서버 AuthMgtRules.DEPT_PER_NO) */
export const DEPT_PER_NO = '1';

/** 사용자 권한 구분 — DEPT 부서권한(perNo '1') / PERSON 개인권한 */
export type AuthUserKind = 'DEPT' | 'PERSON';

/** `GET /api/web/auth-mgt/users` 조회조건 (q: 개인번호·성명·부서코드·부서명 부분일치) */
export interface AuthUserSearch extends PageParams {
  kind?: AuthUserKind;
  ofcCode?: string;
  auth?: string;
  apvSts?: string;
  q?: string;
}

/** 사용자 권한 1행 */
export interface AuthUser extends Audit {
  deptCode: string;
  /** 부서명(TB_OFC_RAW.SAMUSO_NAME) */
  deptNm: string | null;
  ofcCode: string;
  /** 사무소명 */
  kanm: string | null;
  perNo: string;
  /** 성명(TB_INSA.NAME) */
  name: string | null;
  auth: string;
  /** 권한명(설정 코드구분이 있을 때만, 잠정 Q-20) */
  authNm: string | null;
  /** 승인상태 'Y' 승인 / 'N' 미승인 (잠정 Q-18) */
  apvSts: string | null;
  memo: string | null;
  /** 만료일시 14자리, null = 무기한 */
  dueDate: string | null;
  /** 등록일시 14자리 */
  insDate: string | null;
  /** 등록자 개인번호 */
  insId: string | null;
}

/** 사용자 권한 키 */
export interface AuthUserKey {
  deptCode: string;
  ofcCode: string;
  perNo: string;
}

/** 부서 — TB_OFC_RAW (`GET /api/web/auth-mgt/depts`, `GET …/person-dept/{perNo}`) */
export interface AuthDept {
  /** 부서코드 = TB_SCR_AUTH.DEPT_CODE */
  samuso: string;
  samusoName: string | null;
  /** 사무소코드 = TB_SCR_AUTH.OFC_CODE */
  onlineCode: string | null;
}

/**
 * `POST /api/web/auth-mgt/users`, `PUT …/users/{deptCode}/{ofcCode}/{perNo}` 본문 (수정은 키 무시).
 * 등록: 부서권한은 perNo '1' + deptCode, 개인권한은 perNo — 사무소(와 개인권한의 부서)는 서버가 TB_OFC_RAW 로 채운다.
 */
export interface AuthUserSave {
  deptCode?: string;
  ofcCode?: string;
  perNo?: string;
  /** 필수, 1~2자 */
  auth: string;
  /** 필수, 'Y' | 'N' */
  apvSts: string;
  /** 14자리, 비우면 무기한 */
  dueDate?: string;
  /** 100바이트 이내 */
  memo?: string;
}

/** `POST /api/web/auth-mgt/users/delete` 본문 (1~100건) */
export interface AuthUserDeleteRequest {
  keys: AuthUserKey[];
}

/** `POST /api/web/auth-mgt/users/delete` 응답 */
export interface AuthDeleteResult {
  count: number;
}

/* ---------- 권한코드 · 메뉴 권한 (TB_SCR_AUTH_MENU) ---------- */

/** `GET /api/web/auth-mgt/auth-codes` 1행 */
export interface AuthCode {
  auth: string;
  authNm: string | null;
  /** 이 권한을 가진 사용자 행 수 */
  userCount: number | null;
  /** 이 권한에 등록된 메뉴 수 */
  menuCount: number | null;
}

/** `GET /api/web/auth-mgt/menus?auth=` 1행 — 메뉴 전체 × 고른 권한(트리 순서, lvl 1부터) */
export interface AuthMenu extends Audit {
  auth: string;
  menuId: string;
  parentMenuId: string | null;
  menuNm: string | null;
  /** 'G' 그룹 / 'M' 화면 */
  menuType: string | null;
  menuPath: string | null;
  sortNo: number | null;
  /** 메뉴 자체 사용여부(TB_SCR_MENU.USE_YN) */
  menuUseYn: Yn | null;
  lvl: number | null;
  /** TB_SCR_AUTH_MENU 에 행이 있으면 'Y' (화면에는 안 보임 — 저장 때 새로 넣음/고침 권한 구분용). 행이 없으면 플래그·useYn 모두 'N' */
  mappedYn: Yn;
  qryRolYn: Yn;
  regRolYn: Yn;
  updRolYn: Yn;
  delRolYn: Yn;
  exeRolYn: Yn;
  useYn: Yn;
}

/** `POST /api/web/auth-mgt/menus/batch` 본문 — added·deleted 는 쓰지 않는다(빈 배열, 행 삭제 없음 — 사용 해제 = useYn 'N') */
export type AuthMenuBatch = BatchChanges<AuthMenu>;

/** `POST /api/web/auth-mgt/menus/batch` 응답 */
export interface AuthMenuSaveResult {
  /** 새로 넣은 행 */
  inserted: number;
  /** 고친 행 */
  updated: number;
}
