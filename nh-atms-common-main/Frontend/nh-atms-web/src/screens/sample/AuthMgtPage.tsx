import { useState } from 'react';
import { Tabs } from 'antd';
import { PageFrame } from '@/components/page/PageFrame';
import type { ScreenProps } from '@/types/screen';
import { MenuAuthPane, UserAuthPane } from './AuthMgtParts';

type TabKey = 'dept' | 'person' | 'menu';

/**
 * 권한관리(`sample.authMgt`) — AS-IS "시스템 사용권한부여" 화면 구성 기준 + 메뉴별 권한(API 계약 §8).
 *  [부서권한]·[개인권한] TB_SCR_AUTH — AS-IS 권한목록(부서별)/(개인별). PER_NO '1' = 부서권한, 그 밖 = 개인권한(2026-11-11 사용자 결정).
 *                                조회조건 → 권한목록(순번·부서정보·사무소정보·(사용자)·권한·…·등록정보) → [등록]·[수정] 팝업, [삭제](여러 건), [엑셀].
 *                                공지사항 견본과 같은 "팝업형 CRUD". 로그인 권한 = 유효한 개인권한 우선, 없으면 내 부서의 부서권한.
 *  [메뉴 권한]   TB_SCR_AUTH_MENU — 왼쪽 권한코드 → 오른쪽 메뉴 전체 표(조회·등록·수정·삭제·실행·사용 체크) → [저장] 한 번에.
 *                                기기모델코드 견본과 같은 "표 직접 편집·일괄 저장".
 * 권한은 이 화면(sample.authMgt) 기준 — 조회 QRY, 등록 REG, 수정 UPD, 삭제 DEL. 서버도 같은 검사를 한다.
 * 단축키(hotkeys)는 지금 보이는 탭에만 건다.
 */
const AuthMgtPage = ({ screenKey, active }: ScreenProps) => {
  const [tab, setTab] = useState<TabKey>('dept');
  return (
    <PageFrame screenKey={screenKey} subtitle="부서·개인에게 권한을 부여하고(개인권한이 있으면 개인권한 우선), 권한별로 메뉴 권한(조회·등록·수정·삭제·실행)을 정합니다">
      <Tabs
        activeKey={tab}
        onChange={(k) => setTab(k as TabKey)}
        items={[
          { key: 'dept', label: '부서권한', children: <UserAuthPane kind="DEPT" screenKey={screenKey} active={active && tab === 'dept'} /> },
          { key: 'person', label: '개인권한', children: <UserAuthPane kind="PERSON" screenKey={screenKey} active={active && tab === 'person'} /> },
          { key: 'menu', label: '메뉴 권한', children: <MenuAuthPane screenKey={screenKey} active={active && tab === 'menu'} /> },
        ]}
      />
    </PageFrame>
  );
};

export default AuthMgtPage;
