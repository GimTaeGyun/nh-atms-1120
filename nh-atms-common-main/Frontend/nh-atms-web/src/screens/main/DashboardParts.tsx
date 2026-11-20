/**
 * 대시보드(`main.dashboard`) 부품 — 상태 표시·상태 박스(2×2)·공지 목록·기기상태정보 컬럼.
 * 화면 본체 DashboardPage.tsx. 사진(설계서 26-0000 초기화면) 비율에 맞춘 모양은 dashboard.css.
 */
import { Tag } from 'antd';
import { centerCol, col, textCol, type Col } from '@/components/grid/columns';
import { fmtDate } from '@/utils/format';
import type { DashMachine, DashStatus } from '@/domain/dashboard';
import type { Notice } from '@/domain/notice';
import { isNewNotice, NEW_DAYS } from '../sample/NoticeParts';
import { usePermission } from '@/hooks/usePermission';
import { useAppMessage } from '@/hooks/useAppMessage';
import { DENIED_TEXT } from '@/components/auth/useAllowed';
import type { ScreenKey } from '@/screens/screenIds';

/* ---------- 바로가기 (2026-11-17 사용자 결정: 이미지 오른쪽 가로 3칸, 시안 C 위쪽 색띠) ---------- */
export interface QuickLink {
  /** 열 화면 */
  screenKey: ScreenKey;
  /** 칸 이름 */
  label: string;
  /** 설명 한 줄(길면 … , 마우스를 올리면 전체) */
  desc?: string;
}

/**
 * 바로가기 대상 — **여기 한 곳만** 채운다(3칸, null = 빈 자리 '대상 미정').
 * 2026-11-17 사용자 결정: 공지사항 · 기기정보 · 코드관리(설명은 칸 폭 72px 에 맞게 짧게).
 */
export const QUICK_LINKS: (QuickLink | null)[] = [
  { screenKey: 'sample.notice', label: '공지사항', desc: '전체·사무소' },
  { screenKey: 'sample.mcnInfo', label: '기기정보', desc: '기기 조회' },
  { screenKey: 'sample.codeMgt', label: '코드관리', desc: '공통코드' },
];

/** 바로가기 한 칸 — 빈 자리는 흐림, 조회 권한이 없으면 흐림 + 누르면 '권한이 없습니다.' */
const QuickLinkButton = ({ link, onOpen }: { link: QuickLink | null; onOpen: (k: ScreenKey) => void }) => {
  const perm = usePermission(link?.screenKey);
  const { message } = useAppMessage();
  if (!link) {
    return (
      <button type="button" className="dash-ql empty" aria-disabled="true" tabIndex={-1}>
        <span className="nm">바로가기</span>
        <span className="ds">대상 미정</span>
      </button>
    );
  }
  const denied = !perm.canQry;
  return (
    <button
      type="button"
      className={`dash-ql${denied ? ' denied' : ''}`}
      aria-disabled={denied || undefined}
      title={denied ? DENIED_TEXT : link.desc ? `${link.label} — ${link.desc}` : link.label}
      onClick={() => (denied ? void message.warning(DENIED_TEXT) : onOpen(link.screenKey))}
    >
      <span className="go" aria-hidden="true">›</span>
      <span className="nm">{link.label}</span>
      {link.desc && <span className="ds">{link.desc}</span>}
    </button>
  );
};

/** 바로가기 3칸(가로) */
export const QuickLinks = ({ onOpen }: { onOpen: (k: ScreenKey) => void }) => (
  <nav className="dash-links" aria-label="바로가기">
    {QUICK_LINKS.map((l, i) => (
      <QuickLinkButton key={l ? l.screenKey : `empty-${i}`} link={l} onOpen={onOpen} />
    ))}
  </nav>
);

