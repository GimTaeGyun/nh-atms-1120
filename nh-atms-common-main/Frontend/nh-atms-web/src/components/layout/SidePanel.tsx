import { useMemo, useState } from 'react';
import { Button, Tooltip } from 'antd';
import { DownOutlined, DoubleLeftOutlined, DoubleRightOutlined, StarFilled, StarOutlined, SettingOutlined, HomeOutlined } from '@ant-design/icons';
import { useAuthStore } from '@/stores/useAuthStore';
import { HOME_TOP, useNavStore, nodeOfTop, topNodes } from '@/stores/useNavStore';
import { useTabStore } from '@/stores/useTabStore';
import { useFavoriteStore } from '@/stores/useFavoriteStore';
import { useOpenScreen } from '@/screens/navigation';
import { HOME_SCREEN_KEY, type ScreenKey } from '@/screens/screenIds';
import type { MenuTreeNode } from '@/types/menu';
import { MenuIcon } from './icons';
import { HOME_ICON, TOP_MENU_ICON } from './topItems';

interface Props {
  /** 대메뉴ID (홈 = HOME_TOP) */
  topId: string;
  flyout?: boolean;
  onOpenSettings: () => void;
  onMouseLeave?: () => void;
}

/** 화면 항목 — 이름만(번호 없음). ★ 는 마우스를 올리면 */
const Leaf = ({ node, active, onOpen }: { node: MenuTreeNode; active: boolean; onOpen: (key: ScreenKey) => void }) => {
  const fav = useFavoriteStore((s) => !!node.screenKey && s.favorites.includes(node.screenKey));
  const toggle = useFavoriteStore((s) => s.toggle);
  const id = node.screenKey!;
  return (
    <button type="button" className={`panel-leaf${active ? ' on' : ''}`} onClick={() => onOpen(id)} title={node.menuNm}>
      <span className="nm">{node.menuNm}</span>
      {fav ? (
        <StarFilled className="star fav" onClick={(e) => { e.stopPropagation(); toggle(id); }} />
      ) : (
        <StarOutlined className="star" onClick={(e) => { e.stopPropagation(); toggle(id); }} />
      )}
    </button>
  );
};

/** DB 에는 있지만 화면 코드(registry·screenIds)가 아직 없는 메뉴 — 눌러도 열리지 않음 */
const Pending = ({ node }: { node: MenuTreeNode }) => (
  <Tooltip title={`화면 준비 중 (${node.menuPath ?? '경로 없음'})`} placement="right">
    <button type="button" className="panel-leaf" disabled style={{ opacity: 0.5, cursor: 'not-allowed' }}>
      <span className="nm">{node.menuNm}</span>
    </button>
  </Tooltip>
);

/** 소메뉴 한 칸 — 화면키가 있으면 열리는 칸, 없으면 '준비 중' */
const Item = ({ node, activeId, onOpen }: { node: MenuTreeNode; activeId: string; onOpen: (key: ScreenKey) => void }) =>
  node.screenKey ? <Leaf node={node} active={activeId === node.screenKey} onOpen={onOpen} /> : <Pending node={node} />;

const Folder = ({ node, activeId, onOpen }: { node: MenuTreeNode; activeId: string; onOpen: (key: ScreenKey) => void }) => {
  const [open, setOpen] = useState(true);
  const leaves = node.children.filter((c) => c.menuType === 'M');
  return (
    <div className="panel-section">
      <button type="button" className={`panel-folder${open ? ' open' : ''}`} onClick={() => setOpen((v) => !v)}>
        <span>{node.menuNm}</span>
        <DownOutlined />
      </button>
      {open && leaves.map((leaf) => <Item key={leaf.menuId} node={leaf} activeId={activeId} onOpen={onOpen} />)}
    </div>
  );
};

/** 보이는 메뉴가 없을 때 — 메뉴는 DB 로만 관리하므로 메뉴관리·권한관리를 확인하라고 안내 */
const NoMenus = ({ onRetry }: { onRetry: () => void }) => (
  <div className="panel-empty">
    보이는 메뉴가 없습니다.
    <br />
    메뉴관리(메뉴)·권한관리(메뉴 권한)를 확인하세요.
    <br />
    <Button size="small" onClick={onRetry} style={{ marginTop: 6 }}>
      다시 시도
    </Button>
  </div>
);

/**
 * 사이드 패널: 대메뉴 제목(즐겨찾기 상자는 2026-11-13 헤더로 옮김) → 대메뉴 제목 → 중메뉴(폴더)/소메뉴(화면) 트리 — 전부 DB 메뉴(`/api/web/menu/my`).
 * 홈이면 홈·최근 방문. 메뉴를 못 받았거나 보이는 메뉴가 없으면 안내 + [다시 시도]. 하단은 나(고정 사용자) + 환경설정.
 */
