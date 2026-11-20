import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { MenuNode, MenuTreeNode } from '@/types/menu';
import { menuApi } from '@/api/endpoints/menu';
import { toAppError } from '@/api/errors';
import { STORAGE_PREFIX } from '@/config/constants';
import { parseScreenPath } from '@/screens/registry';
import type { ScreenKey } from '@/screens/screenIds';

/**
 * 메뉴 상태 — 메뉴는 **DB(TB_SCR_MENU + TB_SCR_AUTH_MENU)에서만** 온다(`GET /api/web/menu/my`, 2026-11-12 사용자 결정).
 * 프론트에 대메뉴 표·내장 메뉴가 없다. 상단 메뉴·아이콘 줄 = 최상위 메뉴(대메뉴), 사이드 패널 = 그 아래 중메뉴(폴더)·소메뉴(화면).
 * 대메뉴의 키는 메뉴ID(예 'SMP0000'), 홈은 HOME_TOP.
 */

/** 상단 메뉴의 '홈' 칸 키(메뉴 테이블에 없는 고정 칸) */
export const HOME_TOP = 'home';

interface NavState {
  menus: MenuTreeNode[];
  loaded: boolean;
  loading: boolean;
  error: string | null;
  /** 상단 메뉴/아이콘 줄에서 고른 대메뉴의 메뉴ID (홈 = HOME_TOP) */
  activeTop: string;
  /** 좌측 패널 접힘 (레일만 표시) */
  panelCollapsed: boolean;
  /** 사용자가 직접 접힘을 조작했는지 (밴드 기본값 적용 여부) */
  panelUserSet: boolean;
  /** 레일 호버 플라이아웃 대상 대메뉴ID */
  flyoutTop: string | null;
  fetchMenus: (force?: boolean) => Promise<void>;
  clearMenus: () => void;
  setActiveTop: (topId: string) => void;
  setPanelCollapsed: (v: boolean, byUser?: boolean) => void;
  togglePanel: () => void;
  setFlyout: (topId: string | null) => void;
}

export const useNavStore = create<NavState>()(
  persist(
    (set, get) => ({
      menus: [],
      loaded: false,
      loading: false,
      error: null,
      activeTop: HOME_TOP,
      panelCollapsed: false,
      panelUserSet: false,
      flyoutTop: null,
      fetchMenus: async (force = false) => {
        if (get().loading || (get().loaded && !force)) return;
        set({ loading: true, error: null });
        try {
          const menus = toTree(await menuApi.my());
          set({ menus, loaded: true, loading: false });
        } catch (e) {
          set({ loading: false, error: toAppError(e).userMessage });
        }
      },
      clearMenus: () => set({ menus: [], loaded: false, error: null, activeTop: HOME_TOP }),
      setActiveTop: (topId) => set({ activeTop: topId, flyoutTop: null }),
      setPanelCollapsed: (v, byUser = true) => set({ panelCollapsed: v, panelUserSet: byUser || get().panelUserSet, flyoutTop: null }),
      togglePanel: () => set((s) => ({ panelCollapsed: !s.panelCollapsed, panelUserSet: true, flyoutTop: null })),
      setFlyout: (topId) => set({ flyoutTop: topId }),
    }),
    {
      name: `${STORAGE_PREFIX}:nav`,
      storage: createJSONStorage(() => localStorage),
      partialize: (s) => ({ panelCollapsed: s.panelCollapsed, panelUserSet: s.panelUserSet }),
    },
  ),
);

/* ---------- 셀렉터/헬퍼 ---------- */

/** 대메뉴(최상위 그룹) 목록 — 상단 메뉴·아이콘 줄. 순서는 DB SORT_NO */
export const topNodes = (menus: MenuTreeNode[]): MenuTreeNode[] => menus.filter((n) => n.level === 1 && n.menuType === 'G');

/** 대메뉴ID → 노드 */
export const nodeOfTop = (menus: MenuTreeNode[], topId: string): MenuTreeNode | undefined => topNodes(menus).find((n) => n.menuId === topId);

/** 화면(리프) + 위치 — l1 = 대메뉴명, folder = 중메뉴명, topId = 대메뉴ID */
export type ScreenLeaf = MenuTreeNode & { l1?: string; folder?: string; topId?: string };

/** 모든 화면(리프, 화면키가 있는 것) 평탄화 */
export const flattenScreens = (menus: MenuTreeNode[], acc: ScreenLeaf[] = [], top?: MenuTreeNode, folder?: string): ScreenLeaf[] => {
  for (const n of menus) {
    if (n.children.length) flattenScreens(n.children, acc, top ?? n, top ? (folder ?? n.menuNm) : undefined);
    else if (n.screenKey) acc.push({ ...n, l1: top?.menuNm, folder, topId: top?.menuId });
  }
  return acc;
};

/** 화면키로 메뉴 리프 찾기(메뉴에 없으면 undefined) */
export const findMenuByScreen = (menus: MenuTreeNode[], screenKey?: ScreenKey | string): ScreenLeaf | undefined => flattenScreens(menus).find((n) => n.screenKey === screenKey);

/**
 * 계약 MenuNode → 프론트 MenuTreeNode: 화면(M)은 menuPath 로 화면키를 찾고(레지스트리 parseScreenPath, 잠정 Q-05),
 * 사용 안 함(useYn='N') 노드는 뺀다. 형제는 sortNo 순. 화면키를 못 찾은 화면(코드 미구현)은 screenKey 없이 남는다 — 사이드 패널이 '준비 중' 으로 보인다.
 */
export const toTree = (nodes: MenuNode[] | null | undefined, level = 1): MenuTreeNode[] =>
  (nodes ?? [])
    .filter((n) => n.useYn !== 'N')
    .slice()
    .sort((a, b) => (a.sortNo ?? 0) - (b.sortNo ?? 0))
    .map((n) => {
      const { children, ...rest } = n;
      const screenKey = n.menuType === 'M' ? parseScreenPath(n.menuPath)?.screenKey : undefined;
      return { ...rest, ...(screenKey ? { screenKey } : {}), level, children: toTree(children, level + 1) };
    });
