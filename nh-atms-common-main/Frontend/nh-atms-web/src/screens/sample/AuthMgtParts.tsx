/**
 * 권한관리(`sample.authMgt`) 부품 — 탭 2개의 본체.
 *  ① 사용자 권한(UserAuthPane, TB_SCR_AUTH) — "팝업형 CRUD"(공지사항 견본 모양): 조회조건 → 목록(체크) → 등록·수정 팝업, 삭제
 *  ③ 메뉴 권한(MenuAuthPane, TB_SCR_AUTH_MENU) — "표 체크 편집·일괄 저장": 상단 조회조건 권한 콤보 → [조회] → 메뉴 전체 표 체크 → [저장](행추가·행삭제 없음)
 * 화면 본체는 AuthMgtPage.tsx(탭), 컬럼은 AuthMgtColumns.tsx, 입력 팝업은 popup/AuthUserEditModal.tsx.
 */
import { useMemo, useState } from 'react';
import { Form } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { authMgtApi, authMgtKeys } from '@/api/endpoints/authMgt';
import { usePagedSearch } from '@/hooks/usePagedSearch';
import { useAuthCodes } from '@/hooks/useLookup';
import { usePermission, type Permission } from '@/hooks/usePermission';
import { useAppMessage } from '@/hooks/useAppMessage';
import { ResultHeader } from '@/components/page/ResultHeader';
import { ExcelButton } from '@/components/page/ExcelButton';
import { SearchForm, type SearchField } from '@/components/search/SearchForm';
import { officeField } from '@/components/search/fields';
import { authField } from '@/components/search/lookupFields';
import { keywordField, keywordOf, optionField } from '@/components/search/optionFields';
import { allToUndef } from '@/components/search/params';
import { ActionBar, type ActionItem } from '@/components/auth/ActionBar';
import { DENIED_TEXT } from '@/components/auth/useAllowed';
import { DataGrid } from '@/components/grid/DataGrid';
import { EditableGrid, type EditableChanges } from '@/components/grid/EditableGrid';
import { excelColumnsOf } from '@/components/grid/columns';
import { downloadExcel } from '@/utils/excel';
import { compactParams } from '@/utils/object';
import { CMSG } from '@/config/messages';
import type { AuthMenu, AuthUser, AuthUserKey, AuthUserKind, AuthUserSearch } from '@/domain/authMgt';
import type { ScreenKey } from '@/screens/screenIds';
import { AuthUserEditModal, useAfterAuthChange } from '../popup/AuthUserEditModal';
import { APV_STS_OPTIONS, authLabel, authMenuColumns, userColumns, userKeyText } from './AuthMgtColumns';

/* ======================= ①·② 부서권한 / 개인권한 ======================= */

/** 탭별 이름 — 부서권한(PER_NO '1') / 개인권한 */
export const KIND_TITLE: Record<AuthUserKind, string> = { DEPT: '부서권한', PERSON: '개인권한' };

/** 조회조건(모두 공통 필드): 사무소(비우면 전체) · 검색어(부서: 부서코드·부서명 / 개인: 개인번호·성명, AS-IS "개인사번") · 권한 · 승인상태 */
export const userFields = (kind: AuthUserKind): SearchField[] => [
  officeField({ required: false }),
  kind === 'DEPT' ? keywordField({ label: '부서', placeholder: '부서코드·부서명' }) : keywordField({ label: '개인번호/성명', placeholder: '개인번호·성명' }),
  authField(),
  optionField('apvSts', '승인상태', APV_STS_OPTIONS),
];

/** 폼 값 → 조회 파라미터(빈 값은 뺀다). kind 로 부서권한/개인권한만 */
export const toUserParams = (v: Record<string, unknown>, kind: AuthUserKind): Omit<AuthUserSearch, 'page' | 'size'> =>
  compactParams<AuthUserSearch>({
    kind,
    ofcCode: (v.ofcCode as string | undefined) || undefined,
    q: keywordOf(v.q),
    auth: allToUndef(v.auth),
    apvSts: allToUndef(v.apvSts),
  });

const keyOf = (r: AuthUser): AuthUserKey => ({ deptCode: r.deptCode, ofcCode: r.ofcCode, perNo: r.perNo });

/**
 * 부서권한·개인권한 탭(같은 부품, kind 로 구분) — 조회 → 목록(체크 선택) → [등록]·[수정](1건)·[삭제](여러 건, 확인창), 행 더블클릭 = 수정 팝업.
 * 로그인 권한 규칙: 유효한(승인·미만료) 개인권한이 있으면 그것만, 없으면 내 부서(TB_OFC_RAW)의 부서권한.
 * 성공·오류 문구는 서버가 만들고 자동 표시 — onSuccess 에서 목록 다시 읽기(invalidate)만.
 */
