import { useCallback, useMemo, useState } from 'react';
import { Form } from 'antd';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { noticeApi, noticeKeys } from '@/api/endpoints/notice';
import { usePagedSearch } from '@/hooks/usePagedSearch';
import { useOfficeLabel } from '@/hooks/useLookup';
import { usePrint } from '@/hooks/usePrint';
import { usePermission } from '@/hooks/usePermission';
import { useAuthStore } from '@/stores/useAuthStore';
import { PageFrame } from '@/components/page/PageFrame';
import { ResultHeader } from '@/components/page/ResultHeader';
import { ExcelButton } from '@/components/page/ExcelButton';
import { SearchForm } from '@/components/search/SearchForm';
import { ActionBar, type ActionItem } from '@/components/auth/ActionBar';
import { DataGrid } from '@/components/grid/DataGrid';
import { excelColumnsOf } from '@/components/grid/columns';
import { PrintLayout } from '@/components/print/PrintLayout';
import { downloadExcel } from '@/utils/excel';
import { fmtCount, fmtDate } from '@/utils/format';
import { CMSG } from '@/config/messages';
import type { Notice, NoticeSearch } from '@/domain/notice';
import type { ScreenProps } from '@/types/screen';
import { NoticeEditModal } from '../popup/NoticeEditModal';
import { NoticeViewModal } from '../popup/NoticeViewModal';
import { NEW_DAYS, canDeleteNotice, canEditNotice, noticeColumns, noticePrintConditions, permNote, scopeText, toNoticeParams, useNoticeFields } from './NoticeParts';

const TITLE = '공지사항';

/**
 * 공지사항 — "팝업형 CRUD" 견본이자 실제 화면(설계 §8.2, API 계약 §3, TB_NOTICE).
 *  조회조건(SearchForm, 모두 공통 필드) → [조회] → 서버 페이징 목록(DataGrid G1, 체크 선택) → 제목 클릭·행 더블클릭 = 상세 팝업(조회수 +1, 이전·다음 글)
 *  [등록]·[수정](1건 선택) = 입력 팝업(NoticeEditModal), [삭제](여러 건 선택 + 확인창), [엑셀]·[출력] = 지금 보이는 목록.
 *  공지 범위·권한(2026-11-13 사용자 결정): 목록 = 전체 공지 + 내 사무소(관리자 = 모두), 남의 글 수정·삭제·전체 공지 등록 = 관리자만.
 *  버튼은 고른 글이 내 글이 아니면 흐려진다(서버도 403 NTC_002·003 으로 다시 막음). 권한이 조회뿐이면 위에 '읽기 전용'.
 *  성공·오류 문구는 서버가 만들고 자동 표시된다 — 이 화면은 onSuccess 에서 목록 다시 읽기(invalidate)만 한다.
 */
