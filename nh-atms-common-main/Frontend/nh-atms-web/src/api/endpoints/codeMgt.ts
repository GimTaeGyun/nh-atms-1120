import { api } from '../http';
import type { CodeDetail, CodeDetailSave, CodeMaster, CodeMasterSave, CodeMgtDeleteRequest, CodeMgtDeleteResult, CodeMgtSearch } from '@/domain/codeMgt';
import { compactParams } from '@/utils/object';

const sect = (s: string) => `/code-mgt/masters/${encodeURIComponent(s)}`;

/**
 * 코드관리(`sample.codeMgt`) API — API 계약 §11 (`/api/web/code-mgt/…`).
 * 규칙: API 호출은 이 파일에서만, 쓰기 성공 문구(S002·S003·S005)는 서버가 보내고 http.ts 가 띄운다.
 */
export const codeMgtApi = {
  masters: (p: CodeMgtSearch = {}) => api.get<CodeMaster[]>('/code-mgt/masters', { params: compactParams(p) }),
  master: (codeSect: string) => api.get<CodeMaster>(sect(codeSect)),
  createMaster: (body: CodeMasterSave) => api.post<CodeMaster>('/code-mgt/masters', body),
  updateMaster: (codeSect: string, body: CodeMasterSave) => api.put<CodeMaster>(sect(codeSect), body),
  /** 상세코드가 남은 코드구분은 서버가 막는다(CDM_002) */
  removeMasters: (keys: string[]) => api.post<CodeMgtDeleteResult>('/code-mgt/masters/delete', { keys } satisfies CodeMgtDeleteRequest),
  details: (codeSect: string) => api.get<CodeDetail[]>(`${sect(codeSect)}/codes`),
  detail: (codeSect: string, code: string) => api.get<CodeDetail>(`${sect(codeSect)}/codes/${encodeURIComponent(code)}`),
  createDetail: (codeSect: string, body: CodeDetailSave) => api.post<CodeDetail>(`${sect(codeSect)}/codes`, body),
  updateDetail: (codeSect: string, code: string, body: CodeDetailSave) => api.put<CodeDetail>(`${sect(codeSect)}/codes/${encodeURIComponent(code)}`, body),
  removeDetails: (codeSect: string, keys: string[]) => api.post<CodeMgtDeleteResult>(`${sect(codeSect)}/codes/delete`, { keys } satisfies CodeMgtDeleteRequest),
};

/** 코드관리 쿼리 키 — `codeMgtKeys.all` 은 invalidate 용 루트 */
export const codeMgtKeys = {
  all: ['codeMgt'] as const,
  masters: (p: CodeMgtSearch) => ['codeMgt', 'masters', p] as const,
  master: (codeSect: string) => ['codeMgt', 'master', codeSect] as const,
  details: (codeSect: string) => ['codeMgt', 'details', codeSect] as const,
  detail: (codeSect: string, code: string) => ['codeMgt', 'detail', codeSect, code] as const,
};
