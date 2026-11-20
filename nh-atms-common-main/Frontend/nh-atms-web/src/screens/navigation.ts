import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTabStore } from '@/stores/useTabStore';
import { HOME_TOP, useNavStore, findMenuByScreen } from '@/stores/useNavStore';
import { useFavoriteStore } from '@/stores/useFavoriteStore';
import { MAX_OPEN_TABS } from '@/config/constants';
import { notifyError } from '@/hooks/useAppMessage';
import { getScreen, screenPath } from './registry';
import { HOME_SCREEN_KEY, type ScreenKey } from './screenIds';

/** 화면키로 탭 열기 + 라우팅 (메뉴/즐겨찾기/검색/화면 내 링크 공통 진입점). 탭 제목 = DB 메뉴명(메뉴에 없으면 registry 이름) */
export const useOpenScreen = () => {
  const navigate = useNavigate();
  const open = useTabStore((s) => s.open);
  const menus = useNavStore((s) => s.menus);
  const setActiveTop = useNavStore((s) => s.setActiveTop);
  const visit = useFavoriteStore((s) => s.visit);

  return useCallback(
    (screenKey: ScreenKey) => {
      const def = getScreen(screenKey);
      const menu = findMenuByScreen(menus, screenKey);
      const title = menu?.menuNm ?? def?.title ?? screenKey;
      const path = screenPath(screenKey);
      const ok = open({ screenKey, title, path, closable: screenKey !== HOME_SCREEN_KEY });
      if (!ok) {
        notifyError(`열 수 있는 화면 탭은 최대 ${MAX_OPEN_TABS}개입니다. 사용하지 않는 탭을 닫고 다시 시도하세요.`);
        return;
      }
      visit(screenKey);
      if (screenKey === HOME_SCREEN_KEY) setActiveTop(HOME_TOP);
      else if (menu?.topId) setActiveTop(menu.topId);
      navigate(path);
    },
    [menus, open, visit, setActiveTop, navigate],
  );
};
