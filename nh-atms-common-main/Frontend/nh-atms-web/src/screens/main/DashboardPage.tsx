import { useMemo, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Tag } from 'antd';
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { dashboardApi, dashboardKeys } from '@/api/endpoints/dashboard';
import { DataGrid } from '@/components/grid/DataGrid';
import { ResultHeader } from '@/components/page/ResultHeader';
import { OptionSelect } from '@/components/search/optionFields';
import { useOpenScreen } from '@/screens/navigation';
import { fmtDate, fmtDateTime14 } from '@/utils/format';
import type { DashSearch, DashStatus } from '@/domain/dashboard';
import type { ScreenProps } from '@/types/screen';
import { NoticeViewModal } from '../popup/NoticeViewModal';
import { machineColumns, NoticeLine, QuickLinks, STATUS_LABEL, StatusBox } from './DashboardParts';
import './dashboard.css';

/** 표에 보이는 행 수(사진 4행) × 행 높이(목록 표 약 29.6px) — 나머지는 표 안 스크롤 */
const TABLE_ROWS = 4;
const ROW_HEIGHT = 29.6;

/**
 * 대시보드(`main.dashboard`, 홈 탭) — 설계서 26-0000 초기화면, API 계약 §10, 2026-11-16 사용자 결정.
 *  레벨(권한코드 A 중앙본부·T 영업본부·L 영업점)별로 '지역 구분'·'사무소 구분' 콤보가 켜지거나 잠긴다.
 *   중앙본부 = 둘 다 켬 · 영업본부 = 지역 잠금(내 본부), 사무소 켬 · 영업점 = 둘 다 잠금(내 사무소)
 *  [조회]를 누를 때만 다시 읽는다(새로고침 버튼 없음 — 중앙본부 전국 기기 매번 재조회 방지). [↻] = 콤보를 처음 값으로.
 *  ⑦ 총 운영 대수 · ③ 일평균 거래량(전 영업일) · ⑤ 기기 상태 2×2(누르면 표 거름) · ① 공지 6건(제목 = 상세 팝업, 전체보기 = 공지 탭) · ④⑥ 기기상태정보(4행)
 */
