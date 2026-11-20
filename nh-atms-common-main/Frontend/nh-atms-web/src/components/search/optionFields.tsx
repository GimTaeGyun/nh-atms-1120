/**
 * 공통 조회조건 — 고정 선택지 콤보 · 검색어 칸 · 검색구분+검색어 짝 · "다른 칸 값에 따라 켜지는 칸" [신규] 2026-11-13.
 * 2026-11-13 사용자 결정: 검색필터 안의 요소는 모두 공통 부품으로 만든다(없으면 여기·fields·lookupFields 에 먼저 추가).
 * 그래서 화면 파일에 `{ type: 'select', options: [...] }` · `{ type: 'input' }` · 로컬 Select/Input 칸을 직접 쓰지 않는다.
 *
 *  - optionField   고정 선택지 + 맨 위 '전체'(값 ''). 예: 레벨, 승인상태, 공지 범위
 *  - keywordField  검색어 한 칸(이름 'q'). 서버가 여러 컬럼에서 부분일치로 찾는 칸
 *  - keywordTypeFields  검색구분(전체·제목·내용…) + 검색어 두 칸. 검색구분이 '전체'면 검색어 칸이 꺼지고 비워진다
 *  - enabledBy     어떤 칸을 다른 칸 값에 따라 켜고 끈다(꺼지면 값도 비움). 예: 공지 범위 = '사무소 공지' 일 때만 사무소 칸
 *  - menuField     화면(메뉴) 콤보 — 내 메뉴(GET /menu/my) 의 화면들, 값 = 메뉴ID, 검색 가능 [신규] 2026-11-17 사용기록조회
 * 값 → API 파라미터: `allToUndef(v.x)`(params.ts) · `keywordOf(v)` · `keywordTypeOf(v)`.
 */
import { Children, cloneElement, isValidElement, useEffect, useMemo, type ReactElement, type ReactNode } from 'react';
import { Form, Input, Select } from 'antd';
import type { FormInstance, SelectProps } from 'antd';
import type { Option, ValueProps } from '@/types/common';
import type { SearchField } from './SearchForm';
import { trimOrUndef } from '@/utils/object';
import { flattenScreens, useNavStore } from '@/stores/useNavStore';

/* ---------- 1. 고정 선택지 콤보 ---------- */
export interface OptionSelectProps<O extends Option = Option> extends ValueProps<string | undefined> {
  options: O[];
  /** true(기본): 맨 위에 '전체'(값 allValue) 한 줄 */
  allowAll?: boolean;
  allLabel?: string;
  /** '전체' 값 — 기본 ''(API 파라미터에서 빠짐). 검색구분처럼 'ALL' 을 쓰는 곳은 'ALL' */
  allValue?: string;
  placeholder?: string;
  loading?: boolean;
  ariaLabel?: string;
  /** 옵션 한 줄 모양(번호·하위 수 같은 덧붙임) — 없으면 label 글자 */
  optionRender?: SelectProps<string, O>['optionRender'];
  /** 검색(입력해서 거르기). true = label 부분일치, 함수 = 그 규칙. 기본 없음 */
  search?: boolean | ((input: string, option?: O) => boolean);
}

/**
 * 고정 선택지 콤보 — 값이 비면 '전체'(allowAll). 서버 목록 콤보(사무소·기기사·권한 …)는 fields·lookupFields 의 것을 쓴다.
 * 목록이 화면에서 만들어지는 콤보(메뉴관리 대메뉴처럼)도 이걸로: `<OptionSelect options={…} optionRender={…} search={…} />`.
 */
export const OptionSelect = <O extends Option = Option>({ value, onChange, disabled, options, allowAll = true, allLabel = '전체', allValue = '', placeholder, loading, ariaLabel, optionRender, search }: OptionSelectProps<O>) => {
  const all = { value: allValue, label: allLabel } as O;
  const opts = allowAll ? [all, ...options] : options;
  const shown = value === undefined || value === null ? (allowAll ? allValue : undefined) : value;
  return (
    <Select<string, O>
      value={shown}
      onChange={(v) => onChange?.(v ?? (allowAll ? allValue : undefined))}
      options={opts}
      optionRender={optionRender}
      showSearch={search === true ? { optionFilterProp: 'label' } : typeof search === 'function' ? { filterOption: search } : undefined}
      loading={loading}
      disabled={disabled}
      placeholder={placeholder ?? (allowAll ? allLabel : '선택')}
      aria-label={ariaLabel}
      style={{ width: '100%' }}
    />
  );
};

