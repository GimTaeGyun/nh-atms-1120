import { Alert, Checkbox, Form, Input, InputNumber, Radio, Select } from 'antd';
import type { Rule } from 'antd/es/form';
import { useMutation } from '@tanstack/react-query';
import { menuMgtApi } from '@/api/endpoints/menuMgt';
import { DetailGrid, type DetailItem } from '@/components/display/DetailGrid';
import { FormModal, FormModalFooter } from '@/components/display/FormModal';
import { formItem } from '@/components/forms/formItem';
import { byteCount, check, maxBytes, maxLen, numRange, req, reqSelect } from '@/utils/rules';
import { VMSG } from '@/config/messages';
import type { ValueProps } from '@/types/common';
import type { MenuLevel, MenuMgtRow, MenuMgtSave } from '@/domain/menuMgt';
import { LEVEL_NAME, LEVEL_OPTIONS, LevelTag, MENU_NM_MAX_BYTES, MENU_PATH_MAX_LEN, MenuIdText, SORT_NO_MAX } from '../sample/MenuMgtColumns';
import {
  comboFoot,
  matchMenuOption,
  MenuOptionLine,
  midOptions,
  nextSort,
  pathHeadOf,
  previewMenuId,
  topOptions,
  useAfterMenuChange,
  useAllMenus,
  type MenuOption,
} from '../sample/MenuMgtParts';

/** 폼 값(화면 안) — 보낼 때 toBody 로 계약 MenuMgtSave 모양 */
interface MenuFormValues {
  menuLevel: MenuLevel;
  topMenuId?: string;
  midMenuId?: string;
  prefix?: string;
  menuNm: string;
  menuPath?: string;
  sortNo: number;
  use: boolean;
}

/** 메뉴경로 모양 — 서버 MenuMgtRules.PATH 와 같다 */
const PATH_RE = /^\/[a-z][a-z0-9]*\/[a-z0-9]+(-[a-z0-9]+)*$/;
const pathShape = check<string | undefined>((v) => (!v || PATH_RE.test(v.trim()) ? undefined : VMSG.menuPath));

/** 대메뉴 접두어 칸 — 영문만 남기고 대문자로 */
const PrefixInput = ({ value, onChange }: ValueProps<string | undefined>) => (
  <Input value={value} onChange={(e) => onChange?.(e.target.value.toUpperCase().replace(/[^A-Z]/g, ''))} maxLength={3} placeholder="영문 대문자 3자 (예: SYS)" style={{ width: 200 }} aria-label="접두어" />
);

/* 이 팝업의 검사 목록 — 레벨에 따라 안 그리는 칸(대메뉴·중메뉴·접두어·경로)은 폼에 없어서 검사도 안 한다 */
const RULES = {
  menuLevel: [reqSelect('레벨')],
  topMenuId: [reqSelect('대메뉴')],
  midMenuId: [reqSelect('중메뉴')],
  prefix: [req('접두어'), { pattern: /^[A-Z]{3}$/, message: VMSG.upperLetters('접두어', 3) }],
  menuNm: [req('메뉴명'), maxBytes(MENU_NM_MAX_BYTES)],
  menuPath: [req('메뉴경로'), maxLen(MENU_PATH_MAX_LEN), pathShape],
  sortNo: [req('순서'), numRange(0, SORT_NO_MAX)],
  use: [],
} satisfies Record<keyof MenuFormValues, Rule[]>;

const parentOf = (v: Pick<MenuFormValues, 'menuLevel' | 'topMenuId' | 'midMenuId'>): string | undefined =>
  v.menuLevel === 2 ? v.topMenuId : v.menuLevel === 3 ? v.midMenuId : undefined;

const toBody = (v: MenuFormValues, screen: boolean): MenuMgtSave => ({
  menuLevel: v.menuLevel,
  parentMenuId: parentOf(v),
  prefix: v.menuLevel === 1 ? v.prefix?.trim().toUpperCase() : undefined,
  menuNm: v.menuNm.trim(),
  menuPath: screen ? v.menuPath?.trim() : undefined,
  sortNo: v.sortNo,
  useYn: v.use ? 'Y' : 'N',
});

interface Props {
  open: boolean;
  /** null = 등록, 행 = 수정 */
  target: MenuMgtRow | null;
  /** 등록 때 처음 고를 대메뉴(조회조건의 대메뉴) */
  defaultTopMenuId?: string;
  onClose: () => void;
}

