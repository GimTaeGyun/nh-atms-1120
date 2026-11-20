import type { ReactNode } from 'react';
import { Button } from 'antd';
import { DeleteOutlined, DownOutlined, EditOutlined, UpOutlined } from '@ant-design/icons';
import { DetailModal } from '@/components/display/DetailModal';
import { DetailGrid, type DetailItem } from '@/components/display/DetailGrid';
import { AuthButton } from '@/components/auth/AuthButton';
import { fmtCount, fmtDate, fmtDateTime14, fmtPerson } from '@/utils/format';
import { useConfirm } from '@/components/feedback/useConfirm';
import { CMSG } from '@/config/messages';
import type { Notice } from '@/domain/notice';
import { ScopeTag } from '../sample/NoticeParts';

interface Props {
  open: boolean;
  /** 표시할 공지(상세 API 응답, text·앞뒤 글 포함). 읽는 중이면 undefined */
  notice?: Notice;
  loading?: boolean;
  /** 이 글을 고칠 수 있나(관리자 = 모두, 그 밖 = 본인 글 + UPD) — NoticeParts canEditNotice */
  canEdit: boolean;
  /** 이 글을 지울 수 있나(관리자 = 모두, 그 밖 = 본인 글 + DEL) */
  canDelete: boolean;
  deleting?: boolean;
  onClose: () => void;
  /** 이전·다음 글로 — 부모가 그 번호로 상세를 다시 읽는다(조회수 +1) */
  onMove: (no: number) => void;
  /** [수정] — 부모가 이 팝업을 닫고 입력 팝업을 연다 */
  onEdit: (n: Notice) => void;
  /** [삭제] — 확인창 뒤 부모가 지우고 닫는다 */
  onDelete: (n: Notice) => void;
}

/** 상세 항목 — 제목·범위·번호·작성자·작성일·조회수·최종변경, 내용(줄바꿈 그대로) */
const itemsOf = (n: Notice): DetailItem[] => [
  { label: '제목', value: n.subject, span: 2 },
  { label: '공지 범위', value: <span><ScopeTag n={n} /> {n.ofcCode ? '(이 사무소 + 관리자에게만 보임)' : '(모든 사무소에 보임)'}</span>, span: 2 },
  { label: '번호', value: n.no, num: true },
  { label: '작성자', value: fmtPerson(n.name, n.perNo) },
  { label: '작성일', value: fmtDate(n.wrtDate) },
  { label: '조회수', value: fmtCount(n.visit), num: true },
  { label: '최종변경', value: n.lschgDtm ? `${fmtDateTime14(n.lschgDtm)}${n.lsCmeno ? ` · ${n.lsCmeno}` : ''}` : undefined, span: 2 },
  { label: '내용', value: n.text, span: 2, pre: true },
];

/** 이전·다음 글 한 줄 — 없으면 흐린 안내 */
const NavLine = ({ label, icon, no, subject, onMove }: { label: string; icon: ReactNode; no: number | null; subject: string | null; onMove: (no: number) => void }) => (
  <Button
    type="text"
    block
    icon={icon}
    disabled={no === null || no === undefined}
    onClick={() => no !== null && onMove(no)}
    style={{ justifyContent: 'flex-start', height: 'auto', padding: '6px 8px', borderTop: '1px solid var(--border-light)', borderRadius: 0 }}
  >
    <span style={{ minWidth: 56, fontWeight: 600, textAlign: 'left' }}>{label}</span>
    <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', color: no === null || no === undefined ? 'var(--text-muted)' : undefined }}>
      {no === null || no === undefined ? `${label}이 없습니다.` : subject || '(제목 없음)'}
    </span>
  </Button>
);

/**
 * 공지 상세 팝업(`popup.noticeView`). 여는 쪽(NoticePage)이 `GET /api/web/notice/{no}?hit=Y` 로 읽어 넘긴다(조회수 +1).
 * 아래에 이전 글(번호가 바로 작은 것)·다음 글(바로 큰 것) — 내가 볼 수 있는 공지 안에서(서버가 계산).
 * [수정]·[삭제]는 화면 권한 + 본인 글(관리자는 모두)일 때만 보인다(2026-11-13 사용자 결정, 서버도 403 NTC_002 로 다시 막음).
 */
export const NoticeViewModal = ({ open, notice, loading, canEdit, canDelete, deleting, onClose, onMove, onEdit, onDelete }: Props) => {
  const ask = useConfirm();
  return (
  <DetailModal
    open={open}
    onClose={onClose}
    title="공지사항 상세"
    screenKey="popup.noticeView"
    width={760}
    loading={loading || !notice}
    actions={
      <>
        {notice && canDelete && (
          <AuthButton
            screenKey="sample.notice"
            need="del"
            danger
            icon={<DeleteOutlined />}
            loading={deleting}
            onClick={() => ask({ content: CMSG.deleteTarget('이 공지'), danger: true, onOk: () => onDelete(notice) })}
          >
            삭제
          </AuthButton>
        )}
        {notice && canEdit && (
          <AuthButton screenKey="sample.notice" need="upd" type="primary" icon={<EditOutlined />} onClick={() => onEdit(notice)}>
            수정
          </AuthButton>
        )}
      </>
    }
  >
    {notice && (
      <>
        <DetailGrid cols={2} labelWidth={100} items={itemsOf(notice)} />
        <nav aria-label="이전·다음 글" style={{ marginTop: 12, borderBottom: '1px solid var(--border-light)' }}>
          <NavLine label="다음 글" icon={<UpOutlined />} no={notice.nextNo} subject={notice.nextSubject} onMove={onMove} />
          <NavLine label="이전 글" icon={<DownOutlined />} no={notice.prevNo} subject={notice.prevSubject} onMove={onMove} />
        </nav>
      </>
    )}
  </DetailModal>
);
};
