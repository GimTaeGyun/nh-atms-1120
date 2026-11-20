import type { ReactNode } from 'react';
import { Button, Modal, Skeleton } from 'antd';
import { SaveOutlined } from '@ant-design/icons';
import { ModalTitle } from './ModalTitle';
import { AuthButton } from '@/components/auth/AuthButton';
import { EmptyState } from '@/components/feedback/EmptyState';
import type { PermNeed } from '@/components/auth/useAllowed';
import type { ScreenKey } from '@/screens/screenIds';

/**
 * 등록·수정(입력) 팝업 공통 틀 (2026-11-16 사용자 결정 — 팝업마다 같은 틀을 복사하지 않는다).
 * 조회용 상세 팝업은 `DetailModal`, 입력 팝업은 이 파일 3개를 쓴다.
 *
 * ```tsx
 * export const XxxEditModal = ({ open, target, onClose }) => (
 *   <FormModal open={open} onClose={onClose} title={target ? 'Xxx 수정' : 'Xxx 등록'} screenKey="popup.xxxEdit">
 *     <XxxEditBody target={target} onClose={onClose} />     // 열 때마다 새로 그림(destroyOnHidden)
 *   </FormModal>
 * );
 * const XxxEditBody = ({ target, onClose }) => {
 *   const detail = useQuery({ …, enabled: target !== null, gcTime: 0 });
 *   return <FormLoadGate editing={target !== null} isError={detail.isError} data={detail.data} what="Xxx" onClose={onClose}>
 *     {(row) => <XxxForm row={row} onClose={onClose} />}
 *   </FormLoadGate>;
 * };
 * // 폼 아래: <FormModalFooter onClose={onClose} screenKey="sample.xxx" need={row ? 'upd' : 'reg'} saving={save.isPending} onSave={() => form.submit()} />
 * ```
 */
export const FormModal = ({
  open,
  onClose,
  title,
  screenKey,
  width = 760,
  children,
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  /** 팝업 화면키(popup.*) */
  screenKey?: ScreenKey;
  width?: number;
  children: ReactNode;
}) => (
  <Modal open={open} onCancel={onClose} width={width} title={<ModalTitle title={title} screenKey={screenKey} />} footer={null} destroyOnHidden>
    {children}
  </Modal>
);

/**
 * 수정 팝업의 "원본 읽기" 단계 — 읽는 중 = 스켈레톤, 실패 = 안내 + [닫기](문구는 서버가 이미 띄움, 끝없는 로딩 방지), 끝나면 폼.
 * 등록(editing=false)이면 바로 폼(row = null).
 */
export function FormLoadGate<T>({
  editing,
  isError,
  data,
  what,
  onClose,
  children,
}: {
  editing: boolean;
  isError: boolean;
  data: T | undefined;
  /** 실패 안내 '<what>을(를) 불러오지 못했습니다.' 의 대상 */
  what: string;
  onClose: () => void;
  children: (row: T | null) => ReactNode;
}) {
  if (editing && isError) return <EmptyState text={`${what} 정보를 불러오지 못했습니다.`} action={<Button onClick={onClose}>닫기</Button>} />;
  if (editing && data === undefined) return <Skeleton active paragraph={{ rows: 6 }} />;
  return <>{children(editing ? (data as T) : null)}</>;
}

/** 입력 팝업 아래 버튼 줄 — [닫기] [저장](권한 = 등록 reg / 수정 upd, 없으면 흐림). extra 는 닫기 왼쪽(예: 삭제) */
export const FormModalFooter = ({
  onClose,
  screenKey,
  need,
  saving,
  disabled,
  onSave,
  saveText = '저장',
  extra,
}: {
  onClose: () => void;
  screenKey: ScreenKey;
  need: PermNeed;
  saving?: boolean;
  disabled?: boolean;
  onSave: () => void;
  saveText?: string;
  extra?: ReactNode;
}) => (
  <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 16 }}>
    {extra}
    <Button onClick={onClose}>닫기</Button>
    <AuthButton screenKey={screenKey} need={need} type="primary" icon={<SaveOutlined />} loading={saving} disabled={disabled} onClick={onSave}>
      {saveText}
    </AuthButton>
  </div>
);
