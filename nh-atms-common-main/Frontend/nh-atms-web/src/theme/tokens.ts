/**
 * 디자인 토큰 — GUI 시안 v0.6 Concept B(라이트) + A 아이콘 레일(NH 블루) 색상 실측값.
 * CSS 변수(global.css)와 antd 테마(antdTheme.ts)의 단일 원천.
 * 2026-11-13 가독성 개선(사용자 결정): 보조·흐린 글자색을 흰 바탕 대비 4.5:1 이상으로 진하게, 테두리도 한 단계 진하게.
 * 화면 비율: 2026-11-13 에 크기를 모두 × 0.8 로 줄였다가(크롬 80% 비율을 100% 기본으로), 2026-11-18 사용자 결정으로 되돌림 —
 *  폐쇄망 크롬 100% 에서 너무 작았음(휴대폰 데모와 같은 크기가 기준). 글자 14px · 보조 13px · 배지·꼬리글 12px. CSS 의 px 도 같이 되돌렸다.
 */
export const colors = {
  /** NH 브랜드 블루 — 아이콘 레일 배경 (시안 A 실측 #0054a6) */
  nhBlue: '#0054a6',
  nhBlueHover: '#005eb8',
  nhBlueActive: '#003b75',
  /** NH 골드 (로고 마크) */
  nhGold: '#ffb81c',
  /** 주요 액션/선택 — 네이비 (시안 B 조회 버튼·선택 메뉴 실측 #054681) */
  primary: '#054681',
  primaryHover: '#0b5aa3',
  primaryActive: '#043a6b',
  /** 강조 숫자(KPI) 블루 */
  accent: '#1d5fcf',
  /** 상태색 */
  success: '#1f9d55',
  successBg: '#e8f7ee',
  danger: '#e0392c',
  dangerBg: '#fdecec',
  warning: '#f59e0b',
  warningBg: '#fff4e0',
  info: '#2f80ed',
  infoBg: '#eaf2ff',
  neutral: '#8c8c8c',
  neutralBg: '#f0f0f0',
  /** 배경/보더/텍스트 */
  bgPage: '#f4f8fd',
  bgPanel: '#ffffff',
  bgSubtle: '#f7fafd',
  bgSearch: '#f3f8fe',
  bgTableHead: '#eef2f7',
  bgLabelCell: '#eef4fb',
  border: '#d3d9e1',
  borderLight: '#e2e7ee',
  textPrimary: '#1a2433',
  /** 보조 글자 — 흰 바탕 대비 약 7.6:1 */
  textSecondary: '#454f5e',
  /** 흐린 글자(안내·빈 값) — 흰 바탕 대비 약 4.9:1, 이보다 연한 글자색은 쓰지 않는다 */
  textMuted: '#667180',
  textOnDark: '#ffffff',
} as const;

/** 화면 크기 비율 — 예전(100%) 값에 곱한다. 2026-11-18 사용자 결정으로 1(되돌림). CSS 의 px 도 1 기준(바꾸려면 CSS 도 같이) */
export const UI_SCALE = 1;
/** 예전 크기 → 지금 크기(소수 1자리) */
export const sc = (n: number): number => Math.round(n * UI_SCALE * 10) / 10;

export const radius = { sm: sc(4), md: sc(6), lg: sc(10), xl: sc(14) } as const;

export const layout = {
  headerHeight: sc(56),
  tabBarHeight: sc(38),
  railWidth: sc(64),
  panelWidth: sc(220),
  contentPadding: sc(20),
} as const;

export const font = {
  family: "'Pretendard', -apple-system, BlinkMacSystemFont, 'Malgun Gothic', sans-serif",
  /** 기본 글자 14px(본문·표·입력·좌측 메뉴) */
  size: sc(14),
  /** 보조 글자 최소 13px */
  sizeSm: sc(13),
  sizeLg: sc(15),
} as const;