export const UserAuthPane = ({ screenKey, active, kind }: { screenKey: ScreenKey; active: boolean; kind: AuthUserKind }) => {
  const [form] = Form.useForm();
  const afterChange = useAfterAuthChange();
  const [selected, setSelected] = useState<string[]>([]);
  const [edit, setEdit] = useState<{ open: boolean; target: AuthUserKey | null }>({ open: false, target: null });

  const search = usePagedSearch<AuthUserSearch, AuthUser>({ key: authMgtKeys.users, fetch: authMgtApi.users });
  const fields = useMemo(() => userFields(kind), [kind]);
  const columns = useMemo(() => userColumns(search.page, search.size, kind), [search.page, search.size, kind]);
  const remove = useMutation({ mutationFn: authMgtApi.removeUsers, onSuccess: afterChange });

  const selectedRows = search.rows.filter((r) => selected.includes(userKeyText(r)));
  const onSearch = (v: Record<string, unknown>) => {
    setSelected([]);
    search.search(toUserParams(v, kind));
  };
  const excel = () => downloadExcel<AuthUser>(excelColumnsOf(columns), search.rows, KIND_TITLE[kind]);
  const onDelete = () => remove.mutate(selectedRows.map(keyOf), { onSuccess: () => setSelected([]) });

  const actions: ActionItem[] = [
    { key: 'search', onClick: () => form.submit(), loading: search.loading },
    { key: 'excel', onClick: excel, disabled: !search.rows.length },
    { key: 'create', onClick: () => setEdit({ open: true, target: null }) },
    { key: 'update', onClick: () => setEdit({ open: true, target: keyOf(selectedRows[0]) }), disabled: selectedRows.length !== 1 },
    { key: 'delete', onClick: onDelete, disabled: !selectedRows.length, loading: remove.isPending, confirm: CMSG.deleteSelected(selectedRows.length, KIND_TITLE[kind]) },
  ];

  return (
    <>
      <ActionBar screenKey={screenKey} size="small" hotkeys={active} actions={actions} style={{ marginBottom: 8 }} />
      <SearchForm form={form} fields={fields} onSearch={onSearch} loading={search.loading} onReset={search.reset} />
      <ResultHeader
        title={`권한목록(${kind === 'DEPT' ? '부서별' : '개인별'})`}
        total={search.searched ? search.total : undefined}
        selected={selected.length}
        note={search.searched ? '행 더블클릭 = 수정, 체크 = 수정(1건)·삭제(여러 건) 대상' : undefined}
      >
        <ExcelButton onClick={excel} disabled={!search.rows.length} />
      </ResultHeader>
      <DataGrid<AuthUser>
        columns={columns}
        rows={search.rows}
        rowKey={userKeyText}
        loading={search.loading}
        searched={search.searched}
        grade="G1"
        pagination={search.pagination}
        rowSelection={{ selectedRowKeys: selected, onChange: (keys) => setSelected(keys.map(String)) }}
        onRowDoubleClick={(r) => setEdit({ open: true, target: keyOf(r) })}
      />
      <AuthUserEditModal kind={kind} open={edit.open} target={edit.target} onClose={() => setEdit({ open: false, target: null })} />
    </>
  );
};

/* ======================= ③ 메뉴 권한 ======================= */

const NO_MENUS: AuthMenu[] = [];
const MENU_EXCEL_COLUMNS = excelColumnsOf(authMenuColumns());

/** 메뉴 권한 키 '권한-메뉴ID' (서버 AuthMgtService.menuKeyText 와 같은 글) */
export const menuKeyText = (r: Pick<AuthMenu, 'auth' | 'menuId'>): string => `${r.auth}-${r.menuId}`;

/** 조회조건: 권한(필수, 있는 권한코드 콤보) */
const MENU_AUTH_FIELDS: SearchField[] = [authField({ required: true, widthClass: 'normal' })];

/**
 * 변경 종류별 권한 중 모자란 것이 있나 — 서버와 같은 규칙:
 * 고친 행 중 DB 에 행이 없던 것(mappedYn 'N') = 새로 넣음 REG, 있던 것 = 고침 UPD. 행 삭제는 없다(사용 해제 = 고침)
 */
