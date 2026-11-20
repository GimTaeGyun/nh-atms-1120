import type { ComponentType, LazyExoticComponent } from 'react';
import type { ScreenKey } from '@/screens/screenIds';

export type { ScreenKey };

export type OpenMode = 'TAB' | 'POPUP' | 'HOME';

export interface ScreenDef {
  /** 영문 화면키(소스 식별자) */
  key: ScreenKey;
  /** 표시용 화면번호 — 이 저장소는 번호를 쓰지 않아(설계 Q9) 항상 '' */
  screenId: string;
  /** 라우트 경로 /{slug}/{kebab} — 화면키에서 자동(메뉴 테이블 MENU_PATH 와 같은 값) */
  path: string;
  /**
   * 메뉴에 없을 때만 쓰는 이름(홈·팝업·URL 로 직접 연 화면). 메뉴에 있으면 탭 제목·브레드크럼은 DB 메뉴명(MENU_NM)을 쓴다.
   * 대메뉴·중메뉴·순서 같은 메뉴 구조는 프론트에 두지 않는다(2026-11-12 사용자 결정 — 메뉴는 DB·메뉴관리 화면으로만).
   */
  title: string;
  openMode: OpenMode;
  adminOnly?: boolean;
  component: LazyExoticComponent<ComponentType<ScreenProps>>;
  /** 구현 상태 (미구현은 플레이스홀더로 자동 노출) */
  status?: 'done' | 'wip';
}

export interface ScreenProps {
  /** 영문 화면키 — PageFrame·usePermission·useOpenScreen 에 쓴다 */
  screenKey: ScreenKey;
  /** 표시용 화면번호 — 항상 ''(설계 Q9 번호 없음). 코드 분기에 쓰지 않는다 */
  screenId: string;
  /** 탭이 현재 활성인지 (keep-alive 중 백그라운드면 false) */
  active: boolean;
}
