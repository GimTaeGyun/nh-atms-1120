/**
 * 저널조회(`jnal.jnalList`) 목록 컬럼 — As-Is 순서 11칸(G2).
 * 순번 · 저널일자 · 저널시간 · 저널구분 · 거래결과 · 거래명 · 명세표 일련번호 · 계좌/카드번호/수표번호 · 거래금액(원) · 운영내용 · 상세
 */
import { Button } from 'antd';
import { centerCol, col, dateCol, stackCol, textCol, timeCol, type Col } from '@/components/grid/columns';
import type { Jnal } from '@/domain/jnal';
import { acctLines, amountText, hasDetail, jnalTypeText, opText, procRestText } from './JnalListParts';

/** 화면에서 쓰는 행 = 응답 행 + 순번·키(화면 페이징이라 순번을 미리 붙임) */
export type JnalRow = Jnal & { no: number; key: string };

export const toRows = (rows: Jnal[]): JnalRow[] => rows.map((r, i) => ({ ...r, no: i + 1, key: `${r.jnalDt}${r.jnalSeqno}` }));

export const jnalColumns = (onDetail: (r: JnalRow) => void): Col<JnalRow>[] => [
  centerCol<JnalRow>('순번', 'no', 60),
  dateCol<JnalRow>('저널일자', 'jnalDt', 100),
  timeCol<JnalRow>('저널시간', 'jnalTime', 90),
  centerCol<JnalRow>('저널구분', 'jnalType', 90, { render: (v: string | null) => jnalTypeText(v), excel: (r) => jnalTypeText(r.jnalType) }),
  centerCol<JnalRow>('거래결과', 'jrnlProcRest', 90, { render: (_: unknown, r) => procRestText(r), excel: procRestText }),
  textCol<JnalRow>('거래명', 'scrNm', 160),
  centerCol<JnalRow>('명세표 일련번호', 'dtlsSeqno', 140, { className: 'mono' }),
  stackCol<JnalRow>('계좌/카드번호/수표번호', 220, acctLines),
  col<JnalRow>('거래금액(원)', 'dealAmt', 120, { align: 'right', className: 'num', render: (_: unknown, r) => amountText(r), excel: amountText }),
  col<JnalRow>('운영내용', 'aftComt', 160, { render: (_: unknown, r) => opText(r), excel: opText }),
  {
    title: '상세',
    key: 'detail',
    width: 70,
    align: 'center',
    excel: false,
    render: (_: unknown, r: JnalRow) =>
      hasDetail(r) ? (
        <Button type="link" size="small" onClick={(e) => { e.stopPropagation(); onDetail(r); }}>
          상세
        </Button>
      ) : null,
  },
];
