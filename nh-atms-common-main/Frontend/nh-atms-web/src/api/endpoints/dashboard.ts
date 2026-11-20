import { api } from '../http';
import type { DashOffice, DashScope, DashSearch, DashSummary } from '@/domain/dashboard';
import type { Notice } from '@/domain/notice';
import { compactParams } from '@/utils/object';

/**
 * 대시보드(`main.dashboard`) API — API 계약 §10 (`/api/web/dashboard…`, 화면 권한 QRY).
 * 공지 2개는 공지사항 모듈(NoticeService)을 서버가 그대로 부른다 — 공지사항 메뉴 권한 없이도 대시보드에서 보인다.
 */
export const dashboardApi = {
  /** 내 레벨·내 사무소·지역 콤보 목록·처음 고를 값 */
  scope: () => api.get<DashScope>('/dashboard/scope'),
  /** 영업본부 소속 사무소(사무소 구분 콤보) */
  offices: (region: string) => api.get<DashOffice[]>('/dashboard/offices', { params: { region } }),
  /** [조회] — 운영 대수·거래량·상태·기기 목록(범위 밖이면 403 DSH_001) */
  summary: (p: DashSearch) => api.get<DashSummary>('/dashboard/summary', { params: compactParams(p) }),
  /** 공지 최신 6건(보이는 범위 = 공지사항과 같음) */
  notices: () => api.get<Notice[]>('/dashboard/notices'),
  /** 공지 상세(조회수 +1, 이전·다음 글) */
  notice: (no: number) => api.get<Notice>(`/dashboard/notices/${no}`),
};

export const dashboardKeys = {
  all: ['dashboard'] as const,
  scope: () => ['dashboard', 'scope'] as const,
  offices: (region: string) => ['dashboard', 'offices', region] as const,
  summary: (p: DashSearch) => ['dashboard', 'summary', p] as const,
  notices: () => ['dashboard', 'notices'] as const,
};
