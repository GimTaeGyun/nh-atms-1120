import { useCallback, type ReactNode } from 'react';
import { App } from 'antd';
import type { ModalStaticFunctions } from 'antd/es/modal/confirm';
import { DeleteOutlined, QuestionCircleOutlined } from '@ant-design/icons';

/**
 * 확인 팝업 하나로 통일 (2026-11-16 사용자 결정)
 * - 모양: 화면 가운데 작은 팝업(antd `modal.confirm`, 테마 적용 — 휴대폰에서도 잘 보임). 버튼 옆 말풍선(Popconfirm)은 쓰지 않는다.
 * - 버튼: [취소] [동작 이름] — 동작 이름 = 삭제·저장·승인 …, 삭제(danger)는 빨간 버튼. 처음 포커스는 [취소](Enter 로 바로 지워지지 않게).
 * - 문구: `CMSG`(`config/messages.ts`) 의 "~하시겠습니까?" 만.
 * - 브라우저 `window.confirm` 금지(게시·iframe 환경에서 막혀 무반응이 됨).
 *
 * 쓰는 곳: `ActionBar` 의 `confirm`(삭제 기본), 팝업 안 [삭제](`NoticeViewModal`), 첨부파일 삭제(`FileAttachList`), 코드관리.
 */
export interface ConfirmOptions {
  /** 질문 문구 — `CMSG.deleteSelected(3)` 처럼 */
  content: ReactNode;
  /** 실행 버튼 글자(동작 이름). 기본 '확인', danger 면 '삭제' */
  okText?: string;
  /** 되돌릴 수 없는 동작(삭제) — 빨간 버튼·휴지통 아이콘. 기본 false */
  danger?: boolean;
  /** 팝업 제목. 기본 danger ? '삭제 확인' : '확인' */
  title?: ReactNode;
  /** [동작] 을 누르면. Promise 를 돌려주면 끝날 때까지 버튼이 로딩으로 돈다 */
  onOk: () => unknown;
  onCancel?: () => void;
}

type ModalApi = Pick<ModalStaticFunctions, 'confirm'>;

/** React 바깥에서도 같은 모양으로 열 수 있게 함수로 둔다(`appUi.modal` 과 함께) */
export const openConfirm = (modal: ModalApi, o: ConfirmOptions) =>
  modal.confirm({
    centered: true,
    title: o.title ?? (o.danger ? '삭제 확인' : '확인'),
    content: o.content,
    icon: o.danger ? <DeleteOutlined style={{ color: 'var(--danger)' }} /> : <QuestionCircleOutlined style={{ color: 'var(--primary)' }} />,
    okText: o.okText ?? (o.danger ? '삭제' : '확인'),
    cancelText: '취소',
    okButtonProps: { danger: !!o.danger },
    focusable: { autoFocusButton: 'cancel' },
    mask: { closable: false },
    onOk: async () => {
      await o.onOk();
    },
    onCancel: o.onCancel,
  });

/** 컴포넌트 안에서: `const confirm = useConfirm(); confirm({ content: CMSG.delete, danger: true, onOk: remove })` */
export const useConfirm = () => {
  const { modal } = App.useApp();
  return useCallback((o: ConfirmOptions) => openConfirm(modal, o), [modal]);
};
