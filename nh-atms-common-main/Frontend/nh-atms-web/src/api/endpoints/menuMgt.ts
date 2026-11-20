import { api } from '../http';
import type { MenuMgtDeleteRequest, MenuMgtDeleteResult, MenuMgtRow, MenuMgtSave, MenuMgtSearch } from '@/domain/menuMgt';
import { compactParams } from '@/utils/object';

/**
 * 메뉴관리(`sample.menuMgt`) API — API 계약 §9 (`/api/web/menu-mgt/…`).
 * 규칙: API 호출은 이 파일에서만, 응답 타입은 domain/menuMgt.ts, 쓰기 성공 문구는 서버가 보내고 http.ts 가 띄운다.
 */
export const menuMgtApi = {
  /** 목록(트리 순서 전체). 조건을 비우면 전체 — 등록 팝업의 대·중메뉴 콤보도 이것 */
  list: (p: MenuMgtSearch = {}) => api.get<MenuMgtRow[]>('/menu-mgt/menus', { params: compactParams(p) }),
  /** 등록 — 메뉴ID 는 서버가 정한다(MAX+1). 서버 문구 S002 */
  create: (body: MenuMgtSave) => api.post<MenuMgtRow>('/menu-mgt/menus', body),
  /** 수정(메뉴명·메뉴경로·순서·사용여부) — 서버 문구 S003 */
  update: (menuId: string, body: MenuMgtSave) => api.put<MenuMgtRow>(`/menu-mgt/menus/${encodeURIComponent(menuId)}`, body),
  /** 다건 삭제(1~100건, 권한별 메뉴 행도 함께) — 서버 문구 S005 '{n}건 삭제되었습니다.' */
  remove: (menuIds: string[]) => api.post<MenuMgtDeleteResult>('/menu-mgt/menus/delete', { menuIds } satisfies MenuMgtDeleteRequest),
};

/** 메뉴관리 쿼리 키 — `menuMgtKeys.all` 은 invalidate 용 루트 */
export const menuMgtKeys = {
  all: ['menuMgt'] as const,
  list: (p: MenuMgtSearch) => ['menuMgt', 'list', p] as const,
};
