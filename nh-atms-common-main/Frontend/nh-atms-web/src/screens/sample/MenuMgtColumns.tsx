/**
 * 메뉴관리(`sample.menuMgt`) 표시 부품 — 길이 상한, 레벨 표시, 메뉴ID 조각 표시(SYS·03·12), 목록 컬럼.
 * 화면 본체 MenuMgtPage.tsx, 팝업 popup/MenuEditModal.tsx 가 함께 쓴다.
 */
import { Tag } from 'antd';
import { centerCol, col, usedCol, type Col } from '@/components/grid/columns';
import { fmtDateTime14 } from '@/utils/format';
import type { MenuLevel, MenuMgtRow } from '@/domain/menuMgt';

/* ---------- 0. 길이 규칙(정본 컬럼, 서버 MenuMgtRules 와 같음) ---------- */
/** 메뉴명 MENU_NM VARCHAR2(100) — 바이트(잠정 Q-07) */
export const MENU_NM_MAX_BYTES = 100;
/** 메뉴경로 MENU_PATH VARCHAR2(300) */
export const MENU_PATH_MAX_LEN = 300;
/** 순서 SORT_NO NUMBER(5) */
export const SORT_NO_MAX = 99999;

/* ---------- 1. 레벨 ---------- */
export const LEVEL_NAME: Record<MenuLevel, string> = { 1: '대메뉴', 2: '중메뉴', 3: '소메뉴' };
const LEVEL_SHORT: Record<MenuLevel, string> = { 1: '대', 2: '중', 3: '소' };
const LEVEL_COLOR: Record<MenuLevel, string> = { 1: 'blue', 2: 'cyan', 3: 'default' };

/** 레벨 표 옵션(조회조건·등록 팝업) */
export const LEVEL_OPTIONS: { value: MenuLevel; label: string }[] = [
  { value: 1, label: '대메뉴(상단)' },
  { value: 2, label: '중메뉴(좌측)' },
  { value: 3, label: '소메뉴(화면)' },
];

/** '대'·'중'·'소' 태그 + 메뉴타입(G/M). 규칙 밖 메뉴ID 는 '규칙 밖' */
export const LevelTag = ({ level, type }: { level: MenuLevel | null; type?: 'G' | 'M' }) => (
  <span style={{ whiteSpace: 'nowrap' }}>
    {level ? <Tag color={LEVEL_COLOR[level]} style={{ marginRight: 4 }}>{LEVEL_SHORT[level]}</Tag> : <Tag style={{ marginRight: 4 }}>규칙 밖</Tag>}
    {type && <span className="muted mono">{type}</span>}
  </span>
);

/* ---------- 2. 메뉴ID 조각 표시 ---------- */
/**
 * 메뉴ID 를 접두어 · 중메뉴 번호 · 소메뉴 번호로 띄워 보인다. '00' 은 흐리게 → 레벨이 한눈에 보임.
 * 규칙 밖(7자가 아닌) ID 는 그대로.
 */
export const MenuIdText = ({ id, big }: { id: string | null | undefined; big?: boolean }) => {
  if (!id) return <span className="muted">-</span>;
  const style = { fontSize: big ? 18 : undefined, whiteSpace: 'nowrap' as const };
  if (id.length !== 7) return <span className="mono" style={style}>{id}</span>;
  const part = (v: string) => <span style={{ marginLeft: big ? 6 : 3, opacity: v === '00' ? 0.4 : 1 }}>{v}</span>;
  return (
    <span className="mono" style={style} aria-label={id}>
      <b>{id.slice(0, 3)}</b>
      {part(id.slice(3, 5))}
      {part(id.slice(5, 7))}
    </span>
  );
};

/* ---------- 3. 목록 컬럼 (G1) ---------- */
/** 메뉴명 — 레벨만큼 들여쓰기(트리 순서 목록) */
const MenuName = ({ r }: { r: MenuMgtRow }) => {
  const depth = Math.max(0, (r.menuLevel ?? 1) - 1);
  return (
    <span style={{ paddingLeft: depth * 18, fontWeight: r.menuLevel === 1 ? 700 : undefined }}>
      {depth > 0 && <span className="muted">└ </span>}
      {r.menuNm}
    </span>
  );
};

/**
 * 목록 컬럼 — 트리 순서 그대로 보이게 헤더 정렬은 끈다(DataGrid sortable={false}).
 * 메뉴ID · 메뉴명(들여쓰기) · 레벨(대/중/소 + G/M) · 상위 경로 · 하위 수 · 메뉴경로 · 순서 · 사용 · 최종변경
 */
export const menuColumns = (): Col<MenuMgtRow>[] => [
  col<MenuMgtRow>('메뉴ID', 'menuId', 110, { render: (v: string) => <MenuIdText id={v} />, excel: (r) => r.menuId }),
  col<MenuMgtRow>('메뉴명', 'menuNm', 220, { render: (_: unknown, r) => <MenuName r={r} />, excel: (r) => `${'  '.repeat(Math.max(0, (r.menuLevel ?? 1) - 1))}${r.menuNm}` }),
  centerCol<MenuMgtRow>('레벨', 'menuLevel', 80, { render: (_: unknown, r) => <LevelTag level={r.menuLevel} type={r.menuType} />, excel: (r) => (r.menuLevel ? LEVEL_NAME[r.menuLevel] : '규칙 밖') }),
  col<MenuMgtRow>('상위', 'parentPath', 200, { ellipsis: true, render: (v: string | null) => v || <span className="muted">-</span>, excel: (r) => r.parentPath ?? '' }),
  centerCol<MenuMgtRow>('하위', 'childCount', 60, { render: (v: number | null, r) => (r.menuLevel === 3 ? '' : (v ?? 0)), excel: (r) => (r.menuLevel === 3 ? '' : String(r.childCount ?? 0)) }),
  col<MenuMgtRow>('메뉴경로', 'menuPath', 200, { className: 'mono', render: (v: string | null) => v || <span className="muted">-</span>, excel: (r) => r.menuPath ?? '' }),
  centerCol<MenuMgtRow>('순서', 'sortNo', 70),
  usedCol<MenuMgtRow>('사용', 'useYn', 80),
  centerCol<MenuMgtRow>('최종변경', 'lschgDtm', 150, { render: (v: string | null) => (v ? fmtDateTime14(v) : ''), excel: (r) => fmtDateTime14(r.lschgDtm) }),
];
