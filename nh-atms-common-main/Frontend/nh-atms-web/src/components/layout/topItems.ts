import { HOME_TOP, topNodes } from '@/stores/useNavStore';
import type { MenuTreeNode } from '@/types/menu';

/** 상단 메뉴·아이콘 줄의 칸 하나 */
export interface TopItem {
  /** 대메뉴ID (홈 = HOME_TOP) */
  id: string;
  /** 상단 메뉴 글자 = DB 메뉴명 */
  label: string;
  /** 아이콘 줄 글자(좁은 칸) = 메뉴명 앞 2글자 */
  railLabel: string;
  /** components/layout/icons.tsx ICONS 이름 */
  icon: string;
}

/** 홈 아이콘 · 대메뉴 기본 아이콘 — TB_SCR_MENU 에 아이콘 컬럼이 없어 모두 같은 아이콘(2026-11-12 사용자 결정) */
export const HOME_ICON = 'HomeOutlined';
export const TOP_MENU_ICON = 'AppstoreOutlined';

/** 상단 메뉴·아이콘 줄 = '홈'(고정) + 내 메뉴의 대메뉴(DB 순서). 메뉴 표가 프론트에 없다 — 메뉴관리 화면에서 넣으면 바로 나타난다 */
export const topItems = (menus: MenuTreeNode[]): TopItem[] => [
  { id: HOME_TOP, label: '홈', railLabel: '홈', icon: HOME_ICON },
  ...topNodes(menus).map((n) => ({ id: n.menuId, label: n.menuNm, railLabel: n.menuNm.slice(0, 2), icon: TOP_MENU_ICON })),
];
