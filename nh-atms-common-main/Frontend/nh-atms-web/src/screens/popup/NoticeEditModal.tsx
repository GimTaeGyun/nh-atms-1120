import { Form, Input, Radio, Space } from 'antd';
import type { Rule } from 'antd/es/form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { noticeApi, noticeKeys } from '@/api/endpoints/notice';
import { DetailGrid } from '@/components/display/DetailGrid';
import { FormLoadGate, FormModal, FormModalFooter } from '@/components/display/FormModal';
import { formItem } from '@/components/forms/formItem';
import { OfficeSelector } from '@/components/search/fields';
import { useAuthStore, selectIsAdmin } from '@/stores/useAuthStore';
import { byteCount, maxBytes, req } from '@/utils/rules';
import { fmtPerson } from '@/utils/format';
import { trimOrUndef } from '@/utils/object';
import { VMSG } from '@/config/messages';
import type { Notice, NoticeSave } from '@/domain/notice';
import { SUBJECT_MAX_BYTES, TEXT_MAX_BYTES } from '../sample/NoticeParts';

/** 공지 범위(팝업 안) — ALL = 전체 공지(사무소 비움, 관리자만), OFC = 사무소 지정 */
type ScopeValue = 'ALL' | 'OFC';

/** 폼 값(화면 안) — 보낼 때 toBody 로 계약 NoticeSave 모양 */
interface NoticeFormValues {
  scope: ScopeValue;
  subject: string;
  text: string;
  ofcCode?: string;
}

/* 이 팝업의 검사 목록 — 바이트 상한은 서버 DbBytes 와 같은 규칙(잠정 Q-07), 사무소는 '사무소 지정' 일 때만 필수 */
const RULES = {
  scope: [],
  subject: [req('제목'), maxBytes(SUBJECT_MAX_BYTES)],
  text: [req('내용'), maxBytes(TEXT_MAX_BYTES)],
  ofcCode: [(f) => ({ required: f.getFieldValue('scope') === 'OFC', message: VMSG.select('사무소') })],
} satisfies Record<keyof NoticeFormValues, Rule[]>;

/** 전체 공지면 사무소를 보내지 않는다(서버: 사무소 빈 공지 = 전체 공지) */
const toBody = (v: NoticeFormValues): NoticeSave => ({ subject: v.subject.trim(), text: v.text, ofcCode: v.scope === 'OFC' ? trimOrUndef(v.ofcCode) : undefined });

interface Props {
  open: boolean;
  /** null = 등록, 번호 = 수정 */
  no: number | null;
  onClose: () => void;
}

/**
 * 공지 등록/수정 팝업(`popup.noticeEdit`). 모달 안 폼은 열릴 때마다 새로 마운트(destroyOnHidden + 안쪽 컴포넌트, dev-guide §5-5).
 * 작성자는 서버가 고정 사용자로 채운다(화면은 보여 주기만). 성공 문구(S002·S003)는 서버 → 자동 표시.
 * 공지 범위(2026-11-13 사용자 결정): 관리자 = 전체 공지 / 사무소 지정(아무 사무소), 그 밖 = 사무소 지정 + 내 사무소 고정
 * (전체 공지 칸이 흐림, 서버도 403 NTC_003). 제목 200·내용 4000바이트 글자 수는 칸 아래 오른쪽.
 */
export const NoticeEditModal = ({ open, no, onClose }: Props) => (
  <FormModal open={open} onClose={onClose} title={no === null ? '공지사항 등록' : '공지사항 수정'} screenKey="popup.noticeEdit">
    <NoticeEditBody no={no} onClose={onClose} />
  </FormModal>
);

