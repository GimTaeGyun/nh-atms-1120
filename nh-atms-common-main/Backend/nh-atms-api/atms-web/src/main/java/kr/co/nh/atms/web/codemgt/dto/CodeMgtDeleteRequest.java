package kr.co.nh.atms.web.codemgt.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 다건 삭제 본문 {@code { keys: string[] }} (1~100건) — 코드구분 삭제는 코드구분, 상세코드 삭제는 코드. API 계약 §11 */
@Getter
@Setter
public class CodeMgtDeleteRequest {
    private List<String> keys;
}
