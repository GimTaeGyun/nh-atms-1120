import { useMemo, useState } from 'react';
import { Form } from 'antd';
import { useMutation, useQuery } from '@tanstack/react-query';
import { menuMgtApi, menuMgtKeys } from '@/api/endpoints/menuMgt';
import { PageFrame } from '@/components/page/PageFrame';
import { ResultHeader } from '@/components/page/ResultHeader';
import { ExcelButton } from '@/components/page/ExcelButton';
import { SearchForm } from '@/components/search/SearchForm';
import { ActionBar, type ActionItem } from '@/components/auth/ActionBar';
import { DataGrid } from '@/components/grid/DataGrid';
import { excelColumnsOf } from '@/components/grid/columns';
import { downloadExcel } from '@/utils/excel';
import { CMSG } from '@/config/messages';
import type { MenuMgtRow, MenuMgtSearch } from '@/domain/menuMgt';
import type { ScreenProps } from '@/types/screen';
import { MenuEditModal } from '../popup/MenuEditModal';
import { menuColumns } from './MenuMgtColumns';
import { ALL, MENU_FIELDS, toMenuParams, useAfterMenuChange } from './MenuMgtParts';

const TITLE = '메뉴관리';
const NO_ROWS: MenuMgtRow[] = [];

/**
 * 메뉴관리(`sample.menuMgt`, TB_SCR_MENU) — 목록 + 팝업형 CRUD(공지사항 견본 모양, API 계약 §9).
 *  메뉴 3단계: 대메뉴(상단) → 중메뉴(좌측) → 소메뉴(실제 화면). 메뉴ID = 영문 3 + 중 2 + 소 2 (SYS0312), 2026-11-12 사용자 결정.
 *  화면을 열면 전체 메뉴를 트리 순서로 바로 보여 준다(페이징 없음) → 조회조건(대메뉴·레벨·사용·검색어)으로 거른다.
 *  [등록] = 팝업에서 레벨 → 대메뉴 → 중메뉴 연쇄 콤보, 메뉴ID 는 서버가 같은 상위 안 번호 MAX + 1 로 정한다.
 *  [수정](1건)·행 더블클릭 = 메뉴명·경로·순서·사용만. [삭제](여러 건) = 물리 삭제 + 권한별 메뉴 행도 함께(하위가 남으면 서버 MNU_001).
 *  쓰기가 끝나면 목록과 왼쪽 메뉴(사이드 패널)를 다시 읽는다.
 */
const MenuMgtPage = ({ screenKey, active }: ScreenProps) => {
  const [form] = Form.useForm();
  const [params, setParams] = useState<MenuMgtSearch>(ALL);
  const [selected, setSelected] = useState<string[]>([]);
  const [edit, setEdit] = useState<{ open: boolean; target: MenuMgtRow | null }>({ open: false, target: null });

  const list = useQuery({ queryKey: menuMgtKeys.list(params), queryFn: () => menuMgtApi.list(params) });
  const rows = list.data ?? NO_ROWS;
  const columns = useMemo(() => menuColumns(), []);
  const after = useAfterMenuChange();
  const remove = useMutation({ mutationFn: menuMgtApi.remove, onSuccess: after });

  const selectedRows = rows.filter((r) => selected.includes(r.menuId));
  const onSearch = (v: Record<string, unknown>) => {
    setSelected([]);
    const next = toMenuParams(v);
    // 조건이 같으면 다시 읽기, 다르면 새 키로 바뀌어 저절로 읽는다
    if (JSON.stringify(next) === JSON.stringify(params)) void list.refetch();
    else setParams(next);
  };
  const excel = () => downloadExcel<MenuMgtRow>(excelColumnsOf(columns), rows, TITLE);
  const onDelete = () => remove.mutate(selected, { onSuccess: () => setSelected([]) });

  const actions: ActionItem[] = [
    { key: 'search', onClick: () => form.submit(), loading: list.isFetching },
    { key: 'excel', onClick: excel, disabled: !rows.length },
    { key: 'create', onClick: () => setEdit({ open: true, target: null }) },
    { key: 'update', onClick: () => setEdit({ open: true, target: selectedRows[0] }), disabled: selectedRows.length !== 1 },
    { key: 'delete', onClick: onDelete, disabled: !selected.length, loading: remove.isPending, confirm: CMSG.deleteSelected(selected.length, '메뉴') },
  ];

  return (
    <PageFrame
      screenKey={screenKey}
      subtitle="대메뉴(상단) → 중메뉴(좌측) → 소메뉴(화면) 3단계 메뉴를 등록·수정합니다. 메뉴ID = 영문 3자 + 중메뉴 번호 2자리 + 소메뉴 번호 2자리"
      onRefresh={() => void list.refetch()}
      refreshing={list.isFetching}
      extra={<ActionBar screenKey={screenKey} size="small" hotkeys={active} actions={actions} />}
    >
      <SearchForm form={form} fields={MENU_FIELDS} onSearch={onSearch} loading={list.isFetching} />
      <ResultHeader
        title="메뉴 목록"
        total={rows.length}
        selected={selected.length}
        note="트리 순서(같은 상위 안은 순서·메뉴ID 순). 행 더블클릭 = 수정, 체크 = 수정(1건)·삭제(여러 건). 삭제하면 그 메뉴의 권한별 메뉴 행도 함께 지워집니다."
      >
        <ExcelButton onClick={excel} disabled={!rows.length} />
      </ResultHeader>
      <DataGrid<MenuMgtRow>
        columns={columns}
        rows={rows}
        rowKey="menuId"
        loading={list.isFetching}
        searched
        grade="G1"
        clientPageSize={0}
        sortable={false}
        rowSelection={{ selectedRowKeys: selected, onChange: (keys) => setSelected(keys.map(String)) }}
        onRowDoubleClick={(r) => setEdit({ open: true, target: r })}
      />
      <MenuEditModal open={edit.open} target={edit.target} defaultTopMenuId={params.topMenuId} onClose={() => setEdit({ open: false, target: null })} />
    </PageFrame>
  );
};

export default MenuMgtPage;