/** 상태 순서(사진: 정상·장애 / 보수중·통신단절) */
export const STATUS_ORDER: DashStatus[] = ['OK', 'ERR', 'FIX', 'OFF'];
/** 상태 이름 — AS-IS 6000 분류를 4개로(서버 DashboardRules.statusOf, 잠정 Q-26) */
export const STATUS_LABEL: Record<DashStatus, string> = { OK: '정상', ERR: '장애', FIX: '보수중', OFF: '통신단절' };
const STATUS_TAG: Record<DashStatus, string> = { OK: 'success', ERR: 'error', FIX: 'warning', OFF: 'default' };

/** 상태 박스(2×2) — 칸을 누르면 아래 표를 그 상태로 거름(한 번 더 누르면 풀림) */
export const StatusBox = ({ count, selected, onPick }: { count?: Record<DashStatus, number>; selected: DashStatus | null; onPick: (s: DashStatus | null) => void }) => (
  <div className="dash-status" role="group" aria-label="기기 상태">
    {STATUS_ORDER.map((s) => (
      <button key={s} type="button" className={`dash-st st-${s.toLowerCase()}`} aria-pressed={selected === s} onClick={() => onPick(selected === s ? null : s)}>
        <span className="lb">
          <i className="dot" aria-hidden="true" />
          {STATUS_LABEL[s]}
        </span>
        <b className="num">
          {(count?.[s] ?? 0).toLocaleString('ko-KR')}
          <small>대</small>
        </b>
      </button>
    ))}
  </div>
);

/** 작성일 'YY/MM/DD'(사진 26/10/05) */
const shortDate = (ymd?: string | null): string => (ymd ? fmtDate(ymd, '/').slice(2) : '');

/** 공지 목록 한 줄 — '- 제목 (전체) (N) …… 26/10/05'. 전체 공지(사무소 빈 것)는 제목 앞 '전체' 꼬리표 */
export const NoticeLine = ({ n, onOpen }: { n: Notice; onOpen: (n: Notice) => void }) => (
  <li>
    <button type="button" className="dash-notice-link" onClick={() => onOpen(n)} title={n.subject ?? ''}>
      <span className="dash-notice-dash" aria-hidden="true">-</span>
      {!n.ofcCode && <span className="dash-notice-all">전체</span>}
      <span className="dash-notice-subj">{n.subject || '(제목 없음)'}</span>
      {isNewNotice(n.wrtDate) && (
        <span className="dash-notice-new" title={`${NEW_DAYS}일 안에 쓴 글`}>
          N
        </span>
      )}
    </button>
    <span className="dash-notice-date num">{shortDate(n.wrtDate)}</span>
  </li>
);

/** 기기상태정보 컬럼 — 설계서 순서 그대로(관리점·관리점코드·관리모점·관리모점코드 | 코너명·기번·기종·제조사·모델명·기기상태·기기상태상세·주소·관리업체) */
export const machineColumns = (): Col<DashMachine>[] => [
  textCol<DashMachine>('관리점', 'mgrOfcNm', 110, { align: 'center' }),
  centerCol<DashMachine>('관리점코드', 'mgrOfcCode', 90),
  textCol<DashMachine>('관리모점', 'parentOfcNm', 110, { align: 'center' }),
  centerCol<DashMachine>('관리모점코드', 'parentOfcCode', 100),
  textCol<DashMachine>('코너명', 'instPstNm', 130, { align: 'center' }),
  centerCol<DashMachine>('기번', 'mcnNo', 60),
  centerCol<DashMachine>('기종', 'srcNm', 70),
  centerCol<DashMachine>('제조사', 'vendNm', 90),
  centerCol<DashMachine>('모델명', 'modelNm', 90),
  col<DashMachine>('기기상태', 'status', 80, {
    align: 'center',
    render: (v: DashStatus) => <Tag color={STATUS_TAG[v]} style={{ marginInlineEnd: 0 }}>{STATUS_LABEL[v]}</Tag>,
    excel: (r) => STATUS_LABEL[r.status],
  }),
  centerCol<DashMachine>('기기상태상세', 'statusDetail', 120),
  textCol<DashMachine>('주소', 'address', 240),
  centerCol<DashMachine>('관리업체', 'mgmtVendCode', 90),
];
