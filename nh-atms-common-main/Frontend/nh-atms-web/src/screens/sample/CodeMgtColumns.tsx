/**
 * 코드관리(`sample.codeMgt`) 표시 부품 — 길이 상한(서버 CodeMgtRules 와 같음), 표 컬럼, 상세 항목.
 * 화면 본체 CodeMgtPage.tsx, 팝업 popup/CodeMgtModals.tsx 가 함께 쓴다.
 */
import { centerCol, indexCol, textCol, usedCol, type Col } from '@/components/grid/columns';
import { UseYnTag } from '@/components/display/StatusTag';
import type { DetailItem } from '@/components/display/DetailGrid';
import { fmtDateTime14 } from '@/utils/format';
import type { CodeDetail, CodeMaster } from '@/domain/codeMgt';

/* ---------- 0. 길이 규칙(정본 컬럼 크기) ---------- */
export const KEY_MAX_LEN = 10;
export const SECT_NM_MAX_BYTES = 100;
export const DESC_MAX_BYTES = 100;
export const TASK_SECT_MAX_BYTES = 10;
export const CODE_NM_MAX_BYTES = 80;
export const DATA_MAX_BYTES = 80;
export const DATA4_MIN = -9999;
export const DATA4_MAX = 9999;

/* ---------- 1. 표 컬럼 ---------- */
/** 왼쪽 코드구분 — 순번 · 코드구분 · 코드명 · 사용여부(+체크), 나머지는 상세 팝업(2026-11-16 사용자 결정, 순번 같은 날 추가) */
export const masterColumns = (): Col<CodeMaster>[] => [
  indexCol<CodeMaster>(1, 0, 52),
  centerCol<CodeMaster>('코드구분', 'codeSect', 110, { className: 'mono' }),
  textCol<CodeMaster>('코드명', 'codeSectNm', 180),
  usedCol<CodeMaster>('사용여부', 'usedYn', 76),
];

/** 오른쪽 상세코드 — 순번 · 코드 · 코드명 · 데이터1~5 모두(칸 이름 고정, 2026-11-16 사용자 결정 — 데이터4·5 는 좁게, 정렬은 왼쪽) */
export const detailColumns = (): Col<CodeDetail>[] => [
  indexCol<CodeDetail>(1, 0, 52),
  centerCol<CodeDetail>('코드', 'code', 90, { className: 'mono' }),
  textCol<CodeDetail>('코드명', 'codeNm', 180),
  textCol<CodeDetail>('데이터1', 'data1', 130),
  textCol<CodeDetail>('데이터2', 'data2', 130),
  textCol<CodeDetail>('데이터3', 'data3', 130),
  textCol<CodeDetail>('데이터4', 'data4', 80),
  textCol<CodeDetail>('데이터5', 'data5', 70),
];

/* ---------- 2. 상세 팝업 항목 ---------- */
const audit = (dtm?: string, who?: string) => (dtm ? `${fmtDateTime14(dtm)}${who ? ` · ${who}` : ''}` : undefined);

export const masterItems = (m: CodeMaster): DetailItem[] => [
  { label: '코드구분', value: <span className="mono">{m.codeSect}</span> },
  { label: '코드명', value: m.codeSectNm },
  { label: '사용여부', value: <UseYnTag value={m.usedYn} /> },
  { label: '상세코드', value: `${m.detailCount ?? 0}건`, num: true },
  { label: '업무구분', value: m.taskSect },
  { label: '처리구분', value: m.prcSect },
  { label: '설명1', value: m.desc1, span: 2 },
  { label: '설명2', value: m.desc2, span: 2 },
  { label: '설명3', value: m.desc3, span: 2 },
  { label: '설명4', value: m.desc4, span: 2 },
  { label: '설명5', value: m.desc5, span: 2 },
  { label: '등록', value: audit(m.crtDtm, m.crtCmeno) },
  { label: '최종변경', value: audit(m.lschgDtm, m.lsCmeno) },
];

export const detailItems = (d: CodeDetail): DetailItem[] => [
  { label: '코드구분', value: <span className="mono">{d.codeSect}</span> },
  { label: '코드', value: <span className="mono">{d.code}</span> },
  { label: '코드명', value: d.codeNm, span: 2 },
  { label: '데이터1', value: d.data1, span: 2 },
  { label: '데이터2', value: d.data2, span: 2 },
  { label: '데이터3', value: d.data3, span: 2 },
  { label: '데이터4', value: d.data4 ?? undefined, num: true },
  { label: '데이터5', value: d.data5 },
  { label: '등록', value: audit(d.crtDtm, d.crtCmeno) },
  { label: '최종변경', value: audit(d.lschgDtm, d.lsCmeno) },
];
