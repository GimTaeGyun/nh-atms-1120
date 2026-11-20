/**
 * 환경 변수 단일 진입점.
 * import.meta.env 를 화면 코드에서 직접 읽지 않고 여기서 타입/기본값을 확정한다.
 * [원본] nh-atms `src/config/env.ts` — 로그인·SSO·데모·무조작 로그아웃 키는 뺐다(설계 §5.3, 로그인 없음).
 */
/** VITE_ENV_LABEL 이 없을 때(.env 파일 없는 AS-IS 프로젝트에 넣은 경우) 빌드 모드를 .env.* 와 같은 표기로 */
const MODE_LABEL: Record<string, string> = { production: 'prod', development: 'local' };

export const env = {
  apiBase: import.meta.env.VITE_API_BASE || '/api/web',
  envLabel: (import.meta.env.VITE_ENV_LABEL || MODE_LABEL[import.meta.env.MODE] || import.meta.env.MODE || 'local').toLowerCase(),
  appVersion: typeof __APP_VERSION__ === 'string' ? __APP_VERSION__ : '1.0.0',
  buildTime: typeof __BUILD_TIME__ === 'string' ? __BUILD_TIME__ : '',
  isDev: import.meta.env.DEV,
  /** 라우터: browser(기본) | hash(`/#/home` — 웹서버에 SPA 새로고침 설정을 넣을 수 없을 때) */
  routerMode: (import.meta.env.VITE_ROUTER === 'hash' ? 'hash' : 'browser') as 'browser' | 'hash',
} as const;

export type EnvLabel = 'local' | 'dev' | 'stg' | 'prod';