/** 고정 선택지 칸 — 기본값 '전체'(''). 꼭 고르게 하려면 `{ allowAll: false, defaultValue: '첫 값' }` */
export const optionField = <O extends Option = Option>(
  name: string,
  label: string,
  options: O[],
  { allowAll = true, allLabel, allValue = '', optionRender, search, ...opts }: Partial<SearchField> & Pick<OptionSelectProps<O>, 'allowAll' | 'allLabel' | 'allValue' | 'optionRender' | 'search'> = {},
): SearchField => ({
  name,
  label,
  type: 'custom',
  widthClass: 'code',
  defaultValue: allowAll ? allValue : undefined,
  render: () => <OptionSelect<O> options={options} allowAll={allowAll} allLabel={allLabel} allValue={allValue} optionRender={optionRender} search={search} ariaLabel={label} />,
  ...opts,
});

/* ---------- 2. 검색어 ---------- */
/** 검색어 입력(지우기 버튼). 앞뒤 공백은 파라미터로 바꿀 때 keywordOf 가 뗀다 */
export const KeywordInput = ({ value, onChange, disabled, placeholder, ariaLabel = '검색어' }: ValueProps<string | undefined> & { placeholder?: string; ariaLabel?: string }) => (
  <Input value={value} onChange={(e) => onChange?.(e.target.value)} disabled={disabled} allowClear placeholder={placeholder ?? '검색어'} aria-label={ariaLabel} />
);

/** 검색어 칸(기본 이름 'q') — placeholder 에 찾는 대상을 적는다. 예: `keywordField({ label: '부서', placeholder: '부서코드·부서명' })` */
export const keywordField = ({ name = 'q', label = '검색어', placeholder, ...opts }: Partial<SearchField> = {}): SearchField => ({
  name,
  label,
  type: 'custom',
  widthClass: 'normal',
  render: () => <KeywordInput placeholder={placeholder} ariaLabel={label} />,
  ...opts,
});

/** 검색어 값 → 파라미터(앞뒤 공백 뗌, 비면 undefined) */
export const keywordOf = (v: unknown): string | undefined => (typeof v === 'string' ? trimOrUndef(v) : undefined);

/* ---------- 3. 다른 칸 값에 따라 켜지는 칸 ---------- */
/** 폼 칸 watch 의 값이 when 을 만족할 때만 child 를 켠다. 꺼지면 값도 비운다(조회조건에 안 보이는 값이 남지 않게) */
const Gate = ({ form, watch, when, value, onChange, children }: ValueProps<unknown> & { form: FormInstance; watch: string; when: (v: unknown) => boolean; children: ReactNode }) => {
  const on = when(Form.useWatch(watch, form));
  useEffect(() => {
    if (!on && value !== undefined && value !== '') onChange?.(undefined);
  }, [on, value, onChange]);
  const child = Children.only(children);
  if (!isValidElement(child)) return null;
  const el = child as ReactElement<ValueProps<unknown>>;
  return cloneElement(el, { value, onChange, disabled: !on || el.props.disabled });
};

/**
 * 칸 하나를 다른 칸 값에 따라 켜고 끈다(type 'custom' 칸만 — 공통 팩토리는 모두 custom).
 * 예: `enabledBy(officeField({ required: false }), 'scope', (v) => v === 'OFC')`
 */
export const enabledBy = (field: SearchField, watch: string, when: (v: unknown) => boolean): SearchField => ({
  ...field,
  render: (form) => (
    <Gate form={form} watch={watch} when={when}>
      {field.render?.(form)}
    </Gate>
  ),
});

