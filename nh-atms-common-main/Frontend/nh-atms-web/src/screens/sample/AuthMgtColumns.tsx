import { Tag } from 'antd';
import { indexCol, stackCol, textCol, col, type Col } from '@/components/grid/columns';
import type { EditableColumn } from '@/components/grid/EditableGrid';
import { codeNameLabel } from '@/components/search/lookupFields';
import { fmtDateTime14 } from '@/utils/format';
import type { AuthMenu, AuthUser, AuthUserKind } from '@/domain/authMgt';

/* ---------- 0. 길이 규칙(정본 컬럼, 서버 AuthMgtRules 와 같음) ---------- */
/** 권한 AUTH VARCHAR2(2) */
export const AUTH_MAX_LEN = 2;
/** 메모 MEMO VARCHAR2(100) — 바이트(잠정 Q-07) */
export const MEMO_MAX_BYTES = 100;

/* ---------- 1. 표시 도우미 ---------- */
/** 승인상태 옵션(잠정 Q-18 — 정본 코드표 없음, 'Y' 승인 / 'N' 미승인) */
export const APV_STS_OPTIONS = [
  { value: 'Y', label: '승인(Y)' },
  { value: 'N', label: '미승인(N)' },
];
export const apvStsLabel = (v: string | null | undefined): string => APV_STS_OPTIONS.find((o) => o.value === v)?.label ?? (v || '-');

/** '[A] 관리자' (권한명이 없으면 코드만) */
export const authLabel = (auth: string | null | undefined, authNm?: string | null): string => (auth ? codeNameLabel(auth, authNm) : '-');

/** 만료일시 — 비면 '무기한' */
export const dueText = (v: string | null | undefined): string => (v ? fmtDateTime14(v) : '무기한');

/** 사용자 권한 행 키 '부서-사무소-개인번호' (서버 AuthMgtService.keyText 와 같은 글) */
export const userKeyText = (r: Pick<AuthUser, 'deptCode' | 'ofcCode' | 'perNo'>): string => `${r.deptCode}-${r.ofcCode}-${r.perNo}`;

/* ---------- 2. 부서권한·개인권한 목록 (G1) ---------- */
/** AS-IS 그리드: 순번 · 부서정보 · 사무소정보 · 권한 · 등록정보 (+ 승인상태·만료일시·메모). 개인권한 탭만 '사용자' 칸 */
export const userColumns = (page: number, size: number, kind: AuthUserKind): Col<AuthUser>[] => [
  indexCol<AuthUser>(page, size),
  stackCol<AuthUser>('부서정보', 160, (r) => [r.deptCode, r.deptNm]),
  stackCol<AuthUser>('사무소정보', 160, (r) => [r.ofcCode, r.kanm]),
  ...(kind === 'PERSON'
    ? [col<AuthUser>('사용자', 'perNo', 150, { render: (_: unknown, r) => codeNameLabel(r.perNo, r.name), excel: (r) => codeNameLabel(r.perNo, r.name) })]
    : []),
  col<AuthUser>('권한', 'auth', 120, { align: 'center', render: (_: unknown, r) => authLabel(r.auth, r.authNm), excel: (r) => authLabel(r.auth, r.authNm) }),
  col<AuthUser>('승인상태', 'apvSts', 100, {
    align: 'center',
    render: (v: string | null) => <Tag color={v === 'Y' ? 'green' : 'default'}>{apvStsLabel(v)}</Tag>,
    excel: (r) => apvStsLabel(r.apvSts),
  }),
  col<AuthUser>('만료일시', 'dueDate', 150, { align: 'center', render: (v: string | null) => dueText(v), excel: (r) => dueText(r.dueDate) }),
  textCol<AuthUser>('메모', 'memo', 180),
  stackCol<AuthUser>('등록정보', 150, (r) => [r.insDate ? fmtDateTime14(r.insDate) : null, r.insId]),
];

/* ---------- 3. 메뉴 권한 탭: 메뉴 전체 체크 표 (G2) ---------- */
export type AuthMenuCol = EditableColumn<AuthMenu> & Col<AuthMenu>;

/** 체크 칸 하나 — 저장값 'Y'/'N' */
const flagCol = (title: string, dataIndex: keyof AuthMenu): AuthMenuCol => ({
  title,
  dataIndex,
  width: 56,
  align: 'center',
  editable: { type: 'check', checkValues: ['Y', 'N'] },
  excel: (r) => String(r[dataIndex] ?? 'N'),
});

/** 메뉴명 — 깊이만큼 들여쓰고 그룹/화면 표시 */
const MenuName = ({ r }: { r: AuthMenu }) => (
  <span style={{ paddingLeft: Math.max(0, (r.lvl ?? 1) - 1) * 16 }}>
    <Tag color={r.menuType === 'G' ? 'blue' : 'default'} style={{ marginRight: 4 }}>{r.menuType === 'G' ? '그룹' : '화면'}</Tag>
    {r.menuNm ?? r.menuId}
    {r.menuUseYn === 'N' && <span className="muted"> (메뉴 미사용)</span>}
  </span>
);

/**
 * 메뉴 권한 편집 컬럼 — 메뉴(읽기 전용) + 조회·등록·수정·삭제·실행·사용 체크(편집) + 최종변경(읽기 전용).
 * '사용' 체크 = 이 권한이 그 메뉴를 가짐. DB 에 행이 없는 메뉴는 모두 미체크 — 체크해서 [저장]하면 서버가 새로 넣는다(등록 권한 필요).
 * 행삭제는 없다: 사용을 끄고 저장하면 USE_YN 'N'(수정 권한).
 */
export const authMenuColumns = (): AuthMenuCol[] => [
  { title: '순번', key: '__idx', width: 56, align: 'center', render: (_v: unknown, _r: AuthMenu, i: number) => i + 1, excel: false },
  { title: '메뉴', dataIndex: 'menuNm', width: 220, render: (_v: unknown, r: AuthMenu) => <MenuName r={r} />, excel: (r) => `${'  '.repeat(Math.max(0, (r.lvl ?? 1) - 1))}${r.menuNm ?? r.menuId}` },
  { title: '메뉴ID', dataIndex: 'menuId', width: 90, align: 'center', className: 'mono' },
  flagCol('조회', 'qryRolYn'),
  flagCol('등록', 'regRolYn'),
  flagCol('수정', 'updRolYn'),
  flagCol('삭제', 'delRolYn'),
  flagCol('실행', 'exeRolYn'),
  flagCol('사용', 'useYn'),
  { title: '최종변경', dataIndex: 'lschgDtm', width: 150, align: 'center', render: (v: unknown) => (v ? fmtDateTime14(v as string) : ''), excel: (r) => fmtDateTime14(r.lschgDtm) },
];
