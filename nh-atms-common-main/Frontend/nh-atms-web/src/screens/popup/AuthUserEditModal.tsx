import { useState } from 'react';
import { DatePicker, Form, Input, Select } from 'antd';
import type { Rule } from 'antd/es/form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { authMgtApi, authMgtKeys } from '@/api/endpoints/authMgt';
import { DetailGrid } from '@/components/display/DetailGrid';
import { FormLoadGate, FormModal, FormModalFooter } from '@/components/display/FormModal';
import { formItem } from '@/components/forms/formItem';
import { OfficeSelector } from '@/components/search/fields';
import { AuthSelect, PersonSelect, codeNameLabel } from '@/components/search/lookupFields';
import { byteCount, maxBytes, maxLen, reqSelect } from '@/utils/rules';
import { fromYmd, toYmd, type Dayjs } from '@/utils/date';
import { fmtDateTime14, fmtOffice } from '@/utils/format';
import { trimOrUndef } from '@/utils/object';
import { DEPT_PER_NO, type AuthDept, type AuthUser, type AuthUserKey, type AuthUserKind, type AuthUserSave } from '@/domain/authMgt';
import { meApi } from '@/api/endpoints/me';
import { useAuthStore } from '@/stores/useAuthStore';
import { useNavStore } from '@/stores/useNavStore';
import { APV_STS_OPTIONS, AUTH_MAX_LEN, MEMO_MAX_BYTES } from '../sample/AuthMgtColumns';

/**
 * 권한관리 쓰기(사용자 권한 등록·수정·삭제, 메뉴 권한 저장) 성공 뒤 — 목록 다시 읽기 + **내 정보·내 메뉴도 다시 읽기**(2026-11-17).
 * 내 권한이나 내 권한코드의 메뉴 권한을 바꿨을 때 왼쪽 메뉴·버튼 권한이 새로 고침 없이 바로 따라오게 한다
 * (서버는 쓰기 뒤 사용자 캐시를 비우고, 메뉴는 요청마다 다시 읽는다). 권한관리 탭·이 팝업이 함께 쓴다.
 */
export const useAfterAuthChange = () => {
  const qc = useQueryClient();
  const setMe = useAuthStore((s) => s.setMe);
  const fetchMenus = useNavStore((s) => s.fetchMenus);
  return () => {
    void qc.invalidateQueries({ queryKey: authMgtKeys.all });
    // loadMe(true) 는 앱 전체를 '불러오는 중'으로 바꿔 탭이 닫히므로 조용히 다시 읽는다(실패하면 지금 값 그대로)
    void meApi.get().then(setMe, () => undefined).then(() => fetchMenus(true));
  };
};

/** 폼 값(화면 안) — 보낼 때 toBody 로 계약 AuthUserSave 모양 */
interface AuthUserFormValues {
  /** 개인권한 등록: 사람을 찾을 사무소(인원 콤보용, 서버로 보내지 않음) */
  ofcCode?: string;
  /** 개인권한 등록: 사람 */
  perNo?: string;
  /** 부서권한 등록: 부서(TB_OFC_RAW.SAMUSO) */
  deptCode?: string;
  auth: string;
  apvSts: string;
  dueDate?: Dayjs | null;
  memo?: string;
}

/* 이 팝업의 검사 목록 — 서버 AuthMgtService 와 같은 규칙(키 필수, 권한 1~2자, 승인상태 필수, 메모 100바이트) */
const RULES = {
  ofcCode: [reqSelect('사무소')],
  perNo: [reqSelect('사용자')],
  deptCode: [reqSelect('부서')],
  auth: [reqSelect('권한'), maxLen(AUTH_MAX_LEN)],
  apvSts: [reqSelect('승인상태')],
  dueDate: [],
  memo: [maxBytes(MEMO_MAX_BYTES)],
} satisfies Record<keyof AuthUserFormValues, Rule[]>;

/** 만료일(날짜) → 14자리. 그날 끝(23:59:59)까지 유효. 비우면 무기한 */
const toDue = (d?: Dayjs | null): string | undefined => {
  const ymd = toYmd(d ?? undefined);
  return ymd ? `${ymd}235959` : undefined;
};

