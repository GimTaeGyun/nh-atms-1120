import { lazy } from 'react';
import type { ScreenDef } from '@/types/screen';
import { HOME_SCREEN_KEY, isScreenKey, type ScreenKey } from './screenIds';

/**
 * 화면 레지스트리 — 화면 파일을 **자동으로** 찾는다. 등록 줄이 없다(새 화면 = 파일 하나 + DB 메뉴 행).
 * **메뉴 구조(대·중·소메뉴, 이름, 순서)는 여기 두지 않는다** — DB(TB_SCR_MENU)·메뉴관리 화면이 정본(2026-11-12 사용자 결정).
 * 파일명 규칙: `screens/<slug>/{PascalName}Page.tsx` + `export default` → 화면키 `<slug>.{pascalName}`, 라우트 /{slug}/{kebab-case 이름}.
 *   예: screens/sample/NoticePage.tsx → 'sample.notice' → /sample/notice. 홈은 screens/home/HomePage.tsx → 'home' → /home.
 * 메뉴 테이블(TB_SCR_MENU.MENU_PATH)에는 이 라우트 경로를 넣는다 → `parseScreenPath` 로 화면키를 찾는다(잠정 Q-05).
 * 메뉴에는 있는데 파일이 없으면 빈 화면(PlaceholderScreen)이 열린다.
 */
/** camelCase → kebab: 정규식 `([a-z0-9])([A-Z])` → `$1-$2` 후 전체 소문자 (백엔드 MenuAuthService.menuPathOf 와 같은 규칙) */
const kebab = (s: string): string => s.replace(/([a-z0-9])([A-Z])/g, '$1-$2').toLowerCase();

/**
 * 화면키 문자열 → 메뉴 경로(TB_SCR_MENU.MENU_PATH). 'sample.mcnInfo' → '/sample/mcn-info', 'a.mcnABC' → '/a/mcn-abc'.
 * 점이 없으면 '/' + kebab(키) ('home' → '/home'). 규칙은 백엔드 `MenuAuthService.menuPathOf` 와 한 글자도 다르면 안 된다
 * — 백엔드 테스트 MenuAuthServiceTest 에 같은 예시 표가 있다(잠정 Q-05).
 */
export const menuPathOf = (screenKey: string): string => {
  const k = screenKey.trim();
  const dot = k.indexOf('.');
  return (dot < 0 ? `/${kebab(k)}` : `/${k.slice(0, dot)}/${kebab(k.slice(dot + 1))}`).toLowerCase();
};

/** 화면키 → 라우트 경로. 홈은 /home. 예: 'sample.mcnInfo' → '/sample/mcn-info' */
export const screenPath = (key: ScreenKey): string => (key === HOME_SCREEN_KEY ? '/home' : menuPathOf(key));

type ScreenModule = { default: ScreenDef['component'] extends React.LazyExoticComponent<infer C> ? C : never };

/** 화면 파일 전부(지연 로딩). 키 = './sample/NoticePage.tsx'. 로그인은 탭 화면이 아니라 뺀다(router.tsx 가 직접 씀) */
const PAGE_FILES = import.meta.glob<ScreenModule>(['./*/*Page.tsx', '!./login/**']);

/** './sample/NoticePage.tsx' → 'sample.notice', './home/HomePage.tsx' → 'home'. 규칙 밖 파일은 null */
const keyOfFile = (file: string): ScreenKey | null => {
  const m = /^\.\/([a-z]+)\/([A-Z][A-Za-z0-9]*)Page\.tsx$/.exec(file);
  if (!m) return null;
  if (m[1] === HOME_SCREEN_KEY && m[2] === 'Home') return HOME_SCREEN_KEY;
  return `${m[1]}.${m[2].charAt(0).toLowerCase()}${m[2].slice(1)}`;
};

/** 화면 한 줄 — 제목은 메뉴에 없을 때만 쓰인다(메뉴에 있으면 DB 메뉴명). 홈 말고는 화면키를 그대로 */
const def = (key: ScreenKey, loader: () => Promise<ScreenModule>): ScreenDef => ({
  key,
  screenId: '',
  path: screenPath(key),
  title: key === HOME_SCREEN_KEY ? '홈' : key,
  openMode: key === HOME_SCREEN_KEY ? 'HOME' : 'TAB',
  component: lazy(loader),
  status: 'done',
});

export const SCREENS: Record<string, ScreenDef> = Object.fromEntries(
  Object.entries(PAGE_FILES).flatMap(([file, loader]) => {
    const key = keyOfFile(file);
    return key ? [[key, def(key, loader)] as const] : [];
  }),
);

const BY_PATH: Record<string, ScreenDef> = Object.fromEntries(Object.values(SCREENS).map((d) => [d.path, d]));

export const getScreen = (key?: string | null): ScreenDef | undefined => (key ? SCREENS[key] : undefined);

/**
 * 경로 → 화면키. /home 과 /{slug}/{kebab} 만 인정(slug 는 화면키 앞부분 — 메뉴 구조와 무관).
 * 메뉴 응답의 `menuPath` 도 이것으로 화면키를 찾는다. 화면 파일이 없는(아직 미구현) 경로도 화면키를 돌려준다 → 빈 화면으로 열린다.
 */
export const parseScreenPath = (pathname: string | null | undefined): { screenKey: ScreenKey } | null => {
  if (!pathname) return null;
  const p = pathname.trim().replace(/\/+$/, '') || '/';
  if (p === '/home') return { screenKey: HOME_SCREEN_KEY };
  const m = /^\/([a-z]+)\/([a-z0-9-]+)$/.exec(p);
  if (!m) return null;
  const hit = BY_PATH[p];
  if (hit) return { screenKey: hit.key };
  const camel = m[2].replace(/-([a-z0-9])/g, (_, c: string) => c.toUpperCase());
  const key = `${m[1]}.${camel}`;
  return isScreenKey(key) ? { screenKey: key } : null;
};
