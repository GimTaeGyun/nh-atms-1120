/**
 * 사용기록조회(`sample.useLog`) 부품 — 조회조건·조건→파라미터·수행작업 표시·목록 컬럼·상세 항목.
 * 화면 본체 UseLogPage.tsx, 팝업 popup/UseLogViewModal.tsx 가 함께 쓴다.
 * 규칙(2026-11-17 사용자 결정, 서버 UseLogRules 와 같음): 기간 필수·최대 31일, 관리자 = 모든 사무소, 그 밖 = 내 사무소 기록만.
 */
import { useMemo } from 'react';
import { Tag, Tooltip } from 'antd';
import type { SearchField } from '@/components/search/SearchForm';
import { officeField } from '@/components/search/fields';
import { personField } from '@/components/search/lookupFields';
import { rangeField } from '@/components/search/dateFields';
import { keywordField, keywordOf, menuField, optionField } from '@/components/search/optionFields';
import { allToUndef, ymdRange } from '@/components/search/params';
import { centerCol, col, indexCol, textCol, type Col } from '@/components/grid/columns';
import { ResultTag } from '@/components/display/StatusTag';
import type { DetailItem } from '@/components/display/DetailGrid';
import { fmtDateTime14, fmtOffice, fmtPerson } from '@/utils/format';
import { compactParams } from '@/utils/object';
import type { CudType, UseLog, UseLogSearch } from '@/domain/useLog';

/* ---------- 0. 설정값 ---------- */
/** 한 번에 볼 수 있는 최대 일수(서버 UseLogRules.MAX_DAYS 와 같음) */
export const MAX_DAYS = 31;

/* ---------- 1. 수행작업·결과 표시 ---------- */
export const CUD_OPTIONS: { value: CudType; label: string }[] = [
  { value: 'R', label: '조회' },
  { value: 'C', label: '등록' },
  { value: 'U', label: '수정' },
  { value: 'D', label: '삭제' },
  { value: 'E', label: '엑셀' },
];
const CUD_COLOR: Record<CudType, string> = { R: 'default', C: 'green', U: 'blue', D: 'red', E: 'purple' };
export const cudText = (v?: CudType | null): string => CUD_OPTIONS.find((o) => o.value === v)?.label ?? (v || '-');
export const CudTag = ({ value }: { value?: CudType | null }) =>
  value ? <Tag color={CUD_COLOR[value]} style={{ marginInlineEnd: 0 }}>{cudText(value)}</Tag> : <span className="muted">-</span>;

const SUCC_OPTIONS = [
  { value: 'Y', label: '성공' },
  { value: 'N', label: '실패' },
];
export const succText = (v?: string | null): string => (v === 'Y' ? '성공' : v === 'N' ? '실패' : '');
/** 결과 — 실패면 오류코드를 말풍선으로 */
export const SuccTag = ({ r }: { r: Pick<UseLog, 'succYn' | 'err'> }) =>
  r.succYn === 'N' && r.err ? (
    <Tooltip title={`오류코드 ${r.err}`}>
      <span><ResultTag value="실패" /></span>
    </Tooltip>
  ) : (
    <ResultTag value={succText(r.succYn)} />
  );

/** 화면 글자 — 메뉴명, 메뉴를 못 찾으면 메뉴ID, 그것도 없으면 '-' */
export const menuText = (r: Pick<UseLog, 'menuNm' | 'menuId'>): string => r.menuNm || r.menuId || '-';

/* ---------- 2. 조회조건 (모두 공통 필드) ---------- */
/**
 * 기간(오늘, 최대 31일) · 수행작업 · 수행결과 · 사무소(관리자 = 고름·비우면 전체 / 그 밖 = 내 사무소 고정) · 사용자(사무소 종속) · 화면 · 사용자 IP.
 */
