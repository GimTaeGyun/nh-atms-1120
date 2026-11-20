/**
 * 메뉴관리(`sample.menuMgt`) 부품 — 메뉴 전체 읽기, 쓰기 뒤 처리, 메뉴ID 미리보기(서버 규칙과 같음),
 * 대·중메뉴 콤보 옵션(가독성: 번호 먼저 · 하위 수 · 미사용 맨 아래 · 번호/이름 검색), 조회조건.
 * 화면 본체 MenuMgtPage.tsx, 팝업 popup/MenuEditModal.tsx 가 함께 쓴다. 컬럼·표시는 MenuMgtColumns.tsx.
 */
import { Tag } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { menuMgtApi, menuMgtKeys } from '@/api/endpoints/menuMgt';
import type { SearchField } from '@/components/search/SearchForm';
import { allToUndef } from '@/components/search/params';
import { ynField } from '@/components/search/lookupFields';
import { OptionSelect, keywordField, keywordOf, optionField } from '@/components/search/optionFields';
import type { ValueProps } from '@/types/common';
import { useNavStore } from '@/stores/useNavStore';
import { compactParams } from '@/utils/object';
import type { MenuLevel, MenuMgtRow, MenuMgtSearch } from '@/domain/menuMgt';
import type { Yn } from '@/types/menu';
import { LEVEL_OPTIONS } from './MenuMgtColumns';

/* ---------- 0. 메뉴 전체 · 쓰기 뒤 ---------- */
const NO_ROWS: MenuMgtRow[] = [];
/** 조건 없는 전체 목록 — 등록 팝업 콤보와 조회조건 대메뉴 콤보가 쓴다(화면 첫 조회와 같은 캐시) */
export const ALL: MenuMgtSearch = {};

export const useAllMenus = (): { rows: MenuMgtRow[]; loading: boolean } => {
  const q = useQuery({ queryKey: menuMgtKeys.list(ALL), queryFn: () => menuMgtApi.list(ALL) });
  return { rows: q.data ?? NO_ROWS, loading: q.isFetching };
};

/**
 * 등록·수정·삭제 성공 뒤 — 목록 다시 읽기 + 왼쪽 메뉴(사이드 패널)도 다시 읽기.
 * 메뉴 테이블을 바꾸는 화면이라 사이드 패널(`/api/web/menu/my`)이 바로 따라오게 한다(다른 화면은 invalidate 만).
 */
export const useAfterMenuChange = () => {
  const qc = useQueryClient();
  const fetchMenus = useNavStore((s) => s.fetchMenus);
  return () => {
    void qc.invalidateQueries({ queryKey: menuMgtKeys.all });
    void fetchMenus(true);
  };
};

/* ---------- 1. 메뉴ID 미리보기 (서버 MenuMgtRules 와 같은 규칙 — 바꾸면 둘 다) ---------- */
const PREFIX = /^[A-Z]{3}$/;
const TOP_ID = /^[A-Z]{3}0000$/;
const MID_ID = /^[A-Z]{3}(?!00)\d{2}00$/;
const LEAF_ID = /^[A-Z]{3}(?!00)\d{2}(?!00)\d{2}$/;
/** 번호 최댓값(2자리) */
export const MAX_NO = 99;

const two = (n: number) => String(n).padStart(2, '0');

/** 상위로 고를 수 있는 메뉴인가 — 중메뉴의 상위 = 대메뉴(SSS0000), 소메뉴의 상위 = 중메뉴(SSS0100) */
export const canParent = (level: MenuLevel, id: string): boolean => (level === 2 ? TOP_ID.test(id) : level === 3 ? MID_ID.test(id) : false);

/**
 * 새 메뉴ID 미리보기 — 대메뉴 = 접두어 + 0000, 중·소메뉴 = 같은 상위 안 번호 MAX + 1(지운 번호 재사용 안 함).
 * 화면에 보여 주기만 한다. 저장 때 서버가 다시 계산해서 확정한다(동시 등록이면 서버 MNU_009).
 * @returns id 또는 못 만드는 까닭(reason)
 */