/** 수정이면 원문(text 포함, 조회수 증가 없음)을 읽은 뒤 폼을 그린다. 닫으면 캐시도 버린다(gcTime 0) → 다음에 열 때 새로 읽음 */
const NoticeEditBody = ({ no, onClose }: { no: number | null; onClose: () => void }) => {
  const detail = useQuery({ queryKey: noticeKeys.detail(no ?? 0), queryFn: () => noticeApi.get(no as number), enabled: no !== null, gcTime: 0 });
  return (
    <FormLoadGate editing={no !== null} isError={detail.isError} data={detail.data} what="공지" onClose={onClose}>
      {(row) => <NoticeForm target={row} onClose={onClose} />}
    </FormLoadGate>
  );
};

const NoticeForm = ({ target, onClose }: { target: Notice | null; onClose: () => void }) => {
  const [form] = Form.useForm<NoticeFormValues>();
  const qc = useQueryClient();
  const me = useAuthStore((s) => s.me);
  const admin = useAuthStore(selectIsAdmin);
  const myOfc = me?.ofcCode ?? undefined;
  const save = useMutation({
    mutationFn: (body: NoticeSave) => (target ? noticeApi.update(target.no, body) : noticeApi.create(body)),
    onSuccess: () => qc.invalidateQueries({ queryKey: noticeKeys.all }),
  });
  const writer = target ? { name: target.name, perNo: target.perNo } : { name: me?.name ?? null, perNo: me?.perNo ?? null };
  // 관리자가 아니면 늘 '사무소 지정 + 내 사무소'(본인 글이 예전 전체 공지여도 내 사무소로 바뀜)
  const initial: Partial<NoticeFormValues> = !admin
    ? { scope: 'OFC', subject: target?.subject ?? '', text: target?.text ?? '', ofcCode: myOfc }
    : target
      ? { scope: target.ofcCode ? 'OFC' : 'ALL', subject: target.subject ?? '', text: target.text ?? '', ofcCode: target.ofcCode ?? myOfc }
      : { scope: 'ALL', ofcCode: myOfc };
  const scope = Form.useWatch('scope', form) ?? initial.scope;

  // 검증을 통과했을 때만 불린다. 닫기는 이 요청이 성공했을 때만
  const onFinish = (v: NoticeFormValues) => save.mutate(toBody(v), { onSuccess: onClose });

  return (
    <Form<NoticeFormValues> form={form} component={false} initialValues={initial} onFinish={onFinish} scrollToFirstError>
      <DetailGrid
        cols={2}
        labelWidth={90}
        items={[
          { label: '제목', required: true, span: 2, value: formItem('subject', <Input placeholder="제목" count={byteCount(SUBJECT_MAX_BYTES)} aria-label="제목" />, RULES.subject) },
          {
            label: '공지 범위',
            required: true,
            span: 2,
            value: (
              <Space wrap size={12}>
                {formItem(
                  'scope',
                  <Radio.Group aria-label="공지 범위">
                    <Radio value="ALL" disabled={!admin}>
                      전체 공지
                    </Radio>
                    <Radio value="OFC">사무소 지정</Radio>
                  </Radio.Group>,
                  RULES.scope,
                )}
                <div style={{ width: 340 }}>{formItem('ofcCode', <OfficeSelector disabled={!admin || scope !== 'OFC'} />, RULES.ofcCode, { dependencies: ['scope'] })}</div>
                <span className="muted">{admin ? '전체 공지 = 모든 사무소에 보임 · 사무소 지정 = 그 사무소 + 관리자에게만' : '전체 공지·다른 사무소는 관리자만 — 내 사무소 공지로 등록됩니다'}</span>
              </Space>
            ),
          },
          { label: '작성자', span: 2, value: <span aria-label="작성자">{fmtPerson(writer.name, writer.perNo)}</span> },
          { label: '내용', required: true, span: 2, value: formItem('text', <Input.TextArea rows={10} placeholder="내용" count={byteCount(TEXT_MAX_BYTES)} aria-label="내용" />, RULES.text) },
        ]}
      />
      <FormModalFooter onClose={onClose} screenKey="sample.notice" need={target ? 'upd' : 'reg'} saving={save.isPending} onSave={() => form.submit()} />
    </Form>
  );
};
