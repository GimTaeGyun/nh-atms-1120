/**
 * 저널조회(`journal.journalList`) 부품 — 선택지·표시 글자·조회조건·조건→파라미터·롤방식 글·명세서 항목.
 * 화면 본체 JournalListPage.tsx, 컬럼 JournalListColumns.tsx 가 함께 쓴다.
 * 규칙(As-Is 저널조회 화면 근거, 서버 JournalListRules 와 같음): 저널일자 최대 2일, 권한 A·T·L 만 사무소를 고름,
 * IT부문(내 사무소 000080) 비관리자는 조회조건 없음, 저널구분에 따라 조회조건 선택지가 바뀜.
 */
import { useMemo } from 'react';
import type { SearchField } from '@/components/search/SearchForm';
import { mcnField, officeField, timeRangeField } from '@/components/search/fields';
import { rangeField } from '@/components/search/dateFields';
import { keywordTypeFields, keywordTypeOf, optionField } from '@/components/search/optionFields';
import { allToUndef, timeParams, ymdRange } from '@/components/search/params';
import { fmtAcctNo, fmtAmount } from '@/utils/format';
import { compactParams } from '@/utils/object';
import type { Option } from '@/types/common';
import type { JournalListRow, JnalField, JournalListSearch, JnalTypeCode, JrnlProcRestCode } from '@/domain/journalList';

/* ---------- 0. 설정값 (서버 JournalListRules 와 같음) ---------- */
/** 한 번에 볼 수 있는 최대 일수 */
export const MAX_DAYS = 2;
/** IT부문 사무소코드 — 관리자가 아니면 조회조건(계좌·수표·명세표)을 쓰지 않는다(As-Is 2020.09 개인정보 조회 제한) */
export const IT_OFC_CODE = '000080';

/** 권한코드 첫 글자가 A·T·L 이면 사무소를 고른다(As-Is 'T, L 권한 유저에게 사무소 변경 권한 부여') */
export const canPickOffice = (auth?: string | null): boolean => ['A', 'T', 'L'].includes((auth ?? '').trim().slice(0, 1).toUpperCase());

/* ---------- 1. 선택지·표시 글자 ---------- */
export const JNAL_TYPE_OPTIONS: Option<JnalTypeCode>[] = [
  { value: '22', label: '일반' },
  { value: '33', label: '수표' },
  { value: '11', label: '개국' },
  { value: '44', label: '합계' },
  { value: '55', label: '운영' },
];

export const PROC_REST_OPTIONS: Option<JrnlProcRestCode>[] = [
  { value: '01', label: '정상' },
  { value: '02', label: '불능' },
  { value: '97', label: '취소정상' },
  { value: '98', label: '취소불능' },
  { value: '99', label: '확인요망' },
];

const FIELD_LABELS: Record<JnalField, string> = { ACCT: '계좌번호', CHK: '수표번호', DTLS: '명세표일련번호' };
const fieldOpt = (f: JnalField): Option => ({ value: f, label: FIELD_LABELS[f] });
const FIELD_OPTIONS: Option[] = [fieldOpt('ACCT'), fieldOpt('CHK'), fieldOpt('DTLS')];

/** 저널구분별 조회조건 — 전체·수표 = 계좌·수표·명세표, 일반 = 계좌·명세표, 개국·합계·운영 = 명세표(As-Is setCB_search_if) */
export const fieldOptionsOf = (jnalType: unknown): Option[] => {
  if (!jnalType || jnalType === '33') return FIELD_OPTIONS;
  if (jnalType === '22') return [fieldOpt('ACCT'), fieldOpt('DTLS')];
  return [fieldOpt('DTLS')];
};

/** 저널구분 표시 — 00·22·33 거래, 11 개국, 44 합계, 55 운영 */
export const jnalTypeText = (t?: string | null): string => {
  switch (t) {
    case '00':
    case '22':
    case '33':
      return '거래';
    case '11':
      return '개국';
    case '44':
      return '합계';
    case '55':
      return '운영';
    default:
      return '';
  }
};

const PROC_REST_TEXT: Record<string, string> = { '01': '정상', '02': '불능', '97': '취소정상', '98': '취소불능', '99': '확인요망', '77': '고객조작취소' };
/** 거래결과 표시 — 운영(55) 저널은 빈칸 */
export const procRestText = (r: Pick<JournalListRow, 'jnalType' | 'jrnlProcRest'>): string => (r.jnalType === '55' ? '' : PROC_REST_TEXT[r.jrnlProcRest ?? ''] ?? '');