const DashboardPage = (_props: ScreenProps) => {
  const openScreen = useOpenScreen();
  const scope = useQuery({ queryKey: dashboardKeys.scope(), queryFn: dashboardApi.scope, staleTime: Infinity });
  const sc = scope.data;

  // 콤보 값: 손대기 전(undefined)이면 처음 값(scope 의 기본값)
  const [regionPick, setRegion] = useState<string>();
  const [ofcPick, setOfcCode] = useState<string>();
  const [appliedPick, setApplied] = useState<DashSearch>();
  const [stFilter, setStFilter] = useState<DashStatus | null>(null);

  const initial = useMemo<DashSearch | null>(() => (sc ? { region: sc.defaultRegion ?? undefined, ofcCode: sc.defaultOfcCode ?? undefined } : null), [sc]);
  const region = regionPick ?? initial?.region ?? '';
  const ofcCode = ofcPick ?? initial?.ofcCode ?? '';
  // 범위를 받으면 처음 값으로 한 번 조회, 그 뒤는 [조회]를 누른 값
  const applied = appliedPick ?? initial;

  const offices = useQuery({ queryKey: dashboardKeys.offices(region), queryFn: () => dashboardApi.offices(region), enabled: !!region && !!sc?.officeEditable, staleTime: 5 * 60_000 });
  const summary = useQuery({ queryKey: dashboardKeys.summary(applied ?? {}), queryFn: () => dashboardApi.summary(applied ?? {}), enabled: !!applied, staleTime: Infinity });
  const notices = useQuery({ queryKey: dashboardKeys.notices(), queryFn: dashboardApi.notices });
  const view = useMutation({ mutationFn: dashboardApi.notice });
  const [viewOpen, setViewOpen] = useState(false);

  const s = summary.data;
  const changed = !!applied && ((applied.region ?? '') !== region || (applied.ofcCode ?? '') !== ofcCode);
  const onSearch = () => {
    const next: DashSearch = { region: region || undefined, ofcCode: ofcCode || undefined };
    setStFilter(null);
    if (JSON.stringify(next) === JSON.stringify(applied)) void summary.refetch();
    else setApplied(next);
  };
  const onReset = () => {
    setRegion(undefined);
    setOfcCode(undefined);
  };
  const openNotice = (no: number) => {
    setViewOpen(true);
    view.mutate(no, { onError: () => setViewOpen(false) });
  };

  const regionOptions = (sc?.regions ?? []).map((r) => ({ value: r.ofcCode, label: `[${r.ofcCode}] ${r.kanm ?? ''}` }));
  const officeOptions = sc?.officeEditable
    ? (offices.data ?? []).map((o) => ({ value: o.ofcCode, label: `[${o.ofcCode}] ${o.kanm ?? ''}` }))
    : sc?.myOffice
      ? [{ value: sc.myOffice.ofcCode, label: `[${sc.myOffice.ofcCode}] ${sc.myOffice.kanm ?? ''}` }]
      : [];
  const columns = useMemo(() => machineColumns(), []);
  const rows = useMemo(() => (s?.machines ?? []).filter((m) => !stFilter || m.status === stFilter), [s, stFilter]);

  return (
    <div className="dash">
      <div className="dash-top">
        <div className="dash-left">
          <div className="dash-hero">
            <div className="dash-ofc">
              <div className="sys">NH 자동화기기 종합관리시스템</div>
              <h1>
                {s?.title ?? sc?.myOffice?.kanm ?? '-'}
                <small>{s ? (s.titleKind === 'BRANCH' ? '지점' : s.titleKind === 'REGION' ? '영업본부' : '중앙본부') : ''}</small>
              </h1>
              <div className="code">{s?.titleCode ? `사무소 코드 : ${s.titleCode}` : s ? '전국 사무소' : ''}</div>
            </div>
            <div className="dash-chara" aria-label="캐릭터 이미지 자리" />
            <QuickLinks onOpen={(k) => openScreen(k)} />
          </div>

          <form
            className="dash-cond"
            onSubmit={(e) => {
              e.preventDefault();
              onSearch();
            }}
          >
            <div className="fields">
              <div className="f">
                <label htmlFor="dash-region">지역 구분</label>
                <OptionSelect
                  value={region}
                  onChange={(v) => {
                    setRegion(v ?? '');
                    setOfcCode('');
                  }}
                  options={regionOptions}
                  allowAll={!!sc?.regionEditable}
                  allLabel="전국"
                  disabled={!sc?.regionEditable}
                  search
                  ariaLabel="지역 구분"
                />
              </div>
              <div className="f">
                <label htmlFor="dash-ofc">사무소 구분</label>
                <OptionSelect
                  value={ofcCode}
                  onChange={(v) => setOfcCode(v ?? '')}
                  options={officeOptions}
                  allowAll={!!sc?.officeEditable}
                  disabled={!sc?.officeEditable || (sc.level === 'CENTRAL' && !region)}
                  loading={offices.isFetching}
                  search
                  ariaLabel="사무소 구분"
                />
              </div>
            </div>
            <div className="acts">
              <button type="submit" className="dash-btn" disabled={!sc}>
                <SearchOutlined /> 조회
              </button>
              <button type="button" className="dash-reset" aria-label="처음 값으로" title="처음 값으로" onClick={onReset}>
                <ReloadOutlined />
              </button>
            </div>
            {changed && <span className="dash-stale">조건이 바뀌었습니다 — [조회]를 누르면 반영됩니다</span>}
          </form>

          <div className="dash-cards">
            <div className="dash-card">
              <div className="k">총 자동화기기 운영 대수</div>
              <div className="v num">
                {(s?.total ?? 0).toLocaleString('ko-KR')}
                <small>대</small>
              </div>
              <div className="at">{s ? `${fmtDateTime14(s.baseDtm).slice(0, 16)} 기준` : ''}</div>
            </div>
            <div className="dash-card">
              <div className="k">자동화기기 일평균거래량</div>
              <div className="v num">
                {(s?.dealAvg ?? 0).toLocaleString('ko-KR')}
                <small>건</small>
              </div>
              <div className="at">{s ? `${fmtDate(s.dealDate)} (전 영업일) 기준` : ''}</div>
            </div>
            <div className="dash-st-wrap">
              <div className="dash-st-head">
                <button type="button" className="dash-link" onClick={() => setStFilter(null)}>
                  상세보기 ›
                </button>
              </div>
              <StatusBox count={s?.statusCount} selected={stFilter} onPick={setStFilter} />
            </div>
          </div>
        </div>

        <aside className="dash-notice" aria-labelledby="dash-n-title">
          <div className="row">
            <h2 id="dash-n-title">자동화기기 공지사항</h2>
            <button type="button" className="dash-link" onClick={() => openScreen('sample.notice')}>
              전체보기 ›
            </button>
          </div>
          <ul>
            {(notices.data ?? []).map((n) => (
              <NoticeLine key={n.no} n={n} onOpen={(x) => openNotice(x.no)} />
            ))}
            {notices.data && !notices.data.length && <li className="muted">공지가 없습니다.</li>}
          </ul>
        </aside>
      </div>

      <div className="dash-table">
        <ResultHeader
          title="기기상태정보"
          total={s ? rows.length : undefined}
          leftActions={stFilter && (
            <Tag
              closable
              color="blue"
              role="button"
              tabIndex={0}
              aria-label={`${STATUS_LABEL[stFilter]}만 보기 풀기`}
              title="누르면 전체 보기"
              onClick={() => setStFilter(null)}
              onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setStFilter(null); } }}
              onClose={(e) => { e.preventDefault(); setStFilter(null); }}
              style={{ margin: 0, cursor: 'pointer' }}
            >
              {`${STATUS_LABEL[stFilter]}만`}
            </Tag>
          )}
        />
        <DataGrid
          columns={columns}
          rows={rows}
          rowKey={(r) => `${r.mgrOfcCode}-${r.mcnNo}`}
          loading={summary.isFetching}
          searched={!!s}
          grade="G2"
          height={TABLE_ROWS * ROW_HEIGHT}
          clientPageSize={0}
          sortable={false}
          bordered
        />
      </div>

      <NoticeViewModal
        open={viewOpen}
        notice={view.data}
        loading={view.isPending}
        canEdit={false}
        canDelete={false}
        onClose={() => setViewOpen(false)}
        onMove={openNotice}
        onEdit={() => undefined}
        onDelete={() => undefined}
      />
    </div>
  );
};

export default DashboardPage;