export const previewMenuId = (rows: MenuMgtRow[], level: MenuLevel | undefined, parentId?: string, prefix?: string): { id: string | null; reason?: string } => {
  if (level === 1) {
    const p = (prefix ?? '').toUpperCase();
    if (!PREFIX.test(p)) return { id: null, reason: '접두어(영문 대문자 3자)를 입력하세요.' };
    if (rows.some((r) => r.menuId.slice(0, 3) === p)) return { id: null, reason: `이미 쓰는 접두어입니다. (${p})` };
    return { id: `${p}0000` };
  }
  if (level !== 2 && level !== 3) return { id: null, reason: '레벨을 고르세요.' };
  if (!parentId) return { id: null, reason: level === 2 ? '대메뉴를 고르세요.' : '중메뉴를 고르세요.' };
  if (!canParent(level, parentId)) return { id: null, reason: `상위 메뉴로 고를 수 없는 메뉴입니다. (${parentId})` };
  const head = parentId.slice(0, level === 2 ? 3 : 5);
  const shape = level === 2 ? MID_ID : LEAF_ID;
  const at = level === 2 ? 3 : 5;
  const max = rows
    .filter((r) => r.parentMenuId === parentId && shape.test(r.menuId) && r.menuId.startsWith(head))
    .reduce((m, r) => Math.max(m, Number(r.menuId.slice(at, at + 2))), 0);
  if (max >= MAX_NO) return { id: null, reason: `메뉴 번호(01~99)를 다 써서 더 등록할 수 없습니다. (${parentId})` };
  return { id: level === 2 ? `${head}${two(max + 1)}00` : `${head}${two(max + 1)}` };
};

/** 같은 상위 안 다음 순서(가장 큰 순서 + 1). 대메뉴는 +100 */
export const nextSort = (rows: MenuMgtRow[], level: MenuLevel | undefined, parentId?: string): number => {
  const p = level === 1 ? '0' : parentId;
  if (!p) return 1;
  const max = rows.filter((r) => r.parentMenuId === p).reduce((m, r) => Math.max(m, r.sortNo ?? 0), 0);
  return level === 1 ? max + 100 : max + 1;
};

/** 메뉴경로 앞부분 '/{대분류}/' — 같은 대메뉴 아래 화면 경로에서 가져온다(없으면 빈 칸) */
export const pathHeadOf = (rows: MenuMgtRow[], topId?: string): string => {
  if (!topId) return '';
  const leaf = rows.find((r) => r.menuType === 'M' && r.menuId.slice(0, 3) === topId.slice(0, 3) && r.menuPath);
  const slug = leaf?.menuPath?.split('/')[1];
  return slug ? `/${slug}/` : '';
};

/* ---------- 2. 대·중메뉴 콤보 옵션 (가독성) ---------- */
export interface MenuOption {
  value: string;
  /** 고른 뒤 칸에 보이는 글 '[SYS] 시스템관리' · '03 공통코드' */
  label: string;
  /** 검색용 — 번호(03)·접두어·이름 */
  no: string;
  name: string;
  count: number;
  off: boolean;
  disabled?: boolean;
}

/** 미사용은 맨 아래, 나머지는 보이는 순서(SORT_NO) */
const byUse = (a: MenuMgtRow, b: MenuMgtRow) => Number(a.useYn === 'N') - Number(b.useYn === 'N') || a.sortNo - b.sortNo;

const childCount = (rows: MenuMgtRow[], id: string) => rows.filter((r) => r.parentMenuId === id).length;

/** 대메뉴 옵션 — '[SYS] 시스템관리 · 중 14' */
export const topOptions = (rows: MenuMgtRow[]): MenuOption[] =>
  rows
    .filter((r) => TOP_ID.test(r.menuId))
    .sort(byUse)
    .map((r) => ({ value: r.menuId, label: `[${r.menuId.slice(0, 3)}] ${r.menuNm}`, no: r.menuId.slice(0, 3), name: r.menuNm, count: childCount(rows, r.menuId), off: r.useYn === 'N' }));

/** 중메뉴 옵션(고른 대메뉴 것만) — '03 공통코드 · 소 12'. 번호를 다 쓴(99) 중메뉴는 고를 수 없다 */
export const midOptions = (rows: MenuMgtRow[], topId?: string): MenuOption[] =>
  !topId
    ? []
    : rows
        .filter((r) => r.parentMenuId === topId && MID_ID.test(r.menuId))
        .sort(byUse)
        .map((r) => {
          const count = childCount(rows, r.menuId);
          return { value: r.menuId, label: `${r.menuId.slice(3, 5)} ${r.menuNm}`, no: r.menuId.slice(3, 5), name: r.menuNm, count, off: r.useYn === 'N', disabled: count >= MAX_NO };
        });

