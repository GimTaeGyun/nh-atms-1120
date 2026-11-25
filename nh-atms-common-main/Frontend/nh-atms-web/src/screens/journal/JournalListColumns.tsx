/**
 * 저널조회(`journal.journalList`) 목록 컬럼 — As-Is 순서 10칸(G2, [상세] 칸은 명세표 팝업과 함께 나중).
 * 순번 · 저널일자 · 저널시간 · 저널구분 · 거래결과 · 거래명 · 명세표 일련번호 · 계좌/카드번호/수표번호 · 거래금액(원) · 운영내용
 */
import { centerCol, col, dateCol, stackCol, textCol, timeCol, type Col } from '@/components/grid/columns';
import type { JournalListRow } from '@/domain/journalList';
import { acctLines, amountText, jnalTypeText, opText, procRestText } from './JournalListParts';

/** 화면에서 쓰는 행 = 응답 행 + 순번·키(화면 페이징이라 순번을 미리 붙임) */
export type JournalListGridRow = JournalListRow & { no: number; key: string };

export const toRows = (rows: JournalListRow[]): JournalListGridRow[] => rows.map((r, i) => ({ ...r, no: i + 1, key: `${r.jnalDt}${r.jnalSeqno}` }));

export const journalListColumns = (): Col<JournalListGridRow>[] => [
  centerCol<JournalListGridRow>('순번', 'no', 60),
  dateCol<JournalListGridRow>('저널일자', 'jnalDt', 100),
  timeCol<JournalListGridRow>('저널시간', 'jnalTime', 90),
  centerCol<JournalListGridRow>('저널구분', 'jnalType', 90, { render: (v: string | null) => jnalTypeText(v), excel: (r) => jnalTypeText(r.jnalType) }),
  centerCol<JournalListGridRow>('거래결과', 'jrnlProcRest', 90, { render: (_: unknown, r) => procRestText(r), excel: procRestText }),
  textCol<JournalListGridRow>('거래명', 'scrNm', 160),
  centerCol<JournalListGridRow>('명세표 일련번호', 'dtlsSeqno', 140, { className: 'mono' }),
  stackCol<JournalListGridRow>('계좌/카드번호/수표번호', 220, acctLines),
  col<JournalListGridRow>('거래금액(원)', 'dealAmt', 120, { align: 'right', className: 'num', render: (_: unknown, r) => amountText(r), excel: amountText }),
  col<JournalListGridRow>('운영내용', 'aftComt', 160, { render: (_: unknown, r) => opText(r), excel: opText }),
];
