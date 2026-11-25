import { api } from '../http';
import type { JournalListRow, JournalListSearch } from '@/domain/journalList';
import { compactParams } from '@/utils/object';

/**
 * 저널조회(`journal.journalList`) API — API 계약 §13 (`/api/web/journal/journal-list`). 조회만(사용기록 R 은 서버가 자동).
 */
export const journalListApi = {
  /** 목록(페이지 없음, 같은 명세표는 서버가 합침, 최근 순). 2차 파기 JNL_001, 3개월 조회 허가 JNL_002 */
  list: (p: JournalListSearch) => api.get<JournalListRow[]>('/journal/journal-list', { params: compactParams(p) }),
};

export const journalListKeys = {
  all: ['journalList'] as const,
  list: (p: JournalListSearch) => ['journalList', 'list', p] as const,
};