/** 등록 본문 — 부서권한은 perNo '1' + deptCode, 개인권한은 perNo 만(부서·사무소는 서버가 TB_OFC_RAW 로 채움) */
const toBody = (v: AuthUserFormValues, kind: AuthUserKind): AuthUserSave => ({
  ...(kind === 'DEPT' ? { perNo: DEPT_PER_NO, deptCode: v.deptCode } : { perNo: v.perNo }),
  auth: v.auth.trim(),
  apvSts: v.apvSts,
  dueDate: toDue(v.dueDate),
  memo: trimOrUndef(v.memo),
});

/** '[부서코드] 부서명' · 사무소 '[사무소코드]' */
const deptText = (d?: AuthDept | null) => (d ? codeNameLabel(d.samuso, d.samusoName) : '-');

/** 부서권한 등록 — 부서 콤보(TB_OFC_RAW, 부서코드·부서명 검색). 사무소는 고른 부서의 ONLINE_CODE 로 서버가 채운다 */
const DeptSelect = ({ value, onChange, onPick }: { value?: string; onChange?: (v?: string) => void; onPick: (d: AuthDept | null) => void }) => {
  const [q, setQ] = useState('');
  const depts = useQuery({ queryKey: authMgtKeys.depts(q), queryFn: () => authMgtApi.depts(q || undefined) });
  const rows = depts.data ?? [];
  return (
    <Select
      value={value}
      showSearch
      filterOption={false}
      onSearch={setQ}
      onChange={(v?: string) => {
        onChange?.(v);
        onPick(rows.find((d) => d.samuso === v) ?? null);
      }}
      options={rows.map((d) => ({ value: d.samuso, label: deptText(d) }))}
      loading={depts.isFetching}
      allowClear
      placeholder="부서코드·부서명 검색"
      aria-label="부서"
      style={{ width: '100%' }}
    />
  );
};

/** 개인권한 등록 — 고른 사람의 소속 부서·사무소(TB_INSA → TB_OFC_RAW)를 보여 준다(저장할 때 서버가 같은 값으로 채움) */
const PersonDept = ({ perNo }: { perNo?: string }) => {
  const d = useQuery({ queryKey: authMgtKeys.personDept(perNo ?? ''), queryFn: () => authMgtApi.personDept(perNo as string), enabled: !!perNo, meta: { silent: true } });
  if (!perNo) return <span className="muted">사용자를 고르면 자동</span>;
  if (d.isError) return <span className="muted">소속 부서를 찾을 수 없음(저장 불가)</span>;
  return <span aria-label="소속 부서">{d.data ? `${deptText(d.data)} / 사무소 ${d.data.onlineCode ?? '-'}` : '…'}</span>;
};

interface Props {
  /** DEPT 부서권한 / PERSON 개인권한 */
  kind: AuthUserKind;
  open: boolean;
  /** null = 등록, 키 = 수정 */
  target: AuthUserKey | null;
  onClose: () => void;
}

const TITLE: Record<AuthUserKind, string> = { DEPT: '부서권한', PERSON: '개인권한' };

/**
 * 부서권한·개인권한 등록/수정 팝업(`popup.authUserEdit`). 모달 안 폼은 열릴 때마다 새로 마운트(destroyOnHidden + 안쪽 컴포넌트, 공지사항 입력 팝업과 같음).
 * 등록 — 부서권한: 부서(TB_OFC_RAW 콤보), 개인번호는 '1' 고정, 사무소는 그 부서의 ONLINE_CODE(서버가 채움).
 *        개인권한: 사무소 → 사용자, 부서·사무소는 그 사람의 TB_OFC_RAW(서버가 채움, 화면은 보여 주기만).
 * 수정: 키 3개는 바꿀 수 없다(보여 주기만) — 권한·승인상태·만료일·메모만. 성공 문구(S002·S003)는 서버 → 자동 표시.
 */
export const AuthUserEditModal = ({ kind, open, target, onClose }: Props) => (
  <FormModal open={open} onClose={onClose} title={`${TITLE[kind]} ${target === null ? '등록' : '수정'}`} screenKey="popup.authUserEdit">
    <AuthUserEditBody kind={kind} target={target} onClose={onClose} />
  </FormModal>
);

/** 수정이면 원본을 읽은 뒤 폼을 그린다. 닫으면 캐시도 버린다(gcTime 0) → 다음에 열 때 새로 읽음 */
const AuthUserEditBody = ({ kind, target, onClose }: { kind: AuthUserKind; target: AuthUserKey | null; onClose: () => void }) => {
  const detail = useQuery({ queryKey: authMgtKeys.user(target), queryFn: () => authMgtApi.user(target as AuthUserKey), enabled: target !== null, gcTime: 0 });
  return (
    <FormLoadGate editing={target !== null} isError={detail.isError} data={detail.data} what="권한" onClose={onClose}>
      {(row) => <AuthUserForm kind={kind} row={row} onClose={onClose} />}
    </FormLoadGate>
  );
};

