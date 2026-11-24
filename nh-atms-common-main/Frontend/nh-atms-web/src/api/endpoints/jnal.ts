import { api } from '../http';
import type { Jnal, JnalCheck, JnalCheckKey, JnalSearch } from '@/domain/jnal';
import { compactParams } from '@/utils/object';

/**
 * 저널조회(`jnal.jnalList`) API — API 계약 §13 (`/api/web/jnal…`). 조회만(사용기록 R 은 서버가 자동).
 */
export const jnalApi = {
  /** 목록(페이지 없음, 같은 명세표는 서버가 합침, 최근 순). 2차 파기 JNL_001, 3개월 조회 허가 JNL_002 */
  list: (p: JnalSearch) => api.get<Jnal[]>('/jnal', { params: compactParams(p) }),
  /** 저널 한 건의 수표(상세 팝업 수표목록) */
  checks: (k: JnalCheckKey) => api.get<JnalCheck[]>('/jnal/checks', { params: k }),
};

export const jnalKeys = {
  all: ['jnal'] as const,
  list: (p: JnalSearch) => ['jnal', 'list', p] as const,
  checks: (k: JnalCheckKey) => ['jnal', 'checks', k] as const,
};