export const lacksMenuPermission = (ch: EditableChanges<AuthMenu>, p: Pick<Permission, 'canReg' | 'canUpd'>): boolean =>
  ch.updated.some((r) => (r.mappedYn === 'Y' ? !p.canUpd : !p.canReg));

/** 고른 권한의 메뉴 전체 표(EditableGrid — 체크 후 [저장] 한 번에, 행추가·행삭제 없음) */
const AuthMenuEditor = ({ auth, authText, perm, saving, onSave }: { auth: string; authText: string; perm: Permission; saving: boolean; onSave: (ch: EditableChanges<AuthMenu>) => Promise<void> }) => {
  const menus = useQuery({ queryKey: authMgtKeys.authMenus(auth), queryFn: () => authMgtApi.authMenus(auth) });
  const rows = menus.data ?? NO_MENUS;
  const columns = useMemo(() => authMenuColumns(), []);
  const canEdit = perm.canReg || perm.canUpd;
  return (
    <>
      {/* 제목·건수·변경 내역·[엑셀]·[저장]·[취소]를 한 줄로(EditableGrid title) — 표 높이 확보 */}
      <EditableGrid<AuthMenu>
        title={`메뉴 권한 — ${authText}`}
        columns={columns}
        rows={rows}
        rowKey="menuId"
        readOnly={!canEdit}
        canAdd={false}
        canUpdate={canEdit}
        canDelete={false}
        rowButtons={false}
        loading={menus.isFetching || saving}
        toolbarExtra={<ExcelButton onClick={() => void downloadExcel<AuthMenu>(MENU_EXCEL_COLUMNS, rows, `메뉴권한_${auth}`)} disabled={!rows.length} />}
        onSave={onSave}
      />
    </>
  );
};

/**
 * 메뉴 권한 탭 — 상단 조회조건에서 권한 선택 → [조회] → 메뉴 전체 표(조회·등록·수정·삭제·실행·사용 체크) → [저장].
 *  - 이 권한의 TB_SCR_AUTH_MENU 행이 없는 메뉴는 모두 미체크(사용 N)로 보인다. 체크 후 저장하면 PK(권한+메뉴ID)로 새로 들어간다.
 *  - 행삭제는 없다 — 사용을 끄고 저장하면 그 행의 USE_YN 이 'N' 이 된다(행은 남음). 사용을 안 켜고 다른 권한만 켜도 그대로 저장(사용 N).
 * 저장 = `POST /api/web/auth-mgt/menus/batch`(한 트랜잭션, S009). 권한은 저장 전에 한 번 보고 서버도 검사한다.
 * 권한코드 콤보 = 부서권한·개인권한·메뉴 권한에 실제로 있는 값 — 새 권한코드는 부서권한/개인권한 탭에서 먼저 부여하면 나온다.
 */
export const MenuAuthPane = ({ screenKey, active }: { screenKey: ScreenKey; active: boolean }) => {
  const [form] = Form.useForm();
  const qc = useQueryClient();
  const { message } = useAppMessage();
  const perm = usePermission(screenKey);
  const codes = useAuthCodes();
  const [auth, setAuth] = useState<string | null>(null);

  const afterChange = useAfterAuthChange();
  const save = useMutation({ mutationFn: authMgtApi.saveAuthMenus, onSuccess: afterChange });

  const onSearch = (v: Record<string, unknown>) => {
    const a = allToUndef(v.auth);
    if (!a) return;
    if (a === auth) void qc.invalidateQueries({ queryKey: authMgtKeys.authMenus(a) });
    setAuth(a);
  };

  const onSave = async (ch: EditableChanges<AuthMenu>) => {
    if (lacksMenuPermission(ch, perm)) {
      void message.warning(DENIED_TEXT);
      return;
    }
    await save.mutateAsync({ added: [], updated: ch.updated, deleted: [] }).then(
      () => undefined,
      () => undefined,
    );
  };

  const code = codes.data?.find((c) => c.auth === auth);
  return (
    <>
      <ActionBar screenKey={screenKey} size="small" hotkeys={active} style={{ marginBottom: 8 }} actions={[{ key: 'search', onClick: () => form.submit() }]} />
      <SearchForm form={form} fields={MENU_AUTH_FIELDS} onSearch={onSearch} />
      {auth ? (
        <AuthMenuEditor key={auth} auth={auth} authText={authLabel(auth, code?.authNm)} perm={perm} saving={save.isPending} onSave={onSave} />
      ) : (
        <div className="grid-empty">권한을 고르고 [조회]를 누르세요.</div>
      )}
    </>
  );
};
