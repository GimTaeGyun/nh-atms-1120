import type { ThemeConfig } from 'antd';
import { theme } from 'antd';
import { colors, font, radius, sc } from './tokens';

/**
 * antd 6 테마. 기본 알고리즘 + 시안 B 토큰 + 촘촘한 높이·여백.
 * 화면 비율: 글자·높이·여백은 sc()(tokens.ts UI_SCALE) — 2026-11-13 의 × 0.8 은 2026-11-18 되돌림(UI_SCALE = 1).
 * 여백·간격은 sizeUnit·sizeStep(sc(4))으로 antd 가 한꺼번에 맞춘다.
 * 화면 코드에서 색상 하드코딩을 금지하고 이 토큰(또는 CSS 변수)만 쓴다.
 */
export const antdTheme: ThemeConfig = {
  // 2026-11-13 가독성 개선: compactAlgorithm 은 모든 입력·버튼 글자를 기본보다 2px 작게(14 → 12px) 만들어서 빼고,
  // 촘촘함은 아래 controlHeight·표 칸 여백으로 직접 맞춘다(입력 32px, 작은 버튼 26px).
  // 글자 크기는 아래 fontSize* 를 직접 준다(antd 가 기본 글자에서 짝수로 반올림해 만드는 값 대신).
  algorithm: theme.defaultAlgorithm,
  cssVar: { prefix: 'nh' },
  hashed: false,
  token: {
    colorPrimary: colors.primary,
    colorInfo: colors.primary,
    colorSuccess: colors.success,
    colorError: colors.danger,
    colorWarning: colors.warning,
    colorLink: colors.primary,
    colorText: colors.textPrimary,
    colorTextSecondary: colors.textSecondary,
    colorBorder: colors.border,
    colorBorderSecondary: colors.borderLight,
    colorBgLayout: colors.bgPage,
    colorBgContainer: colors.bgPanel,
    borderRadius: radius.md,
    borderRadiusSM: radius.sm,
    borderRadiusLG: radius.lg,
    fontFamily: font.family,
    fontSize: font.size,
    fontSizeSM: font.sizeSm,
    fontSizeLG: font.sizeLg,
    fontSizeXL: sc(20),
    fontSizeHeading1: sc(38),
    fontSizeHeading2: sc(30),
    fontSizeHeading3: sc(24),
    fontSizeHeading4: sc(20),
    fontSizeHeading5: sc(16),
    fontSizeIcon: sc(12),
    sizeUnit: sc(4),
    sizeStep: sc(4),
    controlPaddingHorizontal: sc(12),
    controlPaddingHorizontalSM: sc(8),
    colorTextPlaceholder: colors.textMuted,
    colorTextDescription: colors.textSecondary,
    // 흐림(권한 없음·선택 전) 버튼·칸도 글자는 읽히게 — antd 기본 rgba(0,0,0,.25) 보다 진하게
    colorTextDisabled: '#8b95a3',
    colorBgContainerDisabled: '#f2f4f7',
    controlHeight: sc(32),
    controlHeightSM: sc(26),
    controlHeightLG: sc(38),
    controlHeightXS: sc(16),
  },
  components: {
    Table: {
      headerBg: colors.bgTableHead,
      headerColor: colors.textPrimary,
      headerSplitColor: colors.border,
      borderColor: colors.borderLight,
      rowHoverBg: '#eef5ff',
      rowSelectedBg: '#e3efff',
      rowSelectedHoverBg: '#d6e8ff',
      cellPaddingBlockSM: sc(5),
      cellPaddingInlineSM: sc(8),
      fontSize: font.size,
      headerBorderRadius: 0,
    },
    Tabs: { titleFontSize: font.size, itemSelectedColor: colors.primary, inkBarColor: colors.primary, horizontalItemPadding: `${sc(8)}px ${sc(14)}px` },
    Button: { primaryShadow: 'none', defaultShadow: 'none', fontWeight: 500 },
    Card: { headerBg: 'transparent', paddingLG: sc(16) },
    Modal: { titleFontSize: sc(16) },
    Form: { itemMarginBottom: 0, labelColor: colors.textPrimary },
    // 현재 페이지 번호: 남색 바탕에 흰 글자(itemActiveColor 를 안 주면 글자도 남색이라 번호가 안 보인다)
    Pagination: { itemActiveBg: colors.primary, itemActiveColor: '#fff', itemActiveColorHover: '#fff' },
    Tag: { borderRadiusSM: sc(10) },
    Menu: { itemHeight: sc(34), subMenuItemBg: 'transparent' },
    Segmented: { itemSelectedBg: colors.primary, itemSelectedColor: '#fff' },
  },
};
