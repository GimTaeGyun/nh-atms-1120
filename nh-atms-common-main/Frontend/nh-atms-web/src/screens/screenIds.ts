/**
 * 화면키 — 소스(라우트·화면 간 이동·팝업 제목·권한)는 영문 화면키를 쓴다.
 * 키 규칙: `{대분류 slug}.{camelCase 이름}`, 팝업은 `popup.{이름}`, 홈은 `home`. 라우트는 `/{slug}/{kebab 이름}`.
 *
 * 화면키 목록은 두지 않는다 — 화면은 `screens/{slug}/{PascalName}Page.tsx` 파일이 있으면 자동 등록되고(registry.ts),
 * 메뉴는 DB(TB_SCR_MENU)만 보면 된다. 그래서 새 화면·새 메뉴마다 이 파일을 고치지 않는다.
 *
 * 화면번호: 이 저장소는 번호를 쓰지 않는다(설계 Q9) → `screenIdOf()` 는 항상 ''.
 */
export type ScreenKey = string;

export const HOME_SCREEN_KEY: ScreenKey = 'home';

/** 화면키 → 표시용 화면번호. 이 저장소는 번호가 없어 항상 '' (호출하는 쪽은 빈 값이면 번호를 그리지 않는다) */
export const screenIdOf = (_key: ScreenKey): string => '';

/** 화면키 모양인지(목록 대조가 아니라 모양 검사): 'home' 또는 '{영문 소문자}.{영문·숫자}' */
export const isScreenKey = (v: unknown): v is ScreenKey =>
  typeof v === 'string' && (v === HOME_SCREEN_KEY || /^[a-z]+\.[a-z][A-Za-z0-9]*$/.test(v));