const NoticePage = ({ screenKey }: ScreenProps) => {
  const [form] = Form.useForm();
  const qc = useQueryClient();
  const [selected, setSelected] = useState<number[]>([]);
  const [edit, setEdit] = useState<{ open: boolean; no: number | null }>({ open: false, no: null });
  const [viewOpen, setViewOpen] = useState(false);

  const perm = usePermission(screenKey);
  const myPerNo = useAuthStore((s) => s.me?.perNo ?? undefined);
  const fields = useNoticeFields(perm.isAdmin);
  const search = usePagedSearch<NoticeSearch, Notice>({ key: noticeKeys.list, fetch: noticeApi.list });
  const officeLabel = useOfficeLabel(search.params?.ofcCode);
  const { print } = usePrint(TITLE);
  const invalidate = () => qc.invalidateQueries({ queryKey: noticeKeys.all });

  // 상세 열기 = 조회수 +1 이 일어나는 요청이라 mutation 으로(다시 그릴 때 저절로 재요청되지 않게). 성공하면 목록의 조회수도 다시 읽는다
  const view = useMutation({ mutationFn: (no: number) => noticeApi.get(no, true), onSuccess: invalidate });
  const remove = useMutation({ mutationFn: noticeApi.remove, onSuccess: invalidate });

  const { mutate: openDetail, isPending: viewPending } = view;
  const openView = useCallback(
    (r: Notice) => {
      if (viewPending) return; // 제목 더블클릭(클릭 2번 + 더블클릭)에도 요청은 한 번만
      setViewOpen(true);
      openDetail(r.no, { onError: () => setViewOpen(false) }); // 오류 문구는 전역에서 띄운다
    },
    [viewPending, openDetail],
  );
  const columns = useMemo(() => noticeColumns(search.page, search.size, openView), [search.page, search.size, openView]);

  const selectedRows = search.rows.filter((r) => selected.includes(r.no));
  const canEditSelected = selectedRows.length === 1 && canEditNotice(selectedRows[0], perm, myPerNo);
  const canDeleteSelected = selectedRows.length > 0 && selectedRows.every((r) => canDeleteNotice(r, perm, myPerNo));

  const onSearch = (v: Record<string, unknown>) => {
    setSelected([]);
    search.search(toNoticeParams(v));
  };
  const excel = () => downloadExcel<Notice>(excelColumnsOf(columns), search.rows, TITLE);
  const onDelete = () => remove.mutate(selected, { onSuccess: () => setSelected([]) });

  const actions: ActionItem[] = [
    { key: 'search', onClick: () => form.submit(), loading: search.loading },
    { key: 'excel', onClick: excel, disabled: !search.rows.length },
    { key: 'print', onClick: () => print(), disabled: !search.searched },
    { key: 'create', onClick: () => setEdit({ open: true, no: null }) },
    { key: 'update', onClick: () => setEdit({ open: true, no: selected[0] }), disabled: !canEditSelected },
    { key: 'delete', onClick: onDelete, disabled: !canDeleteSelected, loading: remove.isPending, confirm: CMSG.deleteSelected(selected.length) },
  ];

  return (
    <PageFrame
      screenKey={screenKey}
      alert={permNote(perm)}
      lastUpdated={search.lastUpdated}
      onRefresh={() => void search.refetch()}
      refreshing={search.loading}
      extra={<ActionBar screenKey={screenKey} size="small" hotkeys actions={actions} />}
    >
      <SearchForm form={form} fields={fields} onSearch={onSearch} loading={search.loading} onReset={search.reset} />
      <ResultHeader title="공지 목록" total={search.searched ? search.total : undefined} selected={selected.length} note={search.searched ? `제목 클릭·행 더블클릭 = 상세(조회수 +1), 체크 = 수정(1건)·삭제(여러 건) 대상, N = 최근 ${NEW_DAYS}일 안에 쓴 글` : undefined}>
        <ExcelButton onClick={excel} disabled={!search.rows.length} />
      </ResultHeader>
      <DataGrid<Notice>
        columns={columns}
        rows={search.rows}
        rowKey="no"
        loading={search.loading}
        searched={search.searched}
        grade="G1"
        pagination={search.pagination}
        rowSelection={{ selectedRowKeys: selected, onChange: (keys) => setSelected(keys.map(Number)) }}
        onRowDoubleClick={openView}
      />
      <NoticeViewModal
        open={viewOpen}
        notice={view.data}
        loading={view.isPending}
        canEdit={canEditNotice(view.data, perm, myPerNo)}
        canDelete={canDeleteNotice(view.data, perm, myPerNo)}
        deleting={remove.isPending}
        onClose={() => setViewOpen(false)}
        onMove={(no) => openDetail(no, { onError: () => setViewOpen(false) })}
        onEdit={(n) => {
          setViewOpen(false);
          setEdit({ open: true, no: n.no });
        }}
        onDelete={(n) =>
          remove.mutate([n.no], {
            onSuccess: () => {
              setViewOpen(false);
              setSelected((s) => s.filter((x) => x !== n.no));
            },
          })
        }
      />
      <NoticeEditModal open={edit.open} no={edit.no} onClose={() => setEdit({ open: false, no: null })} />
      <PrintLayout title={TITLE} conditions={noticePrintConditions(search.params, officeLabel, perm.isAdmin)}>
        <table>
          <thead>
            <tr><th>순번</th><th>번호</th><th>범위</th><th>제목</th><th>작성자</th><th>작성일</th><th>조회수</th></tr>
          </thead>
          <tbody>
            {search.rows.map((r, i) => (
              <tr key={r.no}>
                <td>{(search.page - 1) * search.size + i + 1}</td><td>{r.no}</td><td>{scopeText(r)}</td><td>{r.subject}</td><td>{r.name}</td><td>{fmtDate(r.wrtDate)}</td><td>{fmtCount(r.visit)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </PrintLayout>
    </PageFrame>
  );
};

export default NoticePage;
