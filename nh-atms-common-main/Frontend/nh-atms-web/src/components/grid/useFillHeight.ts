import { useEffect, useState, type RefObject } from 'react';

/** 남은 높이 채우기를 할 때 표 몸통의 최소 높이(약 5행) */
const MIN_BODY = 160;

/**
 * 표가 화면의 남은 높이를 채우게 하는 훅(2026-11-13 사용자 결정 — 콘텐츠 영역은 스크롤 없이, 표 높이가 조회조건 양에 따라 가변).
 *
 * 동작
 *  - 표를 감싼 div(ref)의 **부모가 세로 flex**(화면 틀 `.page-frame`, 탭 칸, 마스터·디테일 칸 등 — CSS 가 그렇게 만듦)이고 팝업 안이 아니면 '채우기' 를 켠다.
 *    켜지면 div 에 `grid-fill` 클래스(남은 높이를 차지)를 붙이고, div 높이에서 표 머리글·페이지 줄·도구줄을 뺀 값을 표 몸통 높이(scroll.y)로 돌려준다.
 *  - 창 크기·조회조건 줄 수·행 수가 바뀌면 다시 잰다(ResizeObserver). 스크롤은 표 안에서만 생긴다.
 *  - 부모가 세로 flex 가 아니면(팝업·공통 모듈 시연 카드 등) 아무것도 하지 않는다 → 예전처럼 내용 높이.
 *  - 높이를 숫자로 준 표(`height={…}`)는 이 훅을 쓰지 않는다(enabled = false).
 *
 * @returns fill 여부와 표 몸통 높이(px). fill 이 아니면 y = undefined
 */
export const useFillHeight = (ref: RefObject<HTMLElement | null>, enabled: boolean): { fill: boolean; y: number | undefined } => {
  const [state, setState] = useState<{ fill: boolean; y: number | undefined }>({ fill: false, y: undefined });

  useEffect(() => {
    const el = ref.current;
    if (!enabled || !el || typeof ResizeObserver === 'undefined') return;
    const parent = el.parentElement;

    const measure = () => {
      if (!parent || el.closest('.ant-modal')) return setState((s) => (s.fill ? { fill: false, y: undefined } : s));
      const cs = getComputedStyle(parent);
      const column = cs.display.includes('flex') && cs.flexDirection === 'column';
      if (!column) return setState((s) => (s.fill ? { fill: false, y: undefined } : s));
      if (el.clientHeight === 0) return; // 숨은 탭(keep-alive) — 보일 때 다시 잰다
      // div 안에서 표 몸통이 아닌 부분(머리글·페이지 줄·도구줄)의 높이
      const body = el.querySelector<HTMLElement>('.ant-table-body') ?? el.querySelector<HTMLElement>('.ant-table-tbody');
      // 자식(표·'총 n건' 줄·페이지 줄·도구줄)의 높이 + 위아래 여백. 새로 생긴 자식(조회 뒤 나타나는 아래 줄)도 지켜본다
      const kids = Array.from(el.children) as HTMLElement[];
      kids.forEach((k) => ro.observe(k));
      const content = kids.reduce((sum, k) => {
        const ks = getComputedStyle(k);
        return sum + k.offsetHeight + (parseFloat(ks.marginTop) || 0) + (parseFloat(ks.marginBottom) || 0);
      }, 0);
      const other = content - (body?.offsetHeight ?? 0);
      const y = Math.max(MIN_BODY, Math.floor(el.clientHeight - other));
      setState((s) => (s.fill && s.y !== undefined && Math.abs(s.y - y) <= 1 ? s : { fill: true, y }));
    };

    const ro = new ResizeObserver(() => measure());
    measure();
    ro.observe(el);
    if (parent) ro.observe(parent);
    return () => ro.disconnect();
  }, [ref, enabled]);

  return enabled ? state : { fill: false, y: undefined };
};
