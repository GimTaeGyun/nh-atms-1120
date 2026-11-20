import { useMemo, useState } from 'react';
import { Form } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { codeMgtApi, codeMgtKeys } from '@/api/endpoints/codeMgt';
import { PageFrame } from '@/components/page/PageFrame';
import { SearchForm } from '@/components/search/SearchForm';
import { ActionBar, type ActionItem } from '@/components/auth/ActionBar';
import { DataGrid } from '@/components/grid/DataGrid';
import { MasterDetailGrids } from '@/components/grid/MasterDetailGrids';
import { CMSG } from '@/config/messages';
import { fmtCount } from '@/utils/format';
import type { CodeDetail, CodeMaster, CodeMgtSearch } from '@/domain/codeMgt';
import type { ScreenProps } from '@/types/screen';
import { CodeDetailEditModal, CodeDetailViewModal, CodeMasterEditModal, CodeMasterViewModal, type DetailTarget } from '../popup/CodeMgtModals';
import { detailColumns, masterColumns } from './CodeMgtColumns';
import { ALL, CODE_FIELDS, toCodeParams } from './CodeMgtParts';

const NO_MASTERS: CodeMaster[] = [];
const NO_DETAILS: CodeDetail[] = [];
/** 왼쪽 4 : 오른쪽 8 (프로토타입과 같은 비율) */
const SPLIT = 'minmax(0, 4fr) minmax(0, 8fr)';

/**
 * 코드관리(`sample.codeMgt`, TB_CODE_MASTER · TB_CODE_DTL) — 왼쪽 코드구분 · 오른쪽 상세코드, 등록·수정·상세는 팝업(API 계약 §11).
 * 2026-11-16 사용자 결정(최종 프로토타입):
 *  왼쪽 = 체크 · 코드구분 · 코드명 · 사용여부. 행 클릭 = 오른쪽 상세코드, 두 번 클릭·[상세] = 상세 팝업(업무·처리구분·설명1~5).
 *  오른쪽 = 체크 · 순번 · 코드 · 코드명 · 데이터1~5(칸 이름 고정). 두 번 클릭 = 상세 팝업.
 *  삭제 = 공통 확인 팝업, 상세코드가 있는 코드구분은 서버가 막음(CDM_002). 권한 = 메뉴 권한 플래그, 엑셀 없음.
 *  모든 팝업·확인창·표는 공통 모듈(FormModal·DetailModal·useConfirm·DataGrid·MasterDetailGrids).
 */
