import { useState } from 'react';
import { Tabs } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { jnalApi, jnalKeys } from '@/api/endpoints/jnal';
import { DetailModal } from '@/components/display/DetailModal';
import { DetailGrid } from '@/components/display/DetailGrid';
import { DataGrid } from '@/components/grid/DataGrid';
import { centerCol, type Col } from '@/components/grid/columns';
import type { Jnal, JnalCheck } from '@/domain/jnal';
import { ROLL_SEPARATOR, slipItems } from '../jnal/JnalListParts';

interface Props {
  open: boolean;
  /** 고른 목록 행(서버가 합친 행 그대로) */
  row: Jnal | null;
  /** '[사무소코드] 사무소명' */
  officeLabel: string;
  onClose: () => void;
}

type TabKey = 'slip' | 'checks' | 'jnal';

const CHECK_COLUMNS: Col<JnalCheck>[] = [centerCol<JnalCheck>('일련번호', 'seqno', 90), centerCol<JnalCheck>('수표번호', 'chkNo', 220, { className: 'mono' })];

/** 수표목록 탭 — 열 때만 읽는다(저널 키로 `GET /api/web/jnal/checks`) */
const ChecksPane = ({ row }: { row: Jnal }) => {
  const key = { jnalDt: row.jnalDt, ofcCode: row.ofcCode, mcnNo: row.mcnNo, jnalSeqno: row.jnalSeqno };
  const q = useQuery({ queryKey: jnalKeys.checks(key), queryFn: () => jnalApi.checks(key) });
  return <DataGrid<JnalCheck> columns={CHECK_COLUMNS} rows={q.data ?? []} rowKey={(c) => String(c.seqno)} loading={q.isFetching} grade="G1" clientPageSize={0} sortable={false} />;
};

/**
 * 명세표 팝업(`popup.jnalView`) — As-Is '명세표 및 이미지' 에서 이미지 탭을 뺀 것(2026-11-24 사용자 결정), 모양 = 피그마 명세서 팝업.
 * 탭: 명세서(거래명 제목줄·사무소·거래일·취급점_기번_일련번호·매체·개설은행·계좌/카드번호·거래금액·영수증 글·안내 2줄) | 수표목록 | 저널(인자 글 전부).
 * 매체·개설은행은 코드표가 없어 코드 그대로(잠정 Q-38). 읽기 전용.
 */
export const JnalViewModal = ({ open, row, officeLabel, onClose }: Props) => {
  const [tab, setTab] = useState<TabKey>('slip');
  const close = () => {
    setTab('slip');
    onClose();
  };
  return (
    <DetailModal open={open} onClose={close} title="명세표" screenKey="popup.jnalView" width={760}>
      {row && (
        <Tabs
          activeKey={tab}
          onChange={(k) => setTab(k as TabKey)}
          items={[
            {
              key: 'slip',
              label: '명세서',
              children: (
                <>
                  <div className="section-title">{row.scrNm || '명세서'}</div>
                  <DetailGrid cols={2} labelWidth={150} items={slipItems(row, officeLabel)} />
                  <pre className="mono-block" style={{ maxHeight: 220, overflow: 'auto', marginTop: 8 }}>{row.dtlComts.join(ROLL_SEPARATOR)}</pre>
                  <div className="page-note">※ 거래금액과 잔액을 확인하여 주십시오.</div>
                  <div className="page-note">※ 잔액에는 아직 현금화 되지 않은 수표, 어음 입금액이 포함되어 있으며 [-]표시는 대출잔액 입니다.</div>
                </>
              ),
            },
            { key: 'checks', label: '수표목록', children: tab === 'checks' ? <ChecksPane row={row} /> : null },
            {
              key: 'jnal',
              label: '저널',
              children: <pre className="mono-block" style={{ maxHeight: 420, overflow: 'auto' }}>{row.rolComts.join(ROLL_SEPARATOR)}</pre>,
            },
          ]}
        />
      )}
    </DetailModal>
  );
};