export const SidePanel = ({ topId, flyout, onOpenSettings, onMouseLeave }: Props) => {
  const menus = useNavStore((s) => s.menus);
  const loading = useNavStore((s) => s.loading);
  const loaded = useNavStore((s) => s.loaded);
  const error = useNavStore((s) => s.error);
  const fetchMenus = useNavStore((s) => s.fetchMenus);
  const togglePanel = useNavStore((s) => s.togglePanel);
  const activeKey = useTabStore((s) => s.activeKey);
  const me = useAuthStore((s) => s.me);
  const recent = useFavoriteStore((s) => s.recent);
  const openScreen = useOpenScreen();
  const [recentOpen, setRecentOpen] = useState(true);
  const isHome = topId === HOME_TOP;
  const top = useMemo(() => (isHome ? undefined : nodeOfTop(menus, topId)), [menus, topId, isHome]);
  const noMenus = loaded && !error && topNodes(menus).length === 0;
  const allLeaves = useMemo(() => {
    const acc: MenuTreeNode[] = [];
    const walk = (ns: MenuTreeNode[]) => ns.forEach((n) => (n.children.length ? walk(n.children) : n.screenKey && acc.push(n)));
    walk(menus);
    return acc;
  }, [menus]);
  const byId = (key: string) => allLeaves.find((l) => l.screenKey === key);

  return (
    <aside className={`app-panel${flyout ? ' flyout' : ''}`} onMouseLeave={onMouseLeave} aria-label="메뉴 패널">
      <div className="panel-head">
        <MenuIcon name={isHome ? HOME_ICON : TOP_MENU_ICON} />
        <span>{isHome ? '홈' : (top?.menuNm ?? '메뉴')}</span>
        <span className="panel-head-actions">
          {!flyout && (
            <Tooltip title="패널 접기" placement="right">
              <Button className="collapse-btn" type="text" size="small" icon={<DoubleLeftOutlined />} onClick={togglePanel} aria-label="패널 접기" />
            </Tooltip>
          )}
          {flyout && (
            <Tooltip title="패널 고정" placement="right">
              <Button className="collapse-btn" type="text" size="small" icon={<DoubleRightOutlined />} onClick={togglePanel} aria-label="패널 고정" />
            </Tooltip>
          )}
        </span>
      </div>
      <div className="panel-body">
        {isHome ? (
          <>
            <button type="button" className={`panel-leaf${activeKey === HOME_SCREEN_KEY ? ' on' : ''}`} onClick={() => openScreen(HOME_SCREEN_KEY)} style={{ fontWeight: 700 }}>
              <HomeOutlined /> <span className="nm">홈</span>
            </button>
            <button type="button" className={`panel-folder${recentOpen ? ' open' : ''}`} onClick={() => setRecentOpen((v) => !v)}>
              <span>최근 방문한 페이지</span>
              <DownOutlined />
            </button>
            {recentOpen && (recent.length === 0 ? <div className="panel-empty">최근 방문한 화면이 없습니다.</div> : recent.map((r) => byId(r.screenKey)).filter(Boolean).map((n) => <Leaf key={n!.menuId} node={n!} active={activeKey === n!.screenKey} onOpen={openScreen} />))}
            {noMenus && <NoMenus onRetry={() => fetchMenus(true)} />}
          </>
        ) : loading && !top ? (
          <div className="panel-empty">메뉴를 불러오는 중…</div>
        ) : error && !top ? (
          <div className="panel-empty">
            {error}
            <br />
            <Button size="small" onClick={() => fetchMenus(true)} style={{ marginTop: 6 }}>
              다시 시도
            </Button>
          </div>
        ) : !top || !top.children.length ? (
          <NoMenus onRetry={() => fetchMenus(true)} />
        ) : (
          top.children.map((folder) => (folder.menuType === 'G' ? <Folder key={folder.menuId} node={folder} activeId={activeKey} onOpen={openScreen} /> : <Item key={folder.menuId} node={folder} activeId={activeKey} onOpen={openScreen} />))
        )}
      </div>
      <div className="panel-foot">
        <div className="user-card">
          <div className="avatar">{me?.name?.slice(0, 1) ?? 'U'}</div>
          <div style={{ minWidth: 0 }}>
            <div className="nm ellipsis">{me?.name ?? '-'}</div>
            <div className="dept ellipsis">{me?.kanm ?? me?.samusoName ?? ''}</div>
          </div>
        </div>
        <div className="panel-foot-actions">
          <button type="button" className="panel-settings" onClick={onOpenSettings}>
            <SettingOutlined /> 환경설정
          </button>
        </div>
      </div>
    </aside>
  );
};