const AuthUserForm = ({ kind, row, onClose }: { kind: AuthUserKind; row: AuthUser | null; onClose: () => void }) => {
  const [form] = Form.useForm<AuthUserFormValues>();
  const afterChange = useAfterAuthChange();
  const [pickedDept, setPickedDept] = useState<AuthDept | null>(null);
  const perNo = Form.useWatch('perNo', form) as string | undefined;
  const save = useMutation({
    mutationFn: (body: AuthUserSave) => (row ? authMgtApi.updateUser({ deptCode: row.deptCode, ofcCode: row.ofcCode, perNo: row.perNo }, body) : authMgtApi.createUser(body)),
    onSuccess: afterChange,
  });
  const initial: Partial<AuthUserFormValues> = row
    ? { auth: row.auth, apvSts: row.apvSts ?? 'Y', dueDate: row.dueDate ? fromYmd(row.dueDate.slice(0, 8)) ?? null : null, memo: row.memo ?? '' }
    : { apvSts: 'Y' };

  // 검증을 통과했을 때만 불린다. 닫기는 이 요청이 성공했을 때만
  const onFinish = (v: AuthUserFormValues) => save.mutate(toBody(v, kind), { onSuccess: onClose });

  const regInfo = { label: '등록정보', value: row ? (row.insDate ? `${fmtDateTime14(row.insDate)}${row.insId ? ` · ${row.insId}` : ''}` : undefined) : '저장할 때 지금 시각·내 개인번호로 기록' };
  const keyItems = row
    ? [
        { label: '부서', value: fmtOffice(row.deptCode, row.deptNm) },
        { label: '사무소', value: fmtOffice(row.ofcCode, row.kanm) },
        { label: '사용자', value: kind === 'DEPT' ? `부서권한 (개인번호 ${DEPT_PER_NO})` : codeNameLabel(row.perNo, row.name) },
        regInfo,
      ]
    : kind === 'DEPT'
      ? [
          { label: '부서', required: true, value: formItem('deptCode', <DeptSelect onPick={setPickedDept} />, RULES.deptCode) },
          { label: '사무소', value: pickedDept ? (pickedDept.onlineCode ?? '사무소코드 없음(저장 불가)') : '부서를 고르면 자동' },
          { label: '사용자', value: `부서권한 (개인번호 ${DEPT_PER_NO} 고정)` },
          regInfo,
        ]
      : [
          { label: '사무소', required: true, value: formItem('ofcCode', <OfficeSelector />, RULES.ofcCode) },
          { label: '사용자', required: true, value: formItem('perNo', <PersonSelect form={form} officeField="ofcCode" allowAll={false} clearOnParentChange placeholder="사무소를 먼저 고르세요" />, RULES.perNo) },
          { label: '소속 부서', value: <PersonDept perNo={perNo} /> },
          regInfo,
        ];

  return (
    <Form<AuthUserFormValues> form={form} component={false} initialValues={initial} onFinish={onFinish} scrollToFirstError>
      <DetailGrid
        cols={2}
        labelWidth={90}
        items={[
          ...keyItems,
          { label: '권한', required: true, value: formItem('auth', <AuthSelect allowAll={false} keepValue placeholder="권한 선택" />, RULES.auth) },
          { label: '승인상태', required: true, value: formItem('apvSts', <Select options={APV_STS_OPTIONS} aria-label="승인상태" style={{ width: 140 }} />, RULES.apvSts) },
          { label: '만료일', span: 2, value: formItem('dueDate', <DatePicker allowClear placeholder="비우면 무기한" aria-label="만료일" />, RULES.dueDate) },
          { label: '메모', span: 2, value: formItem('memo', <Input placeholder="메모" count={byteCount(MEMO_MAX_BYTES)} aria-label="메모" />, RULES.memo) },
        ]}
      />
      <FormModalFooter onClose={onClose} screenKey="sample.authMgt" need={row ? 'upd' : 'reg'} saving={save.isPending} onSave={() => form.submit()} />
    </Form>
  );
};
