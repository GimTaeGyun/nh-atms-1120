import { useState } from 'react';
import { Button } from 'antd';
import { CheckOutlined, CopyOutlined } from '@ant-design/icons';
import { DetailModal } from '@/components/display/DetailModal';
import { DetailGrid } from '@/components/display/DetailGrid';
import type { UseLog } from '@/domain/useLog';
import { logItems } from '../sample/UseLogParts';

interface Props {
  open: boolean;
  /** 상세 API 응답(파라미터 원문·실행 SQL 포함). 읽는 중이면 undefined */
  log?: UseLog;
  loading?: boolean;
  onClose: () => void;
}

/**
 * 글 복사 — 은행 개발망은 http 라 navigator.clipboard 가 막힌다 → 숨은 글상자 + execCommand('copy') 로.
 * 이 팝업에서만 쓴다(두 화면 이상이면 utils 로 올린다).
 */
const copyText = (text: string): boolean => {
  const ta = document.createElement('textarea');
  ta.value = text;
  ta.setAttribute('readonly', '');
  ta.style.position = 'fixed';
  ta.style.opacity = '0';
  document.body.appendChild(ta);
  ta.select();
  let ok = false;
  try {
    ok = document.execCommand('copy');
  } catch {
    ok = false;
  }
  document.body.removeChild(ta);
  return ok;
};

/**
 * 사용기록 상세 팝업(`popup.useLogView`) — 목록 칸 전부 + URI + 파라미터 원문 + 실행 SQL(줄바꿈 그대로·스크롤·[복사]).
 * 여는 쪽(UseLogPage)이 `GET /api/web/use-log/{histId}` 로 읽어 넘긴다. 읽기 전용.
 * [복사]를 누르면 버튼 글자가 잠깐 '복사됨'으로 바뀐다(알림 문구 없음).
 */
export const UseLogViewModal = ({ open, log, loading, onClose }: Props) => {
  const [copied, setCopied] = useState(false);
  const copy = () => {
    if (!copyText(log?.sqlText ?? '')) return;
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1500);
  };
  return (
  <DetailModal open={open} onClose={onClose} title="사용기록 상세" screenKey="popup.useLogView" width={860} loading={loading || !log}>
    {log && (
      <>
        <DetailGrid cols={2} labelWidth={100} items={logItems(log)} />
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', margin: '12px 0 4px' }}>
          <b>실행 SQL</b>
          <Button size="small" icon={copied ? <CheckOutlined /> : <CopyOutlined />} disabled={!log.sqlText} onClick={copy}>
            {copied ? '복사됨' : '복사'}
          </Button>
        </div>
        {log.sqlText ? (
          <pre className="mono-block" style={{ maxHeight: 320, overflow: 'auto' }}>{log.sqlText}</pre>
        ) : (
          <div className="muted">실행된 SQL 이 없습니다.</div>
        )}
      </>
    )}
  </DetailModal>
  );
};
