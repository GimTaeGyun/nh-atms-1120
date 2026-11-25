import { useNavigate } from 'react-router-dom';
import { Button } from 'antd';
import { DownOutlined, LogoutOutlined, StarFilled, StarOutlined } from '@ant-design/icons';
import { env } from '@/config/env';
import { HOME_TOP, useNavStore } from '@/stores/useNavStore';
import { useSettingsStore } from '@/stores/useSettingsStore';
import { useFavoriteStore } from '@/stores/useFavoriteStore';
import { useAuthStore } from '@/stores/useAuthStore';
import { GlobalSearch } from './GlobalSearch';
import { topItems } from './topItems';
import { BrandMark } from './BrandMark';
import { FavoritesPopover } from './FavoritesPopover';
import { MenuIcon } from './icons';

/**
 * 헤더: 로고 + 시스템명 + 1뎁스 메뉴(홈 + DB 대메뉴) + 화면 검색 + ☆ 즐겨찾기(2026-11-13 현업 화면 기준 — 사이드 패널에서 옮김).
 * 나(이름·소속)는 사이드 패널 아래 사용자 카드에 있다. 맨 오른쪽 로그아웃. 세션 타이머는 없음(서버 세션 만료에 맡김).
 */
export const AppHeader = () => {
  const navigate = useNavigate();
  const hasFavorites = useFavoriteStore((s) => s.favorites.length > 0);
  const menus = useNavStore((s) => s.menus);
  const activeTop = useNavStore((s) => s.activeTop);
  const setActiveTop = useNavStore((s) => s.setActiveTop);
  const panelCollapsed = useNavStore((s) => s.panelCollapsed);
  const setFlyout = useNavStore((s) => s.setFlyout);
  const showTopNav = useSettingsStore((s) => s.showTopNav);
  const signOut = useAuthStore((s) => s.signOut);

  /**
   * 로그인 화면으로 먼저 옮기고(앱 셸·MeGate 가 내려가야 /api/me 를 다시 부르지 않음) 서버 세션을 끊는다.
   * 성공 문구 S013 은 서버가 보내고 자동으로 뜬다.
   */
  const logout = async () => {
    navigate('/login', { replace: true });
    await signOut();
  };

  const select = (topId: string) => {
    if (topId === HOME_TOP) {
      setActiveTop(HOME_TOP);
      navigate('/home');
      return;
    }
    setActiveTop(topId);
    if (panelCollapsed) setFlyout(topId);
  };

  return (
    <header className="app-header">
      <div className="brand" onClick={() => select('home')} role="link" aria-label="홈으로">
        <BrandMark />
        <span className="sys-name">자동화기기 종합관리시스템</span>
        <span className={`env-badge ${env.envLabel}`}>{env.envLabel.toUpperCase()}</span>
      </div>
      {showTopNav && (
        <nav className="top-nav" aria-label="대분류 메뉴">
          {topItems(menus).map((d) => (
            <button key={d.id} className={activeTop === d.id ? 'on' : ''} onClick={() => select(d.id)} type="button">
              <MenuIcon name={d.icon} />
              {d.label}
            </button>
          ))}
        </nav>
      )}
      <div className="header-right">
        <GlobalSearch />
        <FavoritesPopover placement="bottomRight">
          <button type="button" className="header-fav" aria-label="즐겨찾기">
            {hasFavorites ? <StarFilled style={{ color: 'var(--nh-gold)' }} /> : <StarOutlined />}
            <span className="fav-label">즐겨찾기</span>
            <DownOutlined />
          </button>
        </FavoritesPopover>
        <Button type="text" size="small" icon={<LogoutOutlined />} onClick={() => void logout()} data-testid="logout">
          로그아웃
        </Button>
      </div>
    </header>
  );
};
