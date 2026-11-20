import { useState, type ReactNode } from 'react';
import { SectionCard } from '@/components/display/SectionCard';

export interface MasterDetailGridsProps<M> {
  masterTitle: ReactNode;
  detailTitle: ReactNode | ((master: M | null) => ReactNode);
  /** 상단 마스터 영역. `select(row)` 를 행 클릭에 연결 */
  master: (ctx: { select: (row: M | null) => void; selected: M | null }) => ReactNode;
  /** 하단 디테일 영역. 마스터가 없으면 안내문만 */
  detail: (master: M) => ReactNode;
  masterExtra?: ReactNode | ((selected: M | null) => ReactNode);
  detailExtra?: ReactNode | ((master: M | null) => ReactNode);
  emptyText?: string;
  /** 좌우 배치(기본 상하) */
  layout?: 'vertical' | 'horizontal';
  /** 좌우 배치일 때 두 칸 너비(CSS grid-template-columns). 기본 '1fr 1fr' — 예: 코드관리 'minmax(0, 4fr) minmax(0, 8fr)' */
  columns?: string;
  /** 바깥에서 선택을 쥘 때(삭제·조회 뒤 선택 비우기 등). 주지 않으면 안에서 기억한다 */
  selected?: M | null;
  onSelect?: (row: M | null) => void;
  className?: string;
}

/**
 * 마스터(상단) 선택 → 디테일(하단) 편집 레이아웃(견본: 기기모델코드 — 기기사 → 모델).
 * 각 영역에는 EditableGrid 나 DataGrid 를 넣고, 저장/삭제 버튼은 extra 로 둔다.
 */
export function MasterDetailGrids<M>({
  masterTitle,
  detailTitle,
  master,
  detail,
  masterExtra,
  detailExtra,
  emptyText = '상단 목록에서 항목을 선택하세요.',
  layout = 'vertical',
  columns = '1fr 1fr',
  selected: outer,
  onSelect,
  className,
}: MasterDetailGridsProps<M>) {
  const [inner, setInner] = useState<M | null>(null);
  const selected = outer !== undefined ? outer : inner;
  const setSelected = (m: M | null) => {
    setInner(m);
    onSelect?.(m);
  };
  const fn = <X,>(v: X | ((m: M | null) => X)): X => (typeof v === 'function' ? (v as (m: M | null) => X)(selected) : v);
  return (
    <div className={`master-detail${className ? ` ${className}` : ''}`} style={layout === 'horizontal' ? { display: 'grid', gridTemplateColumns: columns, gap: 12 } : { display: 'flex', flexDirection: 'column', gap: 12 }}>
      <SectionCard title={masterTitle} extra={fn(masterExtra)}>{master({ select: setSelected, selected })}</SectionCard>
      <SectionCard title={fn(detailTitle)} extra={fn(detailExtra)}>
        {selected ? detail(selected) : <div className="grid-empty">{emptyText}</div>}
      </SectionCard>
    </div>
  );
}
