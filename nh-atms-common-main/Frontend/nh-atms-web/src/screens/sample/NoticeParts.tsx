/**
 * 공지사항(`sample.notice`) 부품 — 조회조건·조건→파라미터·권한 판단·컬럼·인쇄 조건.
 * 화면 본체 NoticePage.tsx, 팝업 popup/NoticeEditModal.tsx·NoticeViewModal.tsx 가 함께 쓴다.
 * 새 "팝업형 CRUD" 화면은 이 파일을 복사해 이름만 바꿔 시작한다.
 * 공지 범위·권한(2026-11-13 사용자 결정, 서버 NoticeRules 와 같음):
 *  - 사무소코드가 빈 공지 = 전체 공지(잠정 Q-24). 사무소 공지는 그 사무소 사람 + 관리자에게만 보인다(서버가 거름).
 *  - 전체 공지·다른 사무소 공지 등록 = 관리자만(그 밖은 내 사무소 고정). 남의 글 수정·삭제 = 관리자만(그 밖은 본인 글 + UPD·DEL 권한).
 */
import { useMemo } from 'react';
import { Tag, Typography } from 'antd';
import type { SearchField } from '@/components/search/SearchForm';
import { officeField } from '@/components/search/fields';
import { rangeField } from '@/components/search/dateFields';
import { enabledBy, keywordTypeFields, keywordTypeOf, keywordTypeText, optionField } from '@/components/search/optionFields';
import { allToUndef, ymdRange } from '@/components/search/params';
import { centerCol, col, countCol, dateCol, indexCol, textCol, type Col } from '@/components/grid/columns';
import type { PrintCondition } from '@/components/print/PrintLayout';
import { fmtDate, fmtOffice } from '@/utils/format';
import { fromYmd, today, type Dayjs } from '@/utils/date';
import { compactParams } from '@/utils/object';
import type { Permission } from '@/hooks/usePermission';
import type { Notice, NoticeField, NoticeScope, NoticeSearch } from '@/domain/notice';

/* ---------- 0. 설정값 ---------- */
/** 제목 최대 바이트 — TB_NOTICE.SUBJECT VARCHAR2(200) (계산은 공통 utils/rules byteLength·maxBytes·byteCount, 잠정 Q-07) */
export const SUBJECT_MAX_BYTES = 200;
/** 내용 최대 바이트 — TB_NOTICE.TEXT VARCHAR2(4000) */
export const TEXT_MAX_BYTES = 4000;
/** 새 글 N 표시 일수 — 작성일이 오늘부터 이 일수 안이면 제목 옆 N(새 컬럼 없음, 작성일로 계산). 바꾸려면 이 값만 */
export const NEW_DAYS = 3;

/* ---------- 1. 조회조건 (모두 공통 필드) ---------- */
/** 검색구분(계약 NoticeField, '전체' 는 공통 keywordTypeFields 가 붙임) */
export const FIELD_OPTIONS: { value: Exclude<NoticeField, 'ALL'>; label: string }[] = [
  { value: 'SUBJECT', label: '제목' },
  { value: 'TEXT', label: '내용' },
  { value: 'NAME', label: '작성자' },
];

/** 공지 범위 선택지 — 관리자는 '사무소 공지'(사무소 칸으로 지정), 그 밖은 '내 사무소 공지만' */
export const scopeOptions = (admin: boolean): { value: NoticeScope; label: string }[] => [
  { value: 'COMMON', label: '전체 공지만' },
  { value: 'OFC', label: admin ? '사무소 공지' : '내 사무소 공지만' },
];
/** 공지 범위 '전체'(비움) 글자 */
export const scopeAllLabel = (admin: boolean): string => (admin ? '모든 공지' : '전체 공지 + 내 사무소');

/** 기본 기간: 최근 1개월(오늘 포함) — 빠른선택 '1개월' 과 같은 계산 */
export const lastMonth = (): [Dayjs, Dayjs] => [today().subtract(1, 'month').add(1, 'day'), today()];

/**
 * 조회조건: 작성일(최근 1개월, 최대 366일) · 공지 범위 · [관리자만] 사무소(범위 '사무소 공지' 일 때만 켜짐, 비우면 모든 사무소) · 검색구분+검색어.
 * 사무소 지정은 관리자만(2026-11-13 사용자 결정) — 관리자가 아니면 칸이 없고 서버도 무시한다.
 */
export const useNoticeFields = (admin: boolean): SearchField[] =>
  useMemo(
    () => [
      rangeField({ name: 'period', label: '작성일', maxDays: 366, defaultValue: lastMonth() }),
      optionField('scope', '공지 범위', scopeOptions(admin), { allLabel: scopeAllLabel(admin), widthClass: 'normal' }),
      ...(admin ? [enabledBy(officeField({ required: false }), 'scope', (v) => v === 'OFC')] : []),
      ...keywordTypeFields({ options: FIELD_OPTIONS }),
    ],
    [admin],
  );