export const useUseLogFields = (admin: boolean, myOfc: string): SearchField[] =>
  useMemo(
    () => [
      rangeField({ label: '기간', maxDays: MAX_DAYS }),
      optionField('cudType', '수행작업', CUD_OPTIONS),
      optionField('succYn', '수행결과', SUCC_OPTIONS),
      admin ? officeField({ required: false }) : officeField({ required: false, disabled: true, defaultValue: myOfc }),
      personField({ label: '사용자', clearOnParentChange: true }),
      menuField(),
      keywordField({ name: 'ip', label: '사용자 IP', placeholder: '예 10.1.2.3' }),
    ],
    [admin, myOfc],
  );

/* ---------- 3. 조건값 → API 파라미터 ---------- */
export const toUseLogParams = (v: Record<string, unknown>): Omit<UseLogSearch, 'page' | 'size'> => {
  const { fromDate, toDate } = ymdRange(v.period);
  return compactParams<UseLogSearch>({
    fromDate,
    toDate,
    cudType: allToUndef(v.cudType) as CudType | undefined,
    succYn: allToUndef(v.succYn) as 'Y' | 'N' | undefined,
    ofcCode: allToUndef(v.ofcCode),
    perNo: allToUndef(v.perNo),
    menuId: allToUndef(v.menuId),
    ip: keywordOf(v.ip),
  }) as Omit<UseLogSearch, 'page' | 'size'>;
};

/* ---------- 4. 목록 컬럼 (G1) ---------- */
/** 일시 · 사용자 · 사무소 · 권한 · 화면 · 수행작업 · 결과 · IP (파라미터·SQL 은 상세 팝업) */
export const logColumns = (page: number, size: number): Col<UseLog>[] => [
  indexCol<UseLog>(page, size),
  centerCol<UseLog>('일시', 'crtDtm', 150, { render: (v: string | null) => fmtDateTime14(v), excel: (r) => fmtDateTime14(r.crtDtm) }),
  col<UseLog>('사용자', 'perNo', 160, { render: (_: unknown, r) => fmtPerson(r.name, r.perNo), excel: (r) => fmtPerson(r.name, r.perNo) }),
  col<UseLog>('사무소', 'ofcCode', 180, { render: (_: unknown, r) => fmtOffice(r.ofcCode, r.kanm), excel: (r) => fmtOffice(r.ofcCode, r.kanm) }),
  textCol<UseLog>('권한', 'auth', 60, { align: 'center' }),
  col<UseLog>('화면', 'menuNm', 180, { render: (_: unknown, r) => menuText(r), excel: menuText }),
  centerCol<UseLog>('수행작업', 'cudType', 80, { render: (v: CudType | null) => <CudTag value={v} />, excel: (r) => cudText(r.cudType) }),
  centerCol<UseLog>('결과', 'succYn', 80, { render: (_: unknown, r) => <SuccTag r={r} />, excel: (r) => `${succText(r.succYn)}${r.err ? ` (${r.err})` : ''}` }),
  textCol<UseLog>('사용자 IP', 'ip', 130, { align: 'center', className: 'mono' }),
];

/* ---------- 5. 상세 팝업 항목 ---------- */
export const logItems = (r: UseLog): DetailItem[] => [
  { label: '이력번호', value: <span className="mono">{r.histId}</span> },
  { label: '일시', value: fmtDateTime14(r.crtDtm) },
  { label: '사용자', value: fmtPerson(r.name, r.perNo) },
  { label: '사무소', value: fmtOffice(r.ofcCode, r.kanm) },
  { label: '부서코드', value: r.deptCode ? <span className="mono">{r.deptCode}</span> : undefined },
  { label: '권한', value: r.auth },
  { label: '화면', value: r.menuId ? `${menuText(r)} (${r.menuId})` : undefined },
  { label: '수행작업', value: <CudTag value={r.cudType} /> },
  { label: '결과', value: <span><SuccTag r={r} />{r.err ? <span className="mono" style={{ marginLeft: 6 }}>{r.err}</span> : null}</span> },
  { label: '사용자 IP', value: r.ip ? <span className="mono">{r.ip}</span> : undefined },
  { label: 'URI', value: r.uri ? <span className="mono">{r.uri}</span> : undefined, span: 2 },
  { label: '파라미터', value: r.params ? <span className="mono">{r.params}</span> : undefined, span: 2, pre: true },
];