/**
 * 메뉴 등록/수정 팝업(`popup.menuEdit`). 상위는 ① 대 → 중 연쇄 콤보로 고른다(2026-11-12 사용자 결정).
 *  등록: 레벨(대·중·소) → 대메뉴 콤보 → 중메뉴 콤보(소메뉴일 때) → 메뉴ID 미리보기(MAX+1, 저장 때 서버가 확정) → 메뉴명·경로·순서·사용
 *  수정: 위치·메뉴ID 는 보이기만(메뉴ID 에 상위가 들어 있어 못 바꿈) → 메뉴명·경로(화면만)·순서·사용
 * 모달 안 폼은 열 때마다 새로 그린다(destroyOnHidden + 안쪽 컴포넌트). 성공 문구(S002·S003)는 서버 → 자동 표시.
 */
export const MenuEditModal = ({ open, target, defaultTopMenuId, onClose }: Props) => (
  <FormModal open={open} onClose={onClose} width={640} title={target ? '메뉴 수정' : '메뉴 등록'} screenKey="popup.menuEdit">
    <MenuForm target={target} defaultTopMenuId={defaultTopMenuId} onClose={onClose} />
  </FormModal>
);

const MenuForm = ({ target, defaultTopMenuId, onClose }: { target: MenuMgtRow | null; defaultTopMenuId?: string; onClose: () => void }) => {
  const [form] = Form.useForm<MenuFormValues>();
  const { rows, loading } = useAllMenus();
  const after = useAfterMenuChange();
  const isNew = target === null;
  const save = useMutation({
    mutationFn: (body: MenuMgtSave) => (target ? menuMgtApi.update(target.menuId, body) : menuMgtApi.create(body)),
    onSuccess: after,
  });

  // 지금 폼 값 — 레벨·상위가 바뀌면 콤보·메뉴ID 미리보기가 따라 바뀐다
  const level = (Form.useWatch('menuLevel', form) ?? target?.menuLevel ?? 3) as MenuLevel;
  const topMenuId = Form.useWatch('topMenuId', form);
  const midMenuId = Form.useWatch('midMenuId', form);
  const prefix = Form.useWatch('prefix', form);
  const screen = isNew ? level === 3 : target.menuType === 'M';

  const tops = topOptions(rows);
  const mids = midOptions(rows, topMenuId);
  const preview = isNew ? previewMenuId(rows, level, parentOf({ menuLevel: level, topMenuId, midMenuId }), prefix) : { id: target.menuId };

  const initial: Partial<MenuFormValues> = target
    ? { menuLevel: target.menuLevel ?? undefined, menuNm: target.menuNm, menuPath: target.menuPath ?? undefined, sortNo: target.sortNo, use: target.useYn === 'Y' }
    : { menuLevel: 3, topMenuId: defaultTopMenuId, use: true, sortNo: 1 };

  /** 상위가 바뀌면 순서 = 그 상위의 마지막 + 1, 경로 앞부분 = 같은 대메뉴 화면 경로 */
  const onParentChange = (next: Partial<MenuFormValues>) => {
    const v = { ...form.getFieldsValue(), ...next };
    const patch: Partial<MenuFormValues> = { ...next, sortNo: nextSort(rows, v.menuLevel, parentOf(v)) };
    const head = pathHeadOf(rows, v.topMenuId);
    if (v.menuLevel === 3 && head && (!v.menuPath || /^\/[a-z0-9]*\/?$/.test(v.menuPath))) patch.menuPath = head;
    form.setFieldsValue(patch);
  };

  const onFinish = (v: MenuFormValues) => save.mutate(toBody(v, screen), { onSuccess: onClose });

  const topSelect = (
    <Select<string, MenuOption>
      options={tops}
      optionRender={(o) => <MenuOptionLine o={o.data} kind="top" />}
      showSearch={{ filterOption: (input, o) => matchMenuOption(input, o) }}
      popupRender={(menu) => comboFoot(menu, tops.length, '이름·접두어로 검색, 미사용은 맨 아래')}
      onChange={(v: string) => onParentChange({ topMenuId: v, midMenuId: undefined })}
      loading={loading}
      placeholder="대메뉴 선택"
      aria-label="대메뉴"
    />
  );
  const midSelect = (
    <Select<string, MenuOption>
      options={mids}
      optionRender={(o) => <MenuOptionLine o={o.data} kind="mid" />}
      showSearch={{ filterOption: (input, o) => matchMenuOption(input, o) }}
      popupRender={(menu) => comboFoot(menu, mids.length, '번호(3·03)나 이름으로 검색, 미사용은 맨 아래')}
      onChange={(v: string) => onParentChange({ midMenuId: v })}
      disabled={!topMenuId}
      placeholder={topMenuId ? `중메뉴 선택 (${mids.length}개)` : '대메뉴를 먼저 고르세요'}
      aria-label="중메뉴"
    />
  );

  const where: DetailItem[] = isNew
    ? [
        {
          label: '레벨',
          required: true,
          value: formItem(
            'menuLevel',
            <Radio.Group
              options={LEVEL_OPTIONS}
              onChange={(e) => onParentChange({ menuLevel: e.target.value as MenuLevel, ...(e.target.value === 1 ? { topMenuId: undefined } : {}), midMenuId: undefined })}
              aria-label="레벨"
            />,
            RULES.menuLevel,
          ),
        },
        ...(level >= 2 ? [{ label: '대메뉴', required: true, value: formItem('topMenuId', topSelect, RULES.topMenuId) }] : []),
        ...(level === 3 ? [{ label: '중메뉴', required: true, value: formItem('midMenuId', midSelect, RULES.midMenuId) }] : []),
        ...(level === 1
          ? [{ label: '접두어', required: true, value: formItem('prefix', <PrefixInput />, RULES.prefix) }]
          : []),
      ]
    : [{ label: '위치', value: <span>{target.parentPath ? `${target.parentPath} › ` : ''}<b>{target.menuNm}</b> <LevelTag level={target.menuLevel} type={target.menuType} /></span> }];

  const idItem: DetailItem = {
    label: '메뉴ID',
    value: (
      <div>
        {preview.id ? <MenuIdText id={preview.id} big /> : <span className="muted">{preview.reason}</span>}
        <div className="muted" style={{ fontSize: 12, marginTop: 2 }}>
          {isNew ? '미리보기 — 저장할 때 서버가 같은 규칙(같은 상위 안 번호 MAX + 1)으로 확정합니다.' : '메뉴ID 에 상위가 들어 있어 위치는 바꿀 수 없습니다. 옮기려면 새로 등록하고 이 메뉴는 미사용으로.'}
        </div>
      </div>
    ),
  };

  const items: DetailItem[] = [
    ...where,
    idItem,
    { label: '메뉴명', required: true, value: formItem('menuNm', <Input placeholder={`${LEVEL_NAME[level] ?? '메뉴'} 이름`} count={byteCount(MENU_NM_MAX_BYTES)} aria-label="메뉴명" />, RULES.menuNm) },
    screen
      ? { label: '메뉴경로', required: true, value: formItem('menuPath', <Input placeholder="/sample/menu-mgt" aria-label="메뉴경로" />, RULES.menuPath) }
      : { label: '메뉴경로', value: <span className="muted">— 그룹(대·중메뉴)은 경로 없음</span> },
    { label: '순서', required: true, value: formItem('sortNo', <InputNumber min={0} max={SORT_NO_MAX} precision={0} style={{ width: 120 }} aria-label="순서" />, RULES.sortNo) },
    { label: '사용', value: formItem('use', <Checkbox aria-label="사용">사용 — 끄면 메뉴에서 숨김(삭제 대신)</Checkbox>, RULES.use, { valuePropName: 'checked' }) },
  ];

  return (
    <Form<MenuFormValues> form={form} component={false} initialValues={initial} onFinish={onFinish} scrollToFirstError>
      {screen && isNew && (
        <Alert type="info" showIcon style={{ marginBottom: 12 }} message="메뉴경로는 화면 registry 경로와 한 글자도 같아야 메뉴를 눌렀을 때 그 화면이 열립니다." />
      )}
      <DetailGrid cols={1} labelWidth={90} items={items} />
      <FormModalFooter
        onClose={onClose}
        screenKey="sample.menuMgt"
        need={isNew ? 'reg' : 'upd'}
        saving={save.isPending}
        disabled={isNew && !preview.id && level !== 1}
        onSave={() => form.submit()}
      />
    </Form>
  );
};