/** 계좌/카드번호/수표번호 두 줄 — 계좌 하이픈, 수표가 있으면 둘째 줄 '(수표앞8[n건])' */
export const acctLines = (r: Pick<JournalListRow, 'acctNo' | 'estbBankCode' | 'chkCnt' | 'chkNos'>): string[] => [
  r.acctNo ? fmtAcctNo(r.acctNo, r.estbBankCode) : '',
  r.chkCnt && r.chkCnt > 0 ? `(${(r.chkNos ?? '').slice(0, 8)}[${r.chkCnt}건])` : '',
];

/** 거래금액 — 개국(11)·운영(55)은 빈칸 */
export const amountText = (r: Pick<JournalListRow, 'jnalType' | 'dealAmt'>): string => (r.jnalType === '11' || r.jnalType === '55' ? '' : fmtAmount(r.dealAmt, { dash: '' }));

/** 운영내용 — 운영(55) 저널의 후인자(잠정 Q-37: As-Is 원문 줄이 없어 후인자 그대로) */
export const opText = (r: Pick<JournalListRow, 'jnalType' | 'aftComt'>): string => (r.jnalType === '55' ? (r.aftComt ?? '') : '');

/** 롤방식 글 — 행마다 인자 글(합친 것 모두)을 구분선으로 이어 붙인다(잠정 Q-37) */
export const ROLL_SEPARATOR = `\n\n${'='.repeat(48)}\n\n`;
export const rollText = (rows: Pick<JournalListRow, 'rolComts'>[]): string => rows.flatMap((r) => r.rolComts).join(ROLL_SEPARATOR);

/* ---------- 2. 조회조건 (모두 공통 필드, 배치 = 피그마) ---------- */
/**
 * 1행 사무소·거래결과, 2행 기번·저널구분, 상세 필터 = 저널일자(최대 2일)·저널시간(시 단위)·조회조건·검색어.
 * canPick = 권한 A·T·L(또는 관리자) → 사무소를 고름, 아니면 내 사무소 고정. itRestricted = 조회조건·검색어 칸 없음.
 */
export const useJournalListFields = (canPick: boolean, myOfc: string, itRestricted: boolean): SearchField[] =>
  useMemo(
    () => [
      canPick ? officeField({ defaultValue: myOfc || undefined }) : officeField({ disabled: true, defaultValue: myOfc }),
      optionField('jrnlProcRest', '거래결과', PROC_REST_OPTIONS),
      mcnField({ required: true, newRow: true }),
      optionField('jnalType', '저널구분', JNAL_TYPE_OPTIONS),
      rangeField({ label: '저널일자', maxDays: MAX_DAYS, advanced: true, newRow: true }),
      ...(itRestricted
        ? []
        : keywordTypeFields({
            options: FIELD_OPTIONS,
            typeLabel: '조회조건',
            optionsBy: { watch: 'jnalType', map: fieldOptionsOf },
            keywordPlaceholder: '계좌·수표 = 일부, 명세표 = 0001-0050',
          }).map((f) => ({ ...f, advanced: true }))),
      timeRangeField({ label: '저널시간', hourly: true, advanced: true, newRow: true }),
    ],
    [canPick, myOfc, itRestricted],
  );

/* ---------- 3. 조건값 → API 파라미터 ---------- */
export const toJournalListParams = (v: Record<string, unknown>): JournalListSearch => {
  const { fromDate, toDate } = ymdRange(v.period);
  const { fromTime, toTime } = timeParams(v.time);
  const { field, keyword } = keywordTypeOf<JnalField>(v);
  return compactParams<JournalListSearch>({
    ofcCode: String(v.ofcCode ?? ''),
    mcnNo: String(v.mcnNo ?? ''),
    fromDate,
    toDate,
    fromTime,
    toTime,
    jnalType: allToUndef(v.jnalType) as JnalTypeCode | undefined,
    jrnlProcRest: allToUndef(v.jrnlProcRest) as JrnlProcRestCode | undefined,
    field,
    keyword,
  }) as JournalListSearch;
};
