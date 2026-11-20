import { Form, Input, InputNumber, Radio } from 'antd';
import type { Rule } from 'antd/es/form';
import { EditOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { codeMgtApi, codeMgtKeys } from '@/api/endpoints/codeMgt';
import { AuthButton } from '@/components/auth/AuthButton';
import { DetailGrid } from '@/components/display/DetailGrid';
import { DetailModal } from '@/components/display/DetailModal';
import { FormLoadGate, FormModal, FormModalFooter } from '@/components/display/FormModal';
import { formItem } from '@/components/forms/formItem';
import { YN_OPTIONS } from '@/components/search/lookupFields';
import { VMSG } from '@/config/messages';
import { byteCount, check, maxBytes, maxLen, numRange, req, reqSelect } from '@/utils/rules';
import { trimOrUndef } from '@/utils/object';
import type { CodeDetail, CodeDetailSave, CodeMaster, CodeMasterSave } from '@/domain/codeMgt';
import type { Yn } from '@/types/menu';
import {
  CODE_NM_MAX_BYTES,
  DATA4_MAX,
  DATA4_MIN,
  DATA_MAX_BYTES,
  DESC_MAX_BYTES,
  KEY_MAX_LEN,
  SECT_NM_MAX_BYTES,
  TASK_SECT_MAX_BYTES,
  detailItems,
  masterItems,
} from '../sample/CodeMgtColumns';

/**
 * 코드관리 팝업 4개(`popup.codeMasterView`·`codeMasterEdit`·`codeDetailView`·`codeDetailEdit`) — 공통 틀만 쓴다.
 *  상세 = DetailModal(+[수정]), 등록·수정 = FormModal + FormLoadGate + FormModalFooter(2026-11-16 사용자 결정: 같은 틀 복사 금지).
 *  키(코드구분·코드)는 등록 때만 입력, 수정 때는 보이기만. 성공 문구(S002·S003)는 서버 → 자동 표시.
 */

const SCREEN = 'sample.codeMgt' as const;

/** 코드구분·코드: 공백 없이 1~10자(서버 CodeMgtRules.validKey 와 같음) */
const keyRule = check<string | undefined>((v) => (!v || /^\S{1,10}$/.test(v.trim()) ? undefined : VMSG.noSpaceKey(KEY_MAX_LEN)));

/* ======================= 코드구분 ======================= */

interface MasterFormValues {
  codeSect?: string;
  codeSectNm: string;
  usedYn: Yn;
  taskSect?: string;
  prcSect?: string;
  desc1?: string;
  desc2?: string;
  desc3?: string;
  desc4?: string;
  desc5?: string;
}

const M_RULES = {
  codeSect: [req('코드구분'), keyRule],
  codeSectNm: [req('코드명'), maxBytes(SECT_NM_MAX_BYTES)],
  usedYn: [reqSelect('사용여부')],
  taskSect: [maxBytes(TASK_SECT_MAX_BYTES)],
  prcSect: [maxLen(1)],
  desc1: [maxBytes(DESC_MAX_BYTES)],
  desc2: [maxBytes(DESC_MAX_BYTES)],
  desc3: [maxBytes(DESC_MAX_BYTES)],
  desc4: [maxBytes(DESC_MAX_BYTES)],
  desc5: [maxBytes(DESC_MAX_BYTES)],
} satisfies Record<keyof MasterFormValues, Rule[]>;

const toMasterBody = (v: MasterFormValues): CodeMasterSave => ({
  codeSect: trimOrUndef(v.codeSect),
  codeSectNm: v.codeSectNm.trim(),
  usedYn: v.usedYn,
  taskSect: trimOrUndef(v.taskSect),
  prcSect: trimOrUndef(v.prcSect),
  desc1: trimOrUndef(v.desc1),
  desc2: trimOrUndef(v.desc2),
  desc3: trimOrUndef(v.desc3),
  desc4: trimOrUndef(v.desc4),
  desc5: trimOrUndef(v.desc5),
});

/** 코드구분 상세 — 표에 없는 업무구분·처리구분·설명1~5·등록/변경. [수정] = 부모가 이 팝업을 닫고 입력 팝업을 연다 */
export const CodeMasterViewModal = ({ codeSect, onClose, onEdit }: { codeSect: string | null; onClose: () => void; onEdit: (codeSect: string) => void }) => {
  const q = useQuery({ queryKey: codeMgtKeys.master(codeSect ?? ''), queryFn: () => codeMgtApi.master(codeSect as string), enabled: codeSect !== null, gcTime: 0 });
  return (
    <DetailModal
      open={codeSect !== null}
      onClose={onClose}
      title="코드구분 상세"
      screenKey="popup.codeMasterView"
      loading={!q.data}
      actions={
        q.data && (
          <AuthButton screenKey={SCREEN} need="upd" type="primary" icon={<EditOutlined />} onClick={() => onEdit(q.data.codeSect)}>
            수정
          </AuthButton>
        )
      }
    >
      {q.data && <DetailGrid cols={2} labelWidth={90} items={masterItems(q.data)} />}
    </DetailModal>
  );
};

/** 코드구분 등록(target null)·수정 */
export const CodeMasterEditModal = ({ open, target, onClose }: { open: boolean; target: string | null; onClose: () => void }) => (
  <FormModal open={open} onClose={onClose} title={target === null ? '코드구분 등록' : '코드구분 수정'} screenKey="popup.codeMasterEdit">
    <MasterEditBody target={target} onClose={onClose} />
  </FormModal>
);

const MasterEditBody = ({ target, onClose }: { target: string | null; onClose: () => void }) => {
  const q = useQuery({ queryKey: codeMgtKeys.master(target ?? ''), queryFn: () => codeMgtApi.master(target as string), enabled: target !== null, gcTime: 0 });
  return (
    <FormLoadGate editing={target !== null} isError={q.isError} data={q.data} what="코드구분" onClose={onClose}>
      {(row) => <MasterForm row={row} onClose={onClose} />}
    </FormLoadGate>
  );
};

const MasterForm = ({ row, onClose }: { row: CodeMaster | null; onClose: () => void }) => {
  const [form] = Form.useForm<MasterFormValues>();
  const qc = useQueryClient();
  const save = useMutation({
    mutationFn: (body: CodeMasterSave) => (row ? codeMgtApi.updateMaster(row.codeSect, body) : codeMgtApi.createMaster(body)),
    onSuccess: () => qc.invalidateQueries({ queryKey: codeMgtKeys.all }),
  });
  const initial: Partial<MasterFormValues> = row
    ? {
        codeSectNm: row.codeSectNm,
        usedYn: row.usedYn ?? 'Y',
        taskSect: row.taskSect ?? undefined,
        prcSect: row.prcSect ?? undefined,
        desc1: row.desc1 ?? undefined,
        desc2: row.desc2 ?? undefined,
        desc3: row.desc3 ?? undefined,
        desc4: row.desc4 ?? undefined,
        desc5: row.desc5 ?? undefined,
      }
    : { usedYn: 'Y' };
  const onFinish = (v: MasterFormValues) => save.mutate(toMasterBody(v), { onSuccess: onClose });
  const text = (name: keyof MasterFormValues, label: string, max: number) => formItem(name, <Input placeholder={label} count={byteCount(max)} aria-label={label} />, M_RULES[name]);

  return (
    <Form<MasterFormValues> form={form} component={false} initialValues={initial} onFinish={onFinish} scrollToFirstError>
      <DetailGrid
        cols={2}
        labelWidth={90}
        items={[
          row
            ? { label: '코드구분', value: <span className="mono">{row.codeSect}</span> }
            : { label: '코드구분', required: true, value: formItem('codeSect', <Input placeholder={`공백 없이 ${KEY_MAX_LEN}자 이내`} maxLength={KEY_MAX_LEN} aria-label="코드구분" />, M_RULES.codeSect) },
          { label: '사용여부', required: true, value: formItem('usedYn', <Radio.Group options={YN_OPTIONS} aria-label="사용여부" />, M_RULES.usedYn) },
          { label: '코드명', required: true, span: 2, value: text('codeSectNm', '코드명', SECT_NM_MAX_BYTES) },
          { label: '업무구분', value: text('taskSect', '업무구분', TASK_SECT_MAX_BYTES) },
          { label: '처리구분', value: formItem('prcSect', <Input placeholder="한 글자" maxLength={1} aria-label="처리구분" style={{ width: 80 }} />, M_RULES.prcSect) },
          { label: '설명1', span: 2, value: text('desc1', '설명1', DESC_MAX_BYTES) },
          { label: '설명2', span: 2, value: text('desc2', '설명2', DESC_MAX_BYTES) },
          { label: '설명3', span: 2, value: text('desc3', '설명3', DESC_MAX_BYTES) },
          { label: '설명4', span: 2, value: text('desc4', '설명4', DESC_MAX_BYTES) },
          { label: '설명5', span: 2, value: text('desc5', '설명5', DESC_MAX_BYTES) },
        ]}
      />
      <FormModalFooter onClose={onClose} screenKey={SCREEN} need={row ? 'upd' : 'reg'} saving={save.isPending} onSave={() => form.submit()} />
    </Form>
  );
};

/* ======================= 상세코드 ======================= */

interface DetailFormValues {
  code?: string;
  codeNm: string;
  data1?: string;
  data2?: string;
  data3?: string;
  data4?: number | null;
  data5?: string;
}

const D_RULES = {
  code: [req('코드'), keyRule],
  codeNm: [req('코드명'), maxBytes(CODE_NM_MAX_BYTES)],
  data1: [maxBytes(DATA_MAX_BYTES)],
  data2: [maxBytes(DATA_MAX_BYTES)],
  data3: [maxBytes(DATA_MAX_BYTES)],
  data4: [numRange(DATA4_MIN, DATA4_MAX)],
  data5: [maxLen(1)],
} satisfies Record<keyof DetailFormValues, Rule[]>;

const toDetailBody = (v: DetailFormValues): CodeDetailSave => ({
  code: trimOrUndef(v.code),
  codeNm: v.codeNm.trim(),
  data1: trimOrUndef(v.data1),
  data2: trimOrUndef(v.data2),
  data3: trimOrUndef(v.data3),
  data4: v.data4 ?? undefined,
  data5: trimOrUndef(v.data5),
});

export interface DetailTarget {
  codeSect: string;
  code: string;
}

/** 상세코드 상세 — 데이터1~5·등록/변경 정보. [수정] = 부모가 입력 팝업을 연다 */
export const CodeDetailViewModal = ({ target, onClose, onEdit }: { target: DetailTarget | null; onClose: () => void; onEdit: (t: DetailTarget) => void }) => {
  const q = useQuery({
    queryKey: codeMgtKeys.detail(target?.codeSect ?? '', target?.code ?? ''),
    queryFn: () => codeMgtApi.detail((target as DetailTarget).codeSect, (target as DetailTarget).code),
    enabled: target !== null,
    gcTime: 0,
  });
  return (
    <DetailModal
      open={target !== null}
      onClose={onClose}
      title="상세코드 상세"
      screenKey="popup.codeDetailView"
      loading={!q.data}
      actions={
        q.data && (
          <AuthButton screenKey={SCREEN} need="upd" type="primary" icon={<EditOutlined />} onClick={() => onEdit({ codeSect: q.data.codeSect, code: q.data.code })}>
            수정
          </AuthButton>
        )
      }
    >
      {q.data && <DetailGrid cols={2} labelWidth={90} items={detailItems(q.data)} />}
    </DetailModal>
  );
};

/** 상세코드 등록(code null)·수정 — 코드구분은 왼쪽에서 고른 것 */
export const CodeDetailEditModal = ({ open, codeSect, code, onClose }: { open: boolean; codeSect: string; code: string | null; onClose: () => void }) => (
  <FormModal open={open} onClose={onClose} title={`상세코드 ${code === null ? '등록' : '수정'} — ${codeSect}`} screenKey="popup.codeDetailEdit">
    <DetailEditBody codeSect={codeSect} code={code} onClose={onClose} />
  </FormModal>
);

const DetailEditBody = ({ codeSect, code, onClose }: { codeSect: string; code: string | null; onClose: () => void }) => {
  const q = useQuery({ queryKey: codeMgtKeys.detail(codeSect, code ?? ''), queryFn: () => codeMgtApi.detail(codeSect, code as string), enabled: code !== null, gcTime: 0 });
  return (
    <FormLoadGate editing={code !== null} isError={q.isError} data={q.data} what="상세코드" onClose={onClose}>
      {(row) => <DetailForm codeSect={codeSect} row={row} onClose={onClose} />}
    </FormLoadGate>
  );
};

const DetailForm = ({ codeSect, row, onClose }: { codeSect: string; row: CodeDetail | null; onClose: () => void }) => {
  const [form] = Form.useForm<DetailFormValues>();
  const qc = useQueryClient();
  const save = useMutation({
    mutationFn: (body: CodeDetailSave) => (row ? codeMgtApi.updateDetail(codeSect, row.code, body) : codeMgtApi.createDetail(codeSect, body)),
    onSuccess: () => qc.invalidateQueries({ queryKey: codeMgtKeys.all }),
  });
  const initial: Partial<DetailFormValues> = row
    ? { codeNm: row.codeNm, data1: row.data1 ?? undefined, data2: row.data2 ?? undefined, data3: row.data3 ?? undefined, data4: row.data4, data5: row.data5 ?? undefined }
    : {};
  const onFinish = (v: DetailFormValues) => save.mutate(toDetailBody(v), { onSuccess: onClose });
  const text = (name: 'data1' | 'data2' | 'data3', label: string) => formItem(name, <Input placeholder={label} count={byteCount(DATA_MAX_BYTES)} aria-label={label} />, D_RULES[name]);

  return (
    <Form<DetailFormValues> form={form} component={false} initialValues={initial} onFinish={onFinish} scrollToFirstError>
      <DetailGrid
        cols={2}
        labelWidth={90}
        items={[
          { label: '코드구분', value: <span className="mono">{codeSect}</span> },
          row
            ? { label: '코드', value: <span className="mono">{row.code}</span> }
            : { label: '코드', required: true, value: formItem('code', <Input placeholder={`공백 없이 ${KEY_MAX_LEN}자 이내`} maxLength={KEY_MAX_LEN} aria-label="코드" />, D_RULES.code) },
          { label: '코드명', required: true, span: 2, value: formItem('codeNm', <Input placeholder="코드명" count={byteCount(CODE_NM_MAX_BYTES)} aria-label="코드명" />, D_RULES.codeNm) },
          { label: '데이터1', span: 2, value: text('data1', '데이터1') },
          { label: '데이터2', span: 2, value: text('data2', '데이터2') },
          { label: '데이터3', span: 2, value: text('data3', '데이터3') },
          { label: '데이터4', value: formItem('data4', <InputNumber min={DATA4_MIN} max={DATA4_MAX} precision={0} placeholder="숫자" style={{ width: 140 }} aria-label="데이터4" />, D_RULES.data4) },
          { label: '데이터5', value: formItem('data5', <Input placeholder="한 글자" maxLength={1} style={{ width: 80 }} aria-label="데이터5" />, D_RULES.data5) },
        ]}
      />
      <FormModalFooter onClose={onClose} screenKey={SCREEN} need={row ? 'upd' : 'reg'} saving={save.isPending} onSave={() => form.submit()} />
    </Form>
  );
};
