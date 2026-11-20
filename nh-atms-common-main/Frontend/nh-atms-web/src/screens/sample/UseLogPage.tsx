import { useCallback, useEffect, useMemo, useState } from 'react';
import { Form } from 'antd';
import { useMutation } from '@tanstack/react-query';
import { useLogApi, useLogKeys } from '@/api/endpoints/useLog';
import { usePagedSearch } from '@/hooks/usePagedSearch';
import { usePermission } from '@/hooks/usePermission';
import { selectOfcCode, useAuthStore } from '@/stores/useAuthStore';
import { PageFrame } from '@/components/page/PageFrame';
import { ResultHeader } from '@/components/page/ResultHeader';
import { ExcelButton } from '@/components/page/ExcelButton';
import { SearchForm } from '@/components/search/SearchForm';
import { ActionBar, type ActionItem } from '@/components/auth/ActionBar';
import { DataGrid } from '@/components/grid/DataGrid';
import { excelColumnsOf } from '@/components/grid/columns';
import { downloadExcel } from '@/utils/excel';
import type { UseLog, UseLogSearch } from '@/domain/useLog';
import type { ScreenProps } from '@/types/screen';
import { UseLogViewModal } from '../popup/UseLogViewModal';
import { toUseLogParams, logColumns, useUseLogFields } from './UseLogParts';

const TITLE = '사용기록조회';

/**
 * 사용기록조회 — 화면에서 한 조회·등록·수정·삭제·엑셀 기록(TB_SCR_CUD_HIST, API 계약 §12, 2026-11-17 사용자 결정).
 *  조회조건(모두 공통 필드, 기간 필수·최대 31일) → [조회] → 서버 페이징 목록(20건, 최근 순) → 행 더블클릭 = 상세 팝업(파라미터·실행 SQL).
 *  보이는 범위: 관리자 = 모든 사무소(사무소 칸으로 거름), 그 밖 = 내 사무소 기록만(사무소 칸 고정, 서버도 덮어씀).
 *  [엑셀] = 지금 보이는 목록(사유 없음). 기록은 서버가 컨트롤러 메서드 이름으로 자동으로 남긴다 — 이 화면은 읽기만.
 */
const UseLogPage = ({ screenKey }: ScreenProps) => {
  const [form] = Form.useForm();
  const [viewOpen, setViewOpen] = useState(false);
  const perm = usePermission(screenKey);
  const myOfc = useAuthStore(selectOfcCode);
  const fields = useUseLogFields(perm.isAdmin, myOfc);
  // 관리자가 아니면 사무소 칸 = 내 사무소(고정). 처음 값은 화면을 열 때 한 번만 들어가므로, 내 정보가 늦게 읽혀도 맞춘다
  useEffect(() => {
    if (!perm.isAdmin && myOfc) form.setFieldValue('ofcCode', myOfc);
  }, [perm.isAdmin, myOfc, form]);
  const search = usePagedSearch<UseLogSearch, UseLog>({ key: useLogKeys.list, fetch: useLogApi.list });
  const columns = useMemo(() => logColumns(search.page, search.size), [search.page, search.size]);

  // 상세는 고를 때만 읽는다(CLOB SQL 포함) — mutation 으로(다시 그릴 때 저절로 재요청되지 않게)
  const view = useMutation({ mutationFn: useLogApi.get });
  const { mutate: openDetail, isPending: viewPending } = view;
  const openView = useCallback(
    (r: UseLog) => {
      if (viewPending) return;
      setViewOpen(true);
      openDetail(r.histId, { onError: () => setViewOpen(false) });
    },
    [viewPending, openDetail],
  );

  const onSearch = (v: Record<string, unknown>) => search.search(toUseLogParams(v) as UseLogSearch);
  const excel = () => downloadExcel<UseLog>(excelColumnsOf(columns), search.rows, TITLE);

  const actions: ActionItem[] = [
    { key: 'search', onClick: () => form.submit(), loading: search.loading },
    { key: 'excel', onClick: excel, disabled: !search.rows.length },
  ];

  return (
    <PageFrame
      screenKey={screenKey}
      lastUpdated={search.lastUpdated}
      onRefresh={() => void search.refetch()}
      refreshing={search.loading}
      extra={<ActionBar screenKey={screenKey} size="small" hotkeys actions={actions} />}
    >
      <SearchForm form={form} fields={fields} onSearch={onSearch} loading={search.loading} onReset={search.reset} />
      <ResultHeader title="사용기록" total={search.searched ? search.total : undefined}>
        <ExcelButton onClick={excel} disabled={!search.rows.length} />
      </ResultHeader>
      <DataGrid<UseLog>
        columns={columns}
        rows={search.rows}
        rowKey="histId"
        loading={search.loading}
        searched={search.searched}
        grade="G1"
        pagination={search.pagination}
        onRowDoubleClick={openView}
      />
      <UseLogViewModal open={viewOpen} log={view.data} loading={view.isPending} onClose={() => setViewOpen(false)} />
    </PageFrame>
  );
};

export default UseLogPage;
