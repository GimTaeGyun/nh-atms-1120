/**
 * 메뉴관리(`sample.menuMgt`, TB_SCR_MENU) 타입 — API 계약 §9 와 글자 하나까지 같다.
 * 메뉴ID = 영문 대문자 3 + 중메뉴 번호 2 + 소메뉴 번호 2 (SYS0000 대 · SYS0300 중 · SYS0312 소, 2026-11-12 사용자 결정).
 */
import type { Audit } from '@/types/api';
import type { Yn } from '@/types/menu';

/** 레벨 1 대메뉴(상단) · 2 중메뉴(좌측) · 3 소메뉴(실제 화면) */
export type MenuLevel = 1 | 2 | 3;

/** `GET /api/web/menu-mgt/menus` 조회조건 — 모두 비우면 전체(트리 순서, 페이징 없음) */
export interface MenuMgtSearch {
  /** 대메뉴ID — 그 대메뉴와 아래 메뉴만 */
  topMenuId?: string;
  menuLevel?: MenuLevel;
  useYn?: Yn;
  /** 메뉴ID·메뉴명·메뉴경로 부분일치 */
  q?: string;
}

/** 메뉴 1행 */
export interface MenuMgtRow extends Audit {
  menuId: string;
  /** 대메뉴는 '0' */
  parentMenuId: string;
  menuNm: string;
  /** G 그룹(대·중메뉴) / M 화면(소메뉴) */
  menuType: 'G' | 'M';
  /** 화면(M)만 '/sample/menu-mgt' */
  menuPath: string | null;
  /** 같은 상위 안에서 보이는 순서(메뉴ID 번호와 따로) */
  sortNo: number;
  useYn: Yn;
  /** 서버가 메뉴ID 모양으로 계산. 규칙 밖 ID 는 null */
  menuLevel: MenuLevel | null;
  /** 바로 아래 메뉴 수(목록 응답만) */
  childCount?: number | null;
  /** 상위 메뉴 이름 경로 '시스템관리 › 공통코드' (대메뉴는 null, 목록 응답만) */
  parentPath?: string | null;
}

/** `POST /api/web/menu-mgt/menus`(등록), `PUT …/menus/{menuId}`(수정 — menuNm·menuPath·sortNo·useYn 만 씀) 본문 */
export interface MenuMgtSave {
  /** 등록 때 필수 */
  menuLevel?: MenuLevel;
  /** 중메뉴 등록 = 대메뉴ID, 소메뉴 등록 = 중메뉴ID */
  parentMenuId?: string;
  /** 대메뉴 등록 때 접두어 영문 대문자 3자 */
  prefix?: string;
  /** 필수, 100바이트 이내 */
  menuNm: string;
  /** 소메뉴(화면)만 필수 '/{대분류}/{kebab 이름}' */
  menuPath?: string;
  /** 0~99999 */
  sortNo: number;
  useYn: Yn;
}

/** `POST /api/web/menu-mgt/menus/delete` 본문 (1~100건) */
export interface MenuMgtDeleteRequest {
  menuIds: string[];
}

/** `POST /api/web/menu-mgt/menus/delete` 응답 — 지운 메뉴 수, 함께 지운 권한별 메뉴(TB_SCR_AUTH_MENU) 행 수 */
export interface MenuMgtDeleteResult {
  count: number;
  authCount: number;
}