/** 콤보 검색 — 숫자 1~2자리면 번호(3 = 03)로, 그 밖은 이름·접두어 부분일치(대소문자 무시) */
export const matchMenuOption = (input: string, o?: MenuOption): boolean => {
  if (!o) return false;
  const q = input.trim();
  if (!q) return true;
  if (/^\d{1,2}$/.test(q)) return o.no === two(Number(q));
  const l = q.toLowerCase();
  return o.name.toLowerCase().includes(l) || o.no.toLowerCase().includes(l);
};

/** 콤보 한 줄 — 번호(고정폭) · 이름 · 하위 수 · 미사용 */
export const MenuOptionLine = ({ o, kind }: { o: MenuOption; kind: 'top' | 'mid' }) => (
  <span style={{ display: 'flex', alignItems: 'center', gap: 8, opacity: o.off ? 0.55 : 1 }}>
    <span className="mono" style={{ minWidth: 30, fontWeight: 600 }}>{o.no}</span>
    <span style={{ flex: 1, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis' }}>{o.name}</span>
    <span className="muted" style={{ fontSize: 11 }}>{o.disabled ? '번호 다 씀' : `${kind === 'top' ? '중' : '소'} ${o.count}`}</span>
    {o.off && <Tag color="orange" style={{ marginInlineEnd: 0 }}>미사용</Tag>}
  </span>
);

/** 콤보 아래 줄 'n개 표시 / 전체 m개 · …' */
export const comboFoot = (menu: ReactNode, total: number, note: string) => (
  <>
    {menu}
    <div className="muted" style={{ fontSize: 11, padding: '6px 8px 2px', borderTop: '1px solid var(--border-light)', marginTop: 4 }}>
      전체 {total}개 · {note}
    </div>
  </>
);

/* ---------- 3. 조회조건 ---------- */
/** 조회조건 대메뉴 칸 — 공통 고정 선택지 콤보(OptionSelect, 맨 위 '전체')에 대메뉴 옵션·한 줄 모양·번호 검색만 얹는다 */
const TopMenuSelect = (p: ValueProps<string | undefined>) => {
  const { rows, loading } = useAllMenus();
  return (
    <OptionSelect<MenuOption>
      {...p}
      options={topOptions(rows)}
      optionRender={(o) => (o.data.value ? <MenuOptionLine o={o.data} kind="top" /> : o.label)}
      search={(input, o) => !o?.value || matchMenuOption(input, o)}
      loading={loading}
      ariaLabel="대메뉴"
    />
  );
};

/** 레벨 선택지(콤보 값은 글자 '1'·'2'·'3' → toMenuParams 가 숫자로) */
const LEVEL_TEXT_OPTIONS = LEVEL_OPTIONS.map((o) => ({ value: String(o.value), label: o.label }));

/** 조회조건(모두 공통 필드): 대메뉴 · 레벨 · 사용(전체로 시작) · 검색어(메뉴ID·메뉴명·경로) */
export const MENU_FIELDS: SearchField[] = [
  { name: 'topMenuId', label: '대메뉴', type: 'custom', widthClass: 'normal', defaultValue: '', render: () => <TopMenuSelect /> },
  optionField('menuLevel', '레벨', LEVEL_TEXT_OPTIONS),
  ynField('useYn', '사용', { defaultValue: '' }),
  keywordField({ placeholder: '메뉴ID·메뉴명·경로' }),
];

/** 폼 값 → 조회 파라미터(빈 값은 뺀다) */
export const toMenuParams = (v: Record<string, unknown>): MenuMgtSearch => {
  const lv = allToUndef(v.menuLevel);
  return compactParams<MenuMgtSearch>({
    topMenuId: allToUndef(v.topMenuId),
    menuLevel: lv ? (Number(lv) as MenuLevel) : undefined,
    useYn: allToUndef(v.useYn) as Yn | undefined,
    q: keywordOf(v.q),
  });
};
