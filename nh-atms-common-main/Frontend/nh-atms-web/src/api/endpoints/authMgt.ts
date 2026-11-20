import { api } from '../http';
import type {
  AuthCode,
  AuthDept,
  AuthDeleteResult,
  AuthMenu,
  AuthMenuBatch,
  AuthMenuSaveResult,
  AuthUser,
  AuthUserKey,
  AuthUserSave,
  AuthUserSearch,
} from '@/domain/authMgt';
import type { PageResult } from '@/types/api';
import { compactParams } from '@/utils/object';

/** 키 3개 → 주소 조각 '/{deptCode}/{ofcCode}/{perNo}' */
const keyPath = (k: AuthUserKey) => `/${encodeURIComponent(k.deptCode)}/${encodeURIComponent(k.ofcCode)}/${encodeURIComponent(k.perNo)}`;

/**
 * 권한관리(`sample.authMgt`) API — API 계약 §8 (`/api/web/auth-mgt…`).
 * 규칙: API 호출은 이 파일에서만, 응답 타입은 domain/authMgt.ts, 쓰기 성공 문구는 서버가 보내고 http.ts 가 띄운다.
 * 화면의 onSuccess 는 `invalidateQueries({ queryKey: authMgtKeys.all })` 만.
 */
export const authMgtApi = {
  /* ---- 사용자 권한 ---- */
  /** 목록(페이징, 사무소·개인번호·부서 순) */
  users: (p: AuthUserSearch) => api.get<PageResult<AuthUser>>('/auth-mgt/users', { params: compactParams(p) }),
  /** 1건. 없으면 404 ATH_001 */
  user: (k: AuthUserKey) => api.get<AuthUser>(`/auth-mgt/users${keyPath(k)}`),
  /** 등록 — 서버 문구 S002 / 인사정보 없음 400 E404 / 중복 400 ATH_002 */
  createUser: (body: AuthUserSave) => api.post<AuthUser>('/auth-mgt/users', body),
  /** 수정(권한·승인상태·만료일시·메모) — 서버 문구 S003 */
  updateUser: (k: AuthUserKey, body: AuthUserSave) => api.put<AuthUser>(`/auth-mgt/users${keyPath(k)}`, body),
  /** 다건 삭제(1~100건) — 서버 문구 S005 '{n}건 삭제되었습니다.' */
  removeUsers: (keys: AuthUserKey[]) => api.post<AuthDeleteResult>('/auth-mgt/users/delete', { keys }),

  /** 부서 목록(TB_OFC_RAW, 최대 200) — q = 부서코드·부서명 부분일치 */
  depts: (q?: string) => api.get<AuthDept[]>('/auth-mgt/depts', { params: q ? { q } : undefined }),
  /** 그 사람의 소속 부서·사무소. 없으면 400 ATH_006 */
  personDept: (perNo: string) => api.get<AuthDept>(`/auth-mgt/person-dept/${encodeURIComponent(perNo)}`),

  /* ---- 권한코드 · 메뉴 권한 ---- */
  /** 권한코드 목록(사용자 수·메뉴 수) */
  authCodes: () => api.get<AuthCode[]>('/auth-mgt/auth-codes'),
  /** 이 권한의 메뉴 전체(트리 순서) */
  authMenus: (auth: string) => api.get<AuthMenu[]>('/auth-mgt/menus', { params: { auth } }),
  /**
   * 일괄 저장 — updated = (권한, 메뉴ID) 행이 있으면 고침(UPD)·없으면 넣음(REG). 행 삭제 없음(deleted 를 보내면 400). 서버 문구 S009.
   * 권한은 들어온 종류만 서버가 검사(없으면 403 SYS_403). 없음 404 ATH_003 (인자 '권한-메뉴ID').
   */
  saveAuthMenus: (body: AuthMenuBatch) => api.post<AuthMenuSaveResult>('/auth-mgt/menus/batch', body),
};

/** 권한관리 쿼리 키 — `authMgtKeys.all` 은 invalidate 용 루트 */
export const authMgtKeys = {
  all: ['authMgt'] as const,
  users: (p: AuthUserSearch) => ['authMgt', 'users', p] as const,
  user: (k: AuthUserKey | null) => ['authMgt', 'user', k] as const,
  authCodes: () => ['authMgt', 'authCodes'] as const,
  depts: (q: string) => ['authMgt', 'depts', q] as const,
  personDept: (perNo: string) => ['authMgt', 'personDept', perNo] as const,
  authMenus: (auth: string) => ['authMgt', 'menus', auth] as const,
};
