import type { ScreenProps } from '@/types/screen';
import DashboardPage from '../main/DashboardPage';

/**
 * 홈 = 대시보드(2026-11-16 사용자 결정 — 예전 견본 카드 바로가기는 삭제, 견본 화면은 좌측 메뉴로).
 * 같은 화면이 상단 '대시보드' 메뉴(main.dashboard)로도 열린다. 서버 권한은 main.dashboard 조회(QRY).
 */
const HomePage = (props: ScreenProps) => <DashboardPage {...props} />;

export default HomePage;