/* ---------- 2. 조건값 → API 파라미터 ---------- */
/** 빈 값은 빠진다. 사무소는 범위 '사무소 공지' 일 때만, 검색구분은 검색어가 있을 때만 */
export const toNoticeParams = (v: Record<string, unknown>): Omit<NoticeSearch, 'page' | 'size'> => {
  const { fromDate, toDate } = ymdRange(v.period);
  const scope = allToUndef(v.scope) as NoticeScope | undefined;
  return compactParams<NoticeSearch>({
    fromDate,
    toDate,
    scope,
    ofcCode: scope === 'OFC' ? allToUndef(v.ofcCode) : undefined,
    ...keywordTypeOf<NoticeField>(v),
  });
};

/* ---------- 3. 권한 판단 (서버 NoticeRules 와 같음 — 버튼 흐림용, 서버가 다시 검사) ---------- */
/** 이 글을 고칠 수 있나 — 관리자 = 모두, 아니면 본인 글 + 화면 권한 UPD */
export const canEditNotice = (n: Pick<Notice, 'perNo'> | undefined, perm: Permission, myPerNo?: string): boolean =>
  !!n && (perm.isAdmin || (perm.canUpd && !!myPerNo && n.perNo === myPerNo));
/** 이 글을 지울 수 있나 — 관리자 = 모두, 아니면 본인 글 + 화면 권한 DEL */
export const canDeleteNotice = (n: Pick<Notice, 'perNo'> | undefined, perm: Permission, myPerNo?: string): boolean =>
  !!n && (perm.isAdmin || (perm.canDel && !!myPerNo && n.perNo === myPerNo));
/** 화면 위 안내 — 읽기 전용 / 본인 글만 / (관리자) 없음 */
export const permNote = (perm: Permission): string | undefined =>
  perm.isAdmin ? undefined : !perm.canReg && !perm.canUpd && !perm.canDel ? '읽기 전용 — 등록·수정·삭제 권한이 없습니다.' : '본인이 쓴 글만 수정·삭제할 수 있습니다. 등록은 내 사무소 공지만.';

/* ---------- 4. 컬럼 (G1) ---------- */
/** 범위 글자 — 전체 공지 / '[코드] 사무소명' */
export const scopeText = (r: Pick<Notice, 'ofcCode' | 'kanm'>): string => (r.ofcCode ? fmtOffice(r.ofcCode, r.kanm) : '전체');
/** 범위 표시 — 전체 공지는 파란 꼬리표 */
export const ScopeTag = ({ n }: { n: Pick<Notice, 'ofcCode' | 'kanm'> }) =>
  n.ofcCode ? <span>{scopeText(n)}</span> : <Tag color="blue" style={{ marginInlineEnd: 0 }}>전체</Tag>;

/** 새 글인가 — 작성일이 오늘부터 NEW_DAYS 일 안 */
export const isNewNotice = (wrtDate: string | null | undefined): boolean => {
  const d = fromYmd(wrtDate);
  return !!d && today().diff(d, 'day') < NEW_DAYS;
};

/** 목록 컬럼 — 제목을 누르면 상세(onOpen). 엑셀은 excelColumnsOf(이 컬럼)로 같은 모양 */
export const noticeColumns = (page: number, size: number, onOpen: (r: Notice) => void): Col<Notice>[] => [
  indexCol<Notice>(page, size),
  centerCol<Notice>('번호', 'no', 70, { className: 'num' }),
  col<Notice>('범위', 'ofcCode', 170, { align: 'center', render: (_: unknown, r) => <ScopeTag n={r} />, excel: scopeText }),
  col<Notice>('제목', 'subject', 360, {
    render: (v: string | null, r) => (
      <>
        <Typography.Link
          onClick={(e) => {
            e.stopPropagation();
            onOpen(r);
          }}
          onDoubleClick={(e) => e.stopPropagation()}
        >
          {v || '(제목 없음)'}
        </Typography.Link>
        {isNewNotice(r.wrtDate) && (
          <Tag color="red" title={`${NEW_DAYS}일 안에 쓴 글`} style={{ marginInlineStart: 6, marginInlineEnd: 0 }}>
            N
          </Tag>
        )}
      </>
    ),
  }),
  textCol<Notice>('작성자', 'name', 100, { align: 'center' }),
  dateCol<Notice>('작성일', 'wrtDate', 100),
  countCol<Notice>('조회수', 'visit', 80),
];

/* ---------- 5. 인쇄 조건 ---------- */
export const noticePrintConditions = (p: NoticeSearch | null, officeLabel: string, admin: boolean): PrintCondition[] => [
  { label: '작성일', value: p?.fromDate || p?.toDate ? `${fmtDate(p?.fromDate)} ~ ${fmtDate(p?.toDate)}` : '전체' },
  { label: '공지 범위', value: `${scopeOptions(admin).find((o) => o.value === p?.scope)?.label ?? scopeAllLabel(admin)}${p?.scope === 'OFC' && p.ofcCode ? ` (${officeLabel})` : ''}` },
  { label: '검색', value: keywordTypeText(FIELD_OPTIONS, p?.field, p?.keyword) },
];
