import { useEffect, useMemo, useState } from 'react';
import { Form, Segmented } from 'antd';
import { FileTextOutlined, UnorderedListOutlined } from '@ant-design/icons';
import { journalListApi, journalListKeys } from '@/api/endpoints/journalList';
import { useSearch } from '@/hooks/usePagedSearch';
import { usePermission } from '@/hooks/usePermission';
import { useOfficeLabel } from '@/hooks/useLookup';
import { selectOfcCode, useAuthStore } from '@/stores/useAuthStore';
import { PageFrame } from '@/components/page/PageFrame';
import { ResultHeader } from '@/components/page/ResultHeader';
import { EchoBar } from '@/components/page/EchoBar';
import { SearchForm } from '@/components/search/SearchForm';
import { normOfcCode } from '@/components/search/fields';
import { ActionBar, type ActionItem } from '@/components/auth/ActionBar';
import { DataGrid } from '@/components/grid/DataGrid';
import { fmtDateTime14 } from '@/utils/format';
import { dayjs } from '@/utils/date';
import type { JournalListRow, JournalListSearch } from '@/domain/journalList';
import type { ScreenProps } from '@/types/screen';
import { IT_OFC_CODE, canPickOffice, rollText, toJournalListParams, useJournalListFields } from './JournalListParts';
import { journalListColumns, toRows, type JournalListGridRow } from './JournalListColumns';

type ViewMode = 'list' | 'roll';

/**
 * 저널조회 — 자동화기기 1대의 전자저널(TB_JL_JNAL, API 계약 §13).
 *  조회조건(공통 필드, 배치 = 피그마) → [조회] → 에코바(사무소·기번·조회일시) → [목록 보기 | 롤방식 보기]
 *  목록 = 서버가 합친 전체 행을 20건씩(화면 페이징), 롤방식 = 인자 글 이어 보기. 명세표 팝업([상세])은 나중(popup.slipImage).
 *  사무소: 권한 A·T·L = 고름, 그 밖 = 내 사무소 고정(서버도 덮어씀). IT부문 비관리자 = 조회조건 칸 없음. 조회만 — 엑셀·인쇄 없음(As-Is 와 같음).
 */
const JournalListPage = ({ screenKey }: ScreenProps) => {
  const [form] = Form.useForm();
  const [mode, setMode] = useState<ViewMode>('list');
  const [searchedAt, setSearchedAt] = useState<string | null>(null);
  const perm = usePermission(screenKey);
  const myOfc = useAuthStore(selectOfcCode);
  const auth = useAuthStore((s) => s.me?.auth ?? null);
  const canPick = perm.isAdmin || canPickOffice(auth);
  const itRestricted = !perm.isAdmin && !!myOfc && normOfcCode(myOfc) === IT_OFC_CODE;
  const fields = useJournalListFields(canPick, myOfc, itRestricted);
  // 사무소 칸 기본 = 내 사무소(못 고르면 고정). 처음 값은 화면을 열 때 한 번만 들어가므로, 내 정보가 늦게 읽혀도 맞춘다
  useEffect(() => {
    if (myOfc && (!canPick || !form.getFieldValue('ofcCode'))) form.setFieldValue('ofcCode', myOfc);
  }, [canPick, myOfc, form]);

  const search = useSearch<JournalListSearch, JournalListRow[]>({ key: journalListKeys.list, fetch: journalListApi.list });
  const rows = useMemo(() => toRows(search.data ?? []), [search.data]);
  const columns = useMemo(() => journalListColumns(), []);
  const roll = useMemo(() => (mode === 'roll' ? rollText(rows) : ''), [mode, rows]);
  const officeLabel = useOfficeLabel(search.params?.ofcCode);

  const onSearch = (v: Record<string, unknown>) => {
    setSearchedAt(dayjs().format('YYYYMMDDHHmmss'));
    search.search(toJournalListParams(v));
  };
  const actions: ActionItem[] = [{ key: 'search', onClick: () => form.submit(), loading: search.loading }];

  return (
    <PageFrame
      screenKey={screenKey}
      lastUpdated={search.lastUpdated}
      onRefresh={() => void search.refetch()}
      refreshing={search.loading}
      extra={<ActionBar screenKey={screenKey} size="small" hotkeys actions={actions} />}
    >
      <SearchForm form={form} fields={fields} onSearch={onSearch} loading={search.loading} onReset={search.reset} />
      <EchoBar
        columns={3}
        cells={[
          { label: '사무소', value: search.params ? officeLabel : '', align: 'left' },
          { label: '기번', value: search.params?.mcnNo ?? '' },
          { label: '조회일시', value: searchedAt ? fmtDateTime14(searchedAt) : '' },
        ]}
      />
      <ResultHeader total={search.searched ? rows.length : undefined}>
        <Segmented<ViewMode>
          size="small"
          value={mode}
          onChange={setMode}
          options={[
            { value: 'list', label: '목록 보기', icon: <UnorderedListOutlined /> },
            { value: 'roll', label: '롤방식 보기', icon: <FileTextOutlined /> },
          ]}
        />
      </ResultHeader>
      {mode === 'list' ? (
        <DataGrid<JournalListGridRow> columns={columns} rows={rows} rowKey="key" loading={search.loading} searched={search.searched} grade="G2" clientPageSize={20} />
      ) : (
        <pre className="mono-block grid-fill" style={{ overflow: 'auto' }} aria-label="롤방식 저널">
          {search.searched ? roll || '검색된 데이터가 없습니다.' : '조회조건을 넣고 [조회] 하세요.'}
        </pre>
      )}
    </PageFrame>
  );
};

export default JournalListPage;