const CodeMgtPage = ({ screenKey, active }: ScreenProps) => {
  const [form] = Form.useForm();
  const qc = useQueryClient();
  const [params, setParams] = useState<CodeMgtSearch>(ALL);
  const [sect, setSect] = useState<string | null>(null);
  const [chkM, setChkM] = useState<string[]>([]);
  const [chkD, setChkD] = useState<string[]>([]);
  const [mView, setMView] = useState<string | null>(null);
  const [mEdit, setMEdit] = useState<{ open: boolean; target: string | null }>({ open: false, target: null });
  const [dView, setDView] = useState<DetailTarget | null>(null);
  const [dEdit, setDEdit] = useState<{ open: boolean; code: string | null }>({ open: false, code: null });

  const masters = useQuery({ queryKey: codeMgtKeys.masters(params), queryFn: () => codeMgtApi.masters(params) });
  const mRows = masters.data ?? NO_MASTERS;
  const current = mRows.find((m) => m.codeSect === sect) ?? null;
  const details = useQuery({ queryKey: codeMgtKeys.details(sect ?? ''), queryFn: () => codeMgtApi.details(sect as string), enabled: current !== null });
  const dRows = current ? (details.data ?? NO_DETAILS) : NO_DETAILS;
  const mCols = useMemo(() => masterColumns(), []);
  const dCols = useMemo(() => detailColumns(), []);

  const after = () => qc.invalidateQueries({ queryKey: codeMgtKeys.all });
  const removeMasters = useMutation({ mutationFn: codeMgtApi.removeMasters, onSuccess: after });
  const removeDetails = useMutation({ mutationFn: (keys: string[]) => codeMgtApi.removeDetails(sect as string, keys), onSuccess: after });

  const pick = (m: CodeMaster | null) => {
    setSect(m?.codeSect ?? null);
    setChkD([]);
  };
  const onSearch = (v: Record<string, unknown>) => {
    setChkM([]);
    pick(null);
    const next = toCodeParams(v);
    if (JSON.stringify(next) === JSON.stringify(params)) void masters.refetch();
    else setParams(next);
  };
  /** 수정 대상 = 체크 1건, 체크가 없으면 지금 고른 코드구분 */
  const masterTarget = chkM.length === 1 ? chkM[0] : chkM.length === 0 ? sect : null;

  const masterActions: ActionItem[] = [
    { key: 'open', label: '상세', onClick: () => setMView(sect), disabled: !current },
    { key: 'create', onClick: () => setMEdit({ open: true, target: null }) },
    { key: 'update', onClick: () => setMEdit({ open: true, target: masterTarget }), disabled: !masterTarget },
    {
      key: 'delete',
      disabled: !chkM.length,
      loading: removeMasters.isPending,
      confirm: CMSG.deleteSelected(chkM.length, '코드구분'),
      onClick: () =>
        removeMasters.mutate(chkM, {
          onSuccess: () => {
            if (sect && chkM.includes(sect)) pick(null);
            setChkM([]);
          },
        }),
    },
  ];
  const detailActions: ActionItem[] = [
    { key: 'create', onClick: () => setDEdit({ open: true, code: null }), disabled: !current },
    { key: 'update', onClick: () => setDEdit({ open: true, code: chkD[0] }), disabled: chkD.length !== 1 },
    {
      key: 'delete',
      disabled: !chkD.length,
      loading: removeDetails.isPending,
      confirm: CMSG.deleteSelected(chkD.length, '상세코드'),
      onClick: () => removeDetails.mutate(chkD, { onSuccess: () => setChkD([]) }),
    },
  ];

  return (
    <PageFrame
      screenKey={screenKey}
      onRefresh={() => void masters.refetch()}
      refreshing={masters.isFetching}
      extra={<ActionBar screenKey={screenKey} size="small" hotkeys={active} actions={[{ key: 'search', onClick: () => form.submit(), loading: masters.isFetching }]} />}
    >
      <SearchForm form={form} fields={CODE_FIELDS} onSearch={onSearch} loading={masters.isFetching} />
      <MasterDetailGrids<CodeMaster>
        layout="horizontal"
        columns={SPLIT}
        selected={current}
        onSelect={pick}
        masterTitle={`코드구분 (${fmtCount(mRows.length)}건)`}
        detailTitle={(m) => (m ? `상세코드 — ${m.codeSect} · ${m.codeSectNm} (${fmtCount(dRows.length)}건)` : '상세코드')}
        masterExtra={<ActionBar screenKey={screenKey} size="small" actions={masterActions} style={{ width: 'auto' }} />}
        detailExtra={<ActionBar screenKey={screenKey} size="small" actions={detailActions} style={{ width: 'auto' }} />}
        emptyText="왼쪽에서 코드구분을 고르세요."
        master={({ select, selected }) => (
          <DataGrid<CodeMaster>
            columns={mCols}
            rows={mRows}
            rowKey="codeSect"
            loading={masters.isFetching}
            searched
            grade="G1"
            clientPageSize={0}
            sortable={false}
            bordered
            rowSelection={{ selectedRowKeys: chkM, onChange: (keys) => setChkM(keys.map(String)) }}
            onRowClick={(r) => select(r)}
            onRowDoubleClick={(r) => setMView(r.codeSect)}
            selectedKey={selected?.codeSect ?? null}
          />
        )}
        detail={(m) => (
          <DataGrid<CodeDetail>
            columns={dCols}
            rows={dRows}
            rowKey="code"
            loading={details.isFetching}
            searched
            grade="G1"
            clientPageSize={0}
            sortable={false}
            bordered
            emptyText={`${m.codeSect} 의 상세코드가 없습니다.`}
            rowSelection={{ selectedRowKeys: chkD, onChange: (keys) => setChkD(keys.map(String)) }}
            onRowDoubleClick={(r) => setDView({ codeSect: r.codeSect, code: r.code })}
          />
        )}
      />
      <CodeMasterViewModal
        codeSect={mView}
        onClose={() => setMView(null)}
        onEdit={(s) => {
          setMView(null);
          setMEdit({ open: true, target: s });
        }}
      />
      <CodeMasterEditModal open={mEdit.open} target={mEdit.target} onClose={() => setMEdit({ open: false, target: null })} />
      <CodeDetailViewModal
        target={dView}
        onClose={() => setDView(null)}
        onEdit={(t) => {
          setDView(null);
          setDEdit({ open: true, code: t.code });
        }}
      />
      {sect && <CodeDetailEditModal open={dEdit.open} codeSect={sect} code={dEdit.code} onClose={() => setDEdit({ open: false, code: null })} />}
    </PageFrame>
  );
};

export default CodeMgtPage;