/* ---------- 4. 검색구분 + 검색어 ---------- */
export interface KeywordTypeOptions {
  /** 검색구분 선택지('전체' 빼고) — 예: 제목·내용·작성자 */
  options: Option[];
  /** 검색구분 칸 이름(기본 'field') · 검색어 칸 이름(기본 'keyword') */
  typeName?: string;
  keywordName?: string;
  typeLabel?: string;
  keywordLabel?: string;
}

/** '전체' 검색구분 값 — 검색어를 쓰지 않는다 */
export const KEYWORD_TYPE_ALL = 'ALL';

/**
 * 검색구분 + 검색어 두 칸. 검색구분 '전체'(값 'ALL')면 검색어 칸이 꺼지고 비워진다.
 * 펼쳐서 쓴다: `fields = [rangeField(…), ...keywordTypeFields({ options: [{ value: 'SUBJECT', label: '제목' }, …] })]`
 */
export const keywordTypeFields = ({ options, typeName = 'field', keywordName = 'keyword', typeLabel = '검색구분', keywordLabel = '검색어' }: KeywordTypeOptions): SearchField[] => [
  optionField(typeName, typeLabel, options, { allValue: KEYWORD_TYPE_ALL }),
  enabledBy(
    { name: keywordName, label: keywordLabel, type: 'custom', widthClass: 'normal', render: (form) => <TypeKeywordInput form={form} typeName={typeName} label={keywordLabel} /> },
    typeName,
    (v) => !!v && v !== KEYWORD_TYPE_ALL,
  ),
];

/** 검색구분이 '전체'면 안내 문구를 바꿔 보여 준다 */
const TypeKeywordInput = ({ form, typeName, label, ...p }: ValueProps<string | undefined> & { form: FormInstance; typeName: string; label: string }) => {
  const type = Form.useWatch(typeName, form) as string | undefined;
  const off = !type || type === KEYWORD_TYPE_ALL;
  return <KeywordInput {...p} placeholder={off ? '검색구분을 고르세요' : '검색어'} ariaLabel={label} />;
};

/** 검색구분·검색어 값 → 파라미터. '전체'이거나 검색어가 비면 둘 다 undefined(조건 없음) */
export const keywordTypeOf = <T extends string = string>(v: Record<string, unknown>, typeName = 'field', keywordName = 'keyword'): { field?: T; keyword?: string } => {
  const type = v[typeName] as T | undefined;
  const keyword = !type || type === KEYWORD_TYPE_ALL ? undefined : keywordOf(v[keywordName]);
  return keyword ? { field: type, keyword } : {};
};

/** 검색구분 표시 글자(인쇄 조건 등) — '제목 : 점검', 조건 없으면 '전체' */
export const keywordTypeText = (options: Option[], field?: string, keyword?: string): string =>
  keyword ? `${options.find((o) => o.value === field)?.label ?? field} : ${keyword}` : '전체';

/* ---------- 5. 화면(메뉴) 콤보 ---------- */
/**
 * 화면 콤보 — 내 메뉴(상단·사이드 메뉴와 같은 `GET /api/web/menu/my`)의 화면들. 값 = 메뉴ID, 글자 '대메뉴 › 화면명', 입력해서 거르기.
 * 메뉴가 아직 안 읽혔으면 빈 목록(메뉴는 화면 틀이 앱 시작 때 읽는다).
 */
export const MenuSelect = ({ value, onChange, disabled }: ValueProps<string | undefined>) => {
  const menus = useNavStore((s) => s.menus);
  const options = useMemo(() => flattenScreens(menus).map((n) => ({ value: n.menuId, label: n.l1 ? `${n.l1} › ${n.menuNm}` : n.menuNm })), [menus]);
  return <OptionSelect value={value} onChange={onChange} disabled={disabled} options={options} search ariaLabel="화면" />;
};

/** 화면(메뉴) 칸(이름 'menuId', 비우면 전체) */
export const menuField = (opts: Partial<SearchField> = {}): SearchField => ({
  name: 'menuId',
  label: '화면',
  type: 'custom',
  widthClass: 'long',
  defaultValue: '',
  render: () => <MenuSelect />,
  ...opts,
});
