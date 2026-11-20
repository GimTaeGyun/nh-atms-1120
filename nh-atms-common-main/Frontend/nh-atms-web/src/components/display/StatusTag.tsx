import type { ReactNode } from 'react';
import { Tag } from 'antd';
import { CheckCircleFilled, CloseCircleFilled, ExclamationCircleFilled, MinusCircleFilled, StopFilled } from '@ant-design/icons';

export type Tone = 'success' | 'danger' | 'warning' | 'info' | 'neutral';

const ICON: Record<Tone, ReactNode> = { success: <CheckCircleFilled />, danger: <CloseCircleFilled />, warning: <ExclamationCircleFilled />, info: <ExclamationCircleFilled />, neutral: <MinusCircleFilled /> };

/** 상태 알약 태그 (정상/오류/경고/정보/중립) */
export const StatusTag = ({ tone, children, icon = true, text }: { tone: Tone; children: ReactNode; icon?: boolean; text?: boolean }) => (
  <span className={`status-tag ${tone}${text ? ' text' : ''}`}>
    {icon && ICON[tone]}
    {children}
  </span>
);

/** 거래상태/결과 등 텍스트 기반 톤 매핑 */
export const toneOf = (v?: string): Tone => {
  if (!v) return 'neutral';
  if (/정상|완료|승인|성공|Y$|^O$/.test(v)) return 'success';
  if (/불능|실패|오류|장애|단절|취소불능|거부/.test(v)) return 'danger';
  if (/처리중|확인요망|경고|대기|진행/.test(v)) return 'warning';
  if (/취소/.test(v)) return 'neutral';
  return 'info';
};

export const ResultTag = ({ value }: { value?: string }) => (value ? <StatusTag tone={toneOf(value)} icon={false}>{value}</StatusTag> : <span className="muted">-</span>);

/**
 * 사용여부 태그 — '사용' 초록 테두리 / '미사용' 주황 테두리 (2026-11-17 사용자 결정, 표·상세 팝업 공통).
 * 값 'Y'(또는 true) = 사용, 그 밖 = 미사용. 표 칸은 columns.tsx `usedCol`.
 */
export const UseYnTag = ({ value }: { value?: boolean | 'Y' | 'N' | string | null }) =>
  value === true || value === 'Y' ? <Tag color="green" style={{ marginInlineEnd: 0 }}>사용</Tag> : <Tag color="orange" style={{ marginInlineEnd: 0 }}>미사용</Tag>;

export const YnIcon = ({ value }: { value?: boolean | 'Y' | 'N' }) => {
  const on = value === true || value === 'Y';
  return on ? <CheckCircleFilled style={{ color: 'var(--success)' }} /> : <StopFilled style={{ color: '#c9d3df' }} />;
};
