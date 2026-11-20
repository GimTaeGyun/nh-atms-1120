/**
 * 코드관리(`sample.codeMgt`) 조각 — 조회조건(공통 필드만)과 파라미터 변환.
 */
import type { SearchField } from '@/components/search/SearchForm';
import { keywordField, keywordOf } from '@/components/search/optionFields';
import { ynField } from '@/components/search/lookupFields';
import { allToUndef } from '@/components/search/params';
import { compactParams } from '@/utils/object';
import type { CodeMgtSearch } from '@/domain/codeMgt';
import type { Yn } from '@/types/menu';

export const ALL: CodeMgtSearch = {};

/** 조회조건(공통 필드): 검색어(코드구분·코드명) · 사용여부(전체로 시작) */
export const CODE_FIELDS: SearchField[] = [keywordField({ placeholder: '코드구분·코드명' }), ynField('usedYn', '사용여부', { defaultValue: '' })];

/** 폼 값 → 조회 파라미터(빈 값은 뺀다) */
export const toCodeParams = (v: Record<string, unknown>): CodeMgtSearch =>
  compactParams<CodeMgtSearch>({ q: keywordOf(v.q), usedYn: allToUndef(v.usedYn) as Yn | undefined });
