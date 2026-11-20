package kr.co.nh.atms.web.common.uselog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 사용기록 이력번호(HIST_ID NUMBER(19)) 생성 — 시각 yyyyMMddHHmmss + 서버번호 1자리 + 초당 일련 4자리(2026-11-17 사용자 결정).
 * <p>
 * 서버번호 = 설정 {@code atms.uselog.node}(기본 1, 2호기 ws02 는 {@code -Datms.uselog.node=2}) → 서버 2대가 같은 초에 써도 겹치지 않는다.
 * 같은 초에 1만 건을 넘으면 다음 초가 될 때까지 기다린다. 시퀀스 없이 만든다(정본에 시퀀스 이름 없음, 잠정 Q-31).
 */
@Component
public class HistIdGenerator {

    private static final DateTimeFormatter DT14 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final int node;
    private String second = "";
    private int seq;

    public HistIdGenerator(@Value("${atms.uselog.node:1}") int node) {
        if (node < 0 || node > 9) throw new IllegalArgumentException("atms.uselog.node 는 0~9");
        this.node = node;
    }

    public synchronized long next() {
        while (true) {
            String now = LocalDateTime.now().format(DT14);
            if (!now.equals(second)) {
                second = now;
                seq = 0;
            }
            if (seq <= 9999) return CrudLogRules.histId(now, node, seq++);
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("HIST_ID", e);
            }
        }
    }
}
