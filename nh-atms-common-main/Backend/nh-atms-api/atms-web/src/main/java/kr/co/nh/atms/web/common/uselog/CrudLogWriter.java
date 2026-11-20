package kr.co.nh.atms.web.common.uselog;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용기록 저장 — 업무 트랜잭션과 분리(REQUIRES_NEW): 업무가 롤백돼도 기록은 남는다.
 * 실패 예외는 부르는 쪽({@link CrudLogAspect}·엑셀 기록 서비스)이 삼킨다 — 기록 실패가 업무를 막지 않는다.
 */
@Service
@RequiredArgsConstructor
public class CrudLogWriter {

    private final CrudLogMapper mapper;
    private final HistIdGenerator ids;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(CrudLogEntry e) {
        if (e.getHistId() == null) e.setHistId(ids.next());
        mapper.insert(e);
    }
}
