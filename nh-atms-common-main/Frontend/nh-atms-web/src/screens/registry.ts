import { lazy } from 'react';
import type { ScreenDef } from '@/types/screen';
import { HOME_SCREEN_KEY, SCREEN_IDS, isScreenKey, type ScreenKey } from './screenIds';

/**
 * 화면 레지스트리 — 영문 화면키 → 화면 파일(컴포넌트). 등록하면 라우트·탭이 자동 연결된다.
 * **메뉴 구조(대·중·소메뉴, 이름, 순서)는 여기 두지 않는다** — DB(TB_SCR_MENU)·메뉴관리 화면이 정본(2026-11-12 사용자 결정).
 * 파일명 규칙: `screens/<slug>/{PascalName}Page.tsx` (1 화면키 = 1 파일, 번호 없음). 라우트는 /{slug}/{kebab-case 이름}.
 * 메뉴 테이블(TB_SCR_MENU.MENU_PATH)에는 이 라우트 경로를 넣는다 → `parseScreenPath` 로 화면키를 찾는다(잠정 Q-05).
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

/** 화면 한 줄 — 화면키 · 메뉴에 없을 때 쓸 이름 · 화면 파일 */
const def = (key: ScreenKey, title: string, loader: () => Promise<ScreenModule>, extra?: Partial<ScreenDef>): ScreenDef => ({
  key,
  screenId: SCREEN_IDS[key],
  path: screenPath(key),
  title,
  openMode: key === HOME_SCREEN_KEY ? 'HOME' : 'TAB',
  component: lazy(loader),
  status: 'done',
  ...extra,
});

export const SCREENS: Record<string, ScreenDef> = Object.fromEntries(
  [
    def(HOME_SCREEN_KEY, '홈', () => import('./home/HomePage')),
    def('main.dashboard', '대시보드', () => import('./main/DashboardPage')),
    // ---- 견본(sample): 공통 모듈 화면 + CRUD 모양 3가지 + 관리 화면 ----
    def('sample.commonModules', '공통 모듈', () => import('./sample/CommonModulesPage')),
    def('sample.notice', '공지사항', () => import('./sample/NoticePage')),
    def('sample.mcnInfo', '기기정보', () => import('./sample/McnInfoPage')),
    def('sample.mcnModel', '기기모델코드', () => import('./sample/McnModelPage')),
    def('sample.authMgt', '권한관리', () => import('./sample/AuthMgtPage')),
    def('sample.menuMgt', '메뉴관리', () => import('./sample/MenuMgtPage')),
    def('sample.codeMgt', '코드관리', () => import('./sample/CodeMgtPage')),
    def('sample.useLog', '사용기록조회', () => import('./sample/UseLogPage')),
    // ---- 저널관리(journal) ----
    def('journal.journalList', '저널 조회', () => import('./journal/JournalListPage')),
  ].map((d) => [d.key, d]),
);

const BY_PATH: Record<string, ScreenDef> = Object.fromEntries(Object.values(SCREENS).map((d) => [d.path, d]));

export const getScreen = (key?: string | null): ScreenDef | undefined => (key ? SCREENS[key] : undefined);

/**
 * 경로 → 화면키. /home 과 /{slug}/{kebab} 만 인정(slug 는 화면키 앞부분 — 메뉴 구조와 무관).
 * 메뉴 응답의 `menuPath` 도 이것으로 화면키를 찾는다. 레지스트리에 없는(아직 미구현) 화면도 screenIds 에 키가 있으면 연다.
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
