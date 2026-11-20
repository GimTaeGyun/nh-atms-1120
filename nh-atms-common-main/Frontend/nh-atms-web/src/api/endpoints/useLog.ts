import { api } from '../http';
import type { UseLog, UseLogExcelRequest, UseLogSearch } from '@/domain/useLog';
import type { PageResult } from '@/types/api';
import { compactParams } from '@/utils/object';

/**
 * 사용기록조회(`sample.useLog`) API — API 계약 §12 (`/api/web/use-log…`).
 * 기록은 서버가 컨트롤러 메서드 이름(select·insert·update·delete)으로 자동으로 남긴다. 엑셀만 화면에서 만들어서 따로 알린다.
 */
export const useLogApi = {
  /** 목록(페이징, 최근 순) */
  list: (p: UseLogSearch) => api.get<PageResult<UseLog>>('/use-log', { params: compactParams(p) }),
  /** 1건(파라미터 원문·실행 SQL 포함). 없거나 다른 사무소 기록이면 404 ULG_001 */
  get: (histId: string) => api.get<UseLog>(`/use-log/${histId}`),
  /** 엑셀 기록(수행작업 E) — 화면 = X-Screen-Key 헤더. 문구 없음 */
  recordExcel: (body: UseLogExcelRequest) => api.post<null>('/use-log/excel', body, { quietSuccess: true }),
};

export const useLogKeys = {
  all: ['useLog'] as const,
  list: (p: UseLogSearch) => ['useLog', 'list', p] as const,
  detail: (id: string) => ['useLog', 'detail', id] as const,
};
